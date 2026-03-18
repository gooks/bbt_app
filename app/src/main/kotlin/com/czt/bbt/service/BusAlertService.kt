package com.czt.bbt.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.czt.bbt.MainActivity
import com.czt.bbt.R
import com.czt.bbt.data.BusRepository
import com.czt.bbt.model.*
import com.czt.bbt.api.GBusArrivalInfoItem
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.sqrt

@AndroidEntryPoint
class BusAlertService : Service(), SensorEventListener, TextToSpeech.OnInitListener {

    @Inject lateinit var repository: BusRepository
    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var notificationManager: NotificationManager
    private lateinit var vibrator: Vibrator
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val gson = Gson()

    private var mode: Mode = Mode.IDLE
    private var activeRideAlert: RideAlert? = null
    private val activeArrivalJobs = mutableMapOf<Long, Job>()
    private val activeArrivalAlerts = mutableMapOf<Long, ArrivalAlert>()
    private val routeStationsCache = mutableMapOf<String, List<CachedRouteStation>>()

    // Arrival Notification State
    private var lastAlertStops: Int = -1
    private val lastArrivalAlertStops = mutableMapOf<Long, Int>()
    private val lastAnnouncedRouteId = mutableMapOf<Long, String>()

    // Ride Mode State
    private var boardingStationName: String? = null
    private var isBoardingDetected = false
    private var isAlightingDetected = false
    private var potentialBoardingTime: Long = 0L 
    private var currentBusPlate: String? = null
    private var boardingTime: Long = 0
    private var lastLocation: Location? = null
    private var destStationIndex: Int = -1
    private var lastStationIndex: Int = -1
    private var lastAdvancedCheckTime: Long = 0L

    companion object {
        const val TAG = "BusAlert"
        const val ACTION_START_RIDE = "ACTION_START_RIDE"
        const val ACTION_START_ARRIVAL = "ACTION_START_ARRIVAL"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_STOP_RIDE = "ACTION_STOP_RIDE"
        const val ACTION_STOP_ARRIVAL_ALL = "ACTION_STOP_ARRIVAL_ALL"
        const val ACTION_STOP_ALERT = "ACTION_STOP_ALERT"
        const val ACTION_REFRESH = "ACTION_REFRESH"
        const val EXTRA_ALERT_ID = "EXTRA_ALERT_ID"
        const val CHANNEL_ID = "bus_alert_channel"
        const val NOTIFICATION_ID = 1001
    }

    enum class Mode { IDLE, RIDE, ARRIVAL }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            lastLocation = locationResult.lastLocation
            if (mode == Mode.RIDE) {
                checkBoardingStatusWithTimeout()
                if (isBoardingDetected) { checkRideStatus() }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else { @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.KOREAN)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) { isTtsReady = true }
        }
    }

    private fun speak(text: String) { if (isTtsReady) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) } }

    private fun notifyWidgetUpdate() {
        val intent = Intent(this, com.czt.bbt.widget.BusAlertWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(application).getAppWidgetIds(android.content.ComponentName(application, com.czt.bbt.widget.BusAlertWidget::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RIDE -> { startRideMode(intent.getLongExtra(EXTRA_ALERT_ID, -1)) }
            ACTION_START_ARRIVAL -> { startArrivalMode(intent.getLongExtra(EXTRA_ALERT_ID, -1)) }
            ACTION_REFRESH -> {
                serviceScope.launch {
                    if (mode == Mode.RIDE) { if (isBoardingDetected) checkRideStatus() else updateNotification("승차 확인 중...") }
                    activeArrivalAlerts.keys.forEach { checkArrivalStatus(it) }
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
            ACTION_STOP_ALERT -> { stopIndividualAlert(intent.getLongExtra(EXTRA_ALERT_ID, -1)) }
            ACTION_STOP_RIDE -> stopRideOnly()
            ACTION_STOP_ARRIVAL_ALL -> stopArrivalAll()
            ACTION_STOP -> handleManualStop()
        }
        return START_NOT_STICKY
    }

    private fun stopRideOnly() {
        mode = Mode.IDLE; activeRideAlert = null; lastAlertStops = -1; notificationManager.cancel(NOTIFICATION_ID)
        serviceScope.launch {
            val histories = repository.getAllRideHistories().first()
            val last = histories.maxByOrNull { it.boardingTime }
            if (last != null && last.alightTime == null) { repository.deleteRideHistory(last) }
        }
        getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE).edit().putLong("active_ride_id", -1L).commit()
        notifyWidgetUpdate(); if (activeArrivalJobs.isEmpty()) stopSelf()
    }

    private fun stopArrivalAll() {
        activeArrivalJobs.keys.toList().forEach { id ->
            activeArrivalJobs[id]?.cancel()
            notificationManager.cancel(NOTIFICATION_ID + id.toInt())
        }
        activeArrivalJobs.clear(); activeArrivalAlerts.clear(); lastArrivalAlertStops.clear()
        getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE).edit().putString("active_arrival_ids", "").commit()
        notifyWidgetUpdate(); if (mode != Mode.RIDE) stopSelf()
    }

    private fun handleManualStop() {
        serviceScope.launch {
            val histories = repository.getAllRideHistories().first()
            val last = histories.maxByOrNull { it.boardingTime }
            if (last != null && last.alightTime == null) { repository.deleteRideHistory(last) }
            getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE).edit().putLong("active_ride_id", -1L).putString("active_arrival_ids", "").commit()
            lastAlertStops = -1; lastArrivalAlertStops.clear()
            notifyWidgetUpdate(); stopSelf()
        }
    }

    private fun stopIndividualAlert(alertId: Long) {
        activeArrivalJobs[alertId]?.cancel(); activeArrivalJobs.remove(alertId); activeArrivalAlerts.remove(alertId); lastArrivalAlertStops.remove(alertId)
        notificationManager.cancel(NOTIFICATION_ID + alertId.toInt())
        val prefs = getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE)
        val newIds = (prefs.getString("active_arrival_ids", "") ?: "").split(",").filter { it != alertId.toString() && it.isNotEmpty() }.joinToString(",")
        prefs.edit().putString("active_arrival_ids", newIds).commit()
        notifyWidgetUpdate(); if (activeArrivalJobs.isEmpty() && mode != Mode.RIDE) { stopSelf() }
    }

    private fun startRideMode(alertId: Long) {
        if (mode == Mode.RIDE) { sensorManager.unregisterListener(this) }
        mode = Mode.RIDE; isBoardingDetected = false; potentialBoardingTime = 0L; lastAlertStops = -1; lastStationIndex = -1
        getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE).edit().putLong("active_ride_id", alertId).commit()
        startForeground(NOTIFICATION_ID, createNotification("버스 이동 알림 준비 중..."))
        startLocationUpdates(); notifyWidgetUpdate()
        serviceScope.launch {
            val histories = repository.getAllRideHistories().first()
            val last = histories.maxByOrNull { it.boardingTime }
            if (last != null && last.alightTime == null) { repository.deleteRideHistory(last) }
            
            try {
                val alert = repository.getAllRideAlerts().first().find { it.id == alertId } ?: run { stopSelf(); return@launch }
                activeRideAlert = alert
                val stations = repository.getBusRouteStations(alert.busRouteId)
                routeStationsCache[alert.busRouteId] = stations
                destStationIndex = stations.indexOfFirst { it.stationId == alert.destinationStationId }
                var retries = 0
                while (lastLocation == null && retries < 5) { delay(1000); retries++ }
                lastLocation?.let { loc ->
                    val nearest = stations.minByOrNull { val r = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, it.y, it.x, r); r[0] }
                    boardingStationName = nearest?.stationName
                    updateNotification("승차 대기 중: ${boardingStationName ?: "위치 확인 중"}")
                    sensorManager.registerListener(this@BusAlertService, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
                } ?: run { stopSelf() }
            } catch (e: Exception) { stopSelf() }
        }
    }

    private fun checkBoardingStatusWithTimeout() {
        if (isBoardingDetected || potentialBoardingTime == 0L) return
        val now = System.currentTimeMillis()
        
        if (now - potentialBoardingTime > 3 * 60 * 1000) {
            revertToWaitingStatus()
            return
        }

        val loc = lastLocation ?: return
        val routeId = activeRideAlert?.busRouteId ?: return
        val stations = routeStationsCache[routeId] ?: return
        val station = stations.find { it.stationName == boardingStationName } ?: return
        val dist = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, station.y, station.x, dist)

        if (dist[0] > 70) {
            confirmBoarding()
        }
    }

    private fun revertToWaitingStatus() {
        if (isBoardingDetected) return
        potentialBoardingTime = 0L
        updateNotification("승차 대기 중: ${boardingStationName ?: "위치 확인 중"}")
        serviceScope.launch { repository.logSystem("RIDE_REVERT", "3분 경과 미이탈로 승차 대기 전환") }
        sensorManager.unregisterListener(this)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun startArrivalMode(alertId: Long) {
        serviceScope.launch {
            val alerts = repository.getAllArrivalAlerts().first()
            val alert = alerts.find { it.id == alertId } ?: return@launch
            activeArrivalAlerts[alertId] = alert
            
            val prefs = getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE)
            val currentIds = prefs.getString("active_arrival_ids", "") ?: ""
            val idList = currentIds.split(",").filter { it.isNotEmpty() }.toMutableList()
            if (!idList.contains(alertId.toString())) {
                idList.add(alertId.toString())
                prefs.edit().putString("active_arrival_ids", idList.joinToString(",")).commit()
            }

            updateArrivalNotification(alertId, "버스도착알림 : ${alert.stationName}", "도착 정보 확인 중...")
            notifyWidgetUpdate()
            lastArrivalAlertStops[alertId] = -1
            
            val job = launch { 
                while (isActive) { 
                    val nextDelay = checkArrivalStatus(alertId)
                    delay(nextDelay) 
                } 
            }
            activeArrivalJobs[alertId] = job
        }
    }

    private suspend fun checkArrivalStatus(alertId: Long): Long {
        val alert = activeArrivalAlerts[alertId] ?: return 300000L
        Log.d(TAG, "[$alertId] 도착 상태 체크 시작: ${alert.stationName}")
        
        try {
            val alertBuses = alert.targetBusNumbers.toSet()
            val results = mutableListOf<Triple<String, String, Int>>()

            Log.d(TAG, "[$alertId] API 호출: getBusArrivalListV2(${alert.stationId})")
            val res = repository.getBusArrivalListV2(alert.stationId)
            val arrivalList = res.response.msgBody?.busArrivalList ?: emptyList()
            Log.d(TAG, "[$alertId] API 응답 수신: ${arrivalList.size}건")
            
            val filteredArrivals = arrivalList.filter { it.routeId.toString() in alertBuses }
            Log.d(TAG, "[$alertId] 필터링된 도착 정보: ${filteredArrivals.size}건")
            
            alert.targetBusNumbers.forEachIndexed { index, rId ->
                try {
                    val busName = alert.targetBusNames.getOrNull(index) ?: "버스"
                    val item = filteredArrivals.find { it.routeId.toString() == rId }
                    
                    Log.d(TAG, "[$alertId] 버스 처리 시작: $busName ($rId)")
                    
                    val routeArrivals = arrivalList.filter { it.routeId.toString() == rId }
                    val estimate = calculateDetailedEstimate(rId, alert.stationId, null, routeArrivals)
                    
                    if (estimate != null) {
                        Log.d(TAG, "[$alertId] 정밀 계산 성공: $busName -> ${estimate.first}초 (${estimate.second})")
                        results.add(Triple(busName, rId, estimate.first))
                    } else if (item != null) {
                        val p1 = item.predictTimeSec1.toIntSafe()
                        if (p1 > 0) {
                            Log.d(TAG, "[$alertId] 기본 정보 사용: $busName -> ${p1}초")
                            results.add(Triple(busName, rId, p1))
                        }
                    }
                } catch (busEx: Exception) {
                    Log.e(TAG, "[$alertId] 개별 버스($rId) 처리 중 예외: ${busEx.message}")
                }
            }

            val title = "버스도착알림 : ${alert.stationName}"
            if (results.isEmpty()) {
                updateArrivalNotification(alertId, title, "운행 정보 없음 (차고지 대기 등)")
                return 60000L
            }

            val sorted = results.sortedBy { it.third }
            val minTimeSec = sorted.first().third
            val earliestRouteId = sorted.first().second
            val busName = sorted.first().first

            if (lastAnnouncedRouteId[alertId] != earliestRouteId) {
                lastAnnouncedRouteId[alertId] = earliestRouteId
                lastArrivalAlertStops[alertId] = -1
            }

            if (minTimeSec <= 180) {
                val threshold = if (minTimeSec <= 65) 1 else 3
                if (lastArrivalAlertStops[alertId] != threshold) {
                    triggerAlertEffects()
                    speak(if (threshold == 1) "$busName 버스가 잠시 후 도착합니다." else "$busName 버스가 약 3분 후에 도착합니다.")
                    lastArrivalAlertStops[alertId] = threshold
                }
            }

            val content = sorted.joinToString("\n") { (name, _, time) ->
                val minutes = time / 60; val seconds = time % 60
                "[$name] ${if (minutes > 0) "${minutes}분 ${seconds}초" else "${seconds}초"} 후 도착"
            }
            updateArrivalNotification(alertId, title, content)
            Log.d(TAG, "[$alertId] 알림 업데이트 성공")
            
            return when {
                minTimeSec > 300 -> 60000L
                minTimeSec > 180 -> 30000L
                else -> 15000L
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$alertId] checkArrivalStatus 치명적 실패: ${e.message}")
            repository.logSystem("ARRIVAL_FATAL", "알림ID($alertId) 실패: ${e.message}")
            updateArrivalNotification(alertId, "버스도착알림 : ${alert.stationName}", "정보 갱신 시도 중...")
            return 60000L
        }
    }

    private fun updateArrivalNotification(alertId: Long, title: String, content: String) {
        val refreshIntent = PendingIntent.getService(this, alertId.toInt(), Intent(this, BusAlertService::class.java).setAction(ACTION_REFRESH), PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = PendingIntent.getService(this, alertId.toInt() + 100, Intent(this, BusAlertService::class.java).setAction(ACTION_STOP_ALERT).putExtra(EXTRA_ALERT_ID, alertId), PendingIntent.FLAG_IMMUTABLE)
        val notificationId = NOTIFICATION_ID + alertId.toInt()
        
        val safeContent = content.ifEmpty { "정보를 불러오는 중입니다..." }
        val firstLine = safeContent.split("\n")[0]

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(firstLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(safeContent))
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentIntent(refreshIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "알림중지", stopIntent)
            .build()
            
        if (activeArrivalJobs.size == 1 && mode != Mode.RIDE) { startForeground(notificationId, notification) } 
        else { notificationManager.notify(notificationId, notification) }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (mode == Mode.RIDE && !isBoardingDetected && event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
            val acc = sqrt(x*x + y*y + z*z)
            if (acc > 13 && potentialBoardingTime == 0L) {
                potentialBoardingTime = System.currentTimeMillis()
                updateNotification("승차 확인 중... (움직임 감지)")
            }
        }
    }

    private fun confirmBoarding() {
        if (isBoardingDetected) return
        isBoardingDetected = true; sensorManager.unregisterListener(this); boardingTime = System.currentTimeMillis()
        serviceScope.launch {
            try {
                val routeId = activeRideAlert?.busRouteId ?: return@launch
                val stations = routeStationsCache[routeId] ?: return@launch
                val res = repository.getBusLocations(routeId)
                val locs = parseLocationList(res.response.msgBody?.busLocationList).filterNotNull()
                val boardingSeq = stations.indexOfFirst { it.stationName == boardingStationName }.takeIf { it != -1 } ?: 0
                lastStationIndex = boardingSeq
                val myBus = locs.minByOrNull { bus -> kotlin.math.abs(bus.stationSeq - boardingSeq) }
                currentBusPlate = myBus?.plateNo ?: "확인 불가"
                
                val destSeq = stations.indexOfFirst { it.stationId == activeRideAlert?.destinationStationId }.takeIf { it != -1 } ?: (boardingSeq + 1)
                val estStops = (destSeq - boardingSeq).coerceAtLeast(1)
                val estDurationMin = estStops * 2
                val estArrivalTime = boardingTime + (estDurationMin * 60000L)
                val estArrivalStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(estArrivalTime))
                
                val dbDate = SimpleDateFormat("yyyy-MM-dd (E)", Locale.KOREAN).format(Date(boardingTime))
                repository.insertRideHistory(RideHistory(
                    date = dbDate, 
                    boardingTime = boardingTime, 
                    boardingStationName = boardingStationName ?: "알 수 없는 정류장", 
                    busNumber = activeRideAlert!!.busNumber, 
                    plateNumber = currentBusPlate,
                    alightStationName = activeRideAlert!!.destinationStationName
                ))
                
                updateNotification("승차 확인! 버스번호: ${activeRideAlert!!.busNumber} (${currentBusPlate})")
                val estText = "도착예상: $estArrivalStr (${estDurationMin}분 소요 예상)"
                shareStatus("승차", activeRideAlert!!.busNumber, currentBusPlate ?: "확인 불가", boardingTime, boardingStationName ?: "", extraInfo = estText)
            } catch (e: Exception) { }
        }
    }

    private suspend fun calculateDetailedEstimate(
        routeId: String, 
        targetStationId: String, 
        targetPlateNo: String?, 
        cachedArrivals: List<GBusArrivalInfoItem>? = null
    ): Pair<Int, String>? {
        try {
            val stations = repository.getBusRouteStations(routeId)
            val locRes = repository.getBusLocations(routeId)
            val busLocs = parseLocationList(locRes.response.msgBody?.busLocationList).filterNotNull()
            if (busLocs.isEmpty()) return null

            val arrivalItems = cachedArrivals ?: repository.getBusArrivalListV2(targetStationId).response.msgBody?.busArrivalList?.filter { it.routeId.toString() == routeId } ?: emptyList()
            
            val myBusLoc = if (targetPlateNo != null) busLocs.find { it.plateNo == targetPlateNo } 
                           else busLocs.minByOrNull { it.stationSeq }
            val mySeq = myBusLoc?.stationSeq ?: return null
            
            val directMatch = arrivalItems.firstOrNull { it.plateNo1 == targetPlateNo || it.plateNo2 == targetPlateNo }
            if (directMatch != null) {
                val sec = if (directMatch.plateNo1 == targetPlateNo) directMatch.predictTimeSec1.toIntSafe() else directMatch.predictTimeSec2.toIntSafe()
                return sec to "실시간"
            }
            
            if (arrivalItems.isNotEmpty()) {
                val leadBusItem = arrivalItems.first()
                val leadPlate = leadBusItem.plateNo1
                val leadSec = leadBusItem.predictTimeSec1.toIntSafe()
                val leadLoc = busLocs.find { it.plateNo == leadPlate }
                
                if (leadLoc != null && mySeq < leadLoc.stationSeq) {
                    val stopDiff = leadLoc.stationSeq - mySeq
                    val estSec = leadSec + (stopDiff * 120)
                    return estSec to "체인추적"
                }
            }
            
            val totalStations = stations.size
            val activeBusCount = busLocs.size
            if (activeBusCount > 0) {
                val avgIntervalStops = totalStations / activeBusCount
                val nearestAhead = busLocs.filter { it.stationSeq > mySeq }.minByOrNull { it.stationSeq - mySeq }
                                   ?: busLocs.minByOrNull { it.stationSeq }
                
                nearestAhead?.let { ahead ->
                    val stopsToAhead = if (ahead.stationSeq > mySeq) ahead.stationSeq - mySeq 
                                       else (totalStations - mySeq) + ahead.stationSeq
                    
                    val aheadItem = arrivalItems.find { it.plateNo1 == ahead.plateNo }
                    val baseSec = aheadItem?.predictTimeSec1.let { if (it == null || it.toString().isEmpty()) null else it.toIntSafe() } ?: (avgIntervalStops * 120)
                    val finalEstSec = baseSec + (stopsToAhead * 120)
                    return finalEstSec to "순환평균"
                }
            }
        } catch (e: Exception) { Log.e(TAG, "[$routeId] 계산 엔진 예외: ${e.message}") }
        return null
    }

    private fun shareStatus(type: String, busNo: String, plateNo: String, time: Long, station: String, summary: String = "", extraInfo: String = "") {
        val alert = activeRideAlert ?: return
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))
        val dateWithDay = SimpleDateFormat("yyyy-MM-dd (E)", Locale.KOREAN).format(Date(time))
        
        serviceScope.launch {
            val allHistories = repository.getAllRideHistories().first()
            val filteredLogs = allHistories.filter { it.busNumber == busNo }.sortedByDescending { it.boardingTime }
            val previousLogs = filteredLogs.filter { it.boardingTime != (if (type == "승차") time else boardingTime) }.take(3)
            val historySection = if (previousLogs.isNotEmpty()) {
                "\n\n[이 노선 과거 이용 기록]\n" + previousLogs.joinToString("\n") { h ->
                    val hDateWithDay = SimpleDateFormat("yyyy-MM-dd (E)", Locale.KOREAN).format(Date(h.boardingTime))
                    val bTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(h.boardingTime))
                    val aTime = if (h.alightTime != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(h.alightTime)) else "??"
                    val duration = if (h.alightTime != null) " (${(h.alightTime - h.boardingTime) / 60000}분)" else ""
                    val pNo = h.plateNumber ?: "차량미확인"
                    val bSt = h.boardingStationName.replace("(", " [").replace(")", "]")
                    val aSt = h.alightStationName?.replace("(", " [")?.replace(")", "]") ?: "종료됨"
                    "• $hDateWithDay $bTime~$aTime$duration $pNo ($bSt → $aSt)"
                }
            } else ""

            val mainContent = if (summary.isNotEmpty()) summary else {
                "버스: ${busNo}번 ($plateNo)\n일자: $dateWithDay\n탑승시간: $timeStr\n" + (if (extraInfo.isNotEmpty()) "$extraInfo\n" else "") +
                "승차정류장: ${station.replace("(", " [").replace(")", "]")}\n목적정류장: ${alert.destinationStationName.replace("(", " [").replace(")", "]")}"
            }
            val finalContent = mainContent + historySection
            alert.shareEmails.forEach { com.czt.bbt.util.NotificationHelper.sendEmail(this@BusAlertService, busNo, plateNo, timeStr, station, type, finalContent) }
            if (alert.shareKakao) { com.czt.bbt.util.NotificationHelper.sendKakaoMessage(this@BusAlertService, busNo, plateNo, timeStr, station, type, finalContent) }
        }
    }

    private fun checkRideStatus() {
        if (!isBoardingDetected) return
        val alert = activeRideAlert ?: return; val loc = lastLocation ?: return
        val stations = routeStationsCache[alert.busRouteId] ?: return
        val searchRange = if (lastStationIndex != -1) stations.drop(lastStationIndex) else stations
        val offset = if (lastStationIndex != -1) lastStationIndex else 0
        val nearestIdx = searchRange.indexOfMinByOrNull { val res = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, it.y, it.x, res); res[0] } ?: -1
        if (nearestIdx != -1) { val currentIdx = nearestIdx + offset; if (currentIdx > lastStationIndex) lastStationIndex = currentIdx }
        val currentIdx = lastStationIndex
        if (currentIdx != -1 && destStationIndex != -1) {
            val stopsRemaining = destStationIndex - currentIdx; val destStation = stations[destStationIndex]
            val distToDest = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, destStation.y, destStation.x, distToDest)
            if (distToDest[0] < 150) { handleAlight() } else {
                val nextStationName = if (currentIdx + 1 < stations.size) stations[currentIdx + 1].stationName else "종점"
                var timeText = "약 ${stopsRemaining * 2}분"
                val distText = "${String.format("%.3f", distToDest[0]/1000.0)}km"
                val now = System.currentTimeMillis()
                if (now - lastAdvancedCheckTime > 60000L || stopsRemaining <= 3) {
                    lastAdvancedCheckTime = now
                    serviceScope.launch {
                        try {
                            val locRes = repository.getBusLocations(alert.busRouteId)
                            val busLocs = parseLocationList(locRes.response.msgBody?.busLocationList).filterNotNull()
                            val arrRes = repository.getBusArrivalListV2(alert.destinationStationId)
                            val arrivalItems = arrRes.response.msgBody?.busArrivalList?.filter { it.routeId.toString() == alert.busRouteId } ?: emptyList()
                            val myLoc = busLocs.find { it.plateNo == currentBusPlate }; val mySeq = myLoc?.stationSeq ?: stations[currentIdx].stationSeq
                            val anchors = mutableListOf<Pair<Int, Int>>()
                            arrivalItems.forEach { item -> 
                                item.plateNo1?.let { p -> busLocs.find { it.plateNo == p }?.let { loc -> anchors.add(loc.stationSeq to item.predictTimeSec1.toIntSafe()) } }
                                item.plateNo2?.let { p -> busLocs.find { it.plateNo == p }?.let { loc -> anchors.add(loc.stationSeq to item.predictTimeSec2.toIntSafe()) } }
                            }
                            anchors.sortByDescending { it.first }
                            var bestEstimateSec: Int? = null
                            val directMatch = arrivalItems.firstOrNull { it.plateNo1 == currentBusPlate || it.plateNo2 == currentBusPlate }
                            if (directMatch != null) { bestEstimateSec = if (directMatch.plateNo1 == currentBusPlate) directMatch.predictTimeSec1.toIntSafe() else directMatch.predictTimeSec2.toIntSafe() }
                            else if (anchors.isNotEmpty()) {
                                val nearestAhead = anchors.filter { it.first > mySeq }.lastOrNull()
                                if (nearestAhead != null) {
                                    var dynamicSecPerStop = 100.0
                                    if (anchors.size >= 2) { val firstA = anchors[0]; val lastA = anchors.last(); if (firstA.first != lastA.first) dynamicSecPerStop = (lastA.second - firstA.second).toDouble() / (firstA.first - lastA.first).toDouble().coerceIn(30.0, 300.0) }
                                    bestEstimateSec = nearestAhead.second + ((nearestAhead.first - mySeq) * dynamicSecPerStop).toInt()
                                }
                            }
                            if (bestEstimateSec != null) {
                                val mins = bestEstimateSec / 60; val secs = bestEstimateSec % 60
                                updateNotification("다음: $nextStationName | 약 ${if (mins > 0) "${mins}분 " else ""}${secs}초 (${stopsRemaining}전) | $distText")
                                return@launch
                            }
                        } catch (e: Exception) { repository.logSystem("ADV_TIME_ERR", "고급 계산 실패: ${e.message}") }
                    }
                }
                updateNotification("다음: $nextStationName | $timeText (${stopsRemaining}전) | $distText")
                if (stopsRemaining <= 2 && stopsRemaining != lastAlertStops) {
                    if (stopsRemaining == 2) { triggerAlertEffects(); speak("도착 2정류장 전입니다."); shareStatus("도착 2정거장 전", alert.busNumber, currentBusPlate ?: "확인불가", System.currentTimeMillis(), alert.destinationStationName); lastAlertStops = 2 }
                    else if (stopsRemaining == 1) { triggerAlertEffects(); speak("이제 도착합니다. 이번에 하차하세요."); lastAlertStops = 1 }
                }
            }
        }
    }

    private fun handleAlight() {
        if (isAlightingDetected) return
        isAlightingDetected = true; triggerAlertEffects(); speak("목적지에 도착하여 서비스를 종료합니다."); updateNotification("하차 완료: 서비스를 종료합니다.")
        serviceScope.launch {
            val alert = activeRideAlert; val plate = currentBusPlate ?: "확인불가"; val now = System.currentTimeMillis()
            if (alert != null) {
                val bTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(boardingTime)); val aTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
                val summary = "[주행 완료 보고]\n버스: ${alert.busNumber} ($plate)\n일자: ${SimpleDateFormat("yyyy-MM-dd (E)", Locale.KOREAN).format(Date(now))}\n승차: $bTime (${boardingStationName ?: "알 수 없음"})\n하차: $aTime (${alert.destinationStationName})\n소요 시간: ${(now - boardingTime) / 60000}분"
                shareStatus("하차", alert.busNumber, plate, now, alert.destinationStationName, summary)
            }
            val histories = repository.getAllRideHistories().first(); val last = histories.maxByOrNull { it.boardingTime }
            if (last != null && last.alightTime == null) { repository.updateRideHistory(last.copy(alightTime = System.currentTimeMillis(), alightStationName = activeRideAlert?.destinationStationName)) }
            mode = Mode.IDLE; activeRideAlert = null; lastAlertStops = -1; getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE).edit().putLong("active_ride_id", -1L).commit(); notifyWidgetUpdate()
            delay(10000); if (activeArrivalJobs.isEmpty()) stopSelf() else notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    private fun triggerAlertEffects() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1)) else @Suppress("DEPRECATION") vibrator.vibrate(1000)
        try { android.media.RingtoneManager.getRingtone(applicationContext, android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)).play() } catch (e: Exception) {}
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000).setMinUpdateIntervalMillis(10000).build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "버스 알림 서비스", NotificationManager.IMPORTANCE_LOW))
    }

    private fun createNotification(content: String): Notification {
        val refreshIntent = PendingIntent.getService(this, 1, Intent(this, BusAlertService::class.java).setAction(ACTION_REFRESH), PendingIntent.FLAG_IMMUTABLE)
        val busNumber = activeRideAlert?.busNumber ?: ""
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (busNumber.isNotEmpty()) "버스이동알림 : $busNumber" else "버스이동알림").setContentText(content).setSmallIcon(R.mipmap.ic_launcher_foreground).setContentIntent(refreshIntent).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "알림중지", PendingIntent.getService(this, 0, Intent(this, BusAlertService::class.java).setAction(ACTION_STOP_RIDE), PendingIntent.FLAG_IMMUTABLE)).build()
    }

    private fun updateNotification(content: String) { notificationManager.notify(NOTIFICATION_ID, createNotification(content)) }

    private fun parseRouteStations(data: Any?): List<com.czt.bbt.api.GBusRouteStationItem> {
        val json = gson.toJson(data ?: return emptyList())
        return try { val list = if (json.startsWith("[")) gson.fromJson<List<com.czt.bbt.api.GBusRouteStationItem>>(json, object : TypeToken<List<com.czt.bbt.api.GBusRouteStationItem>>() {}.type) else listOf(gson.fromJson(json, com.czt.bbt.api.GBusRouteStationItem::class.java)); list.filterNotNull() } catch (e: Exception) { emptyList() }
    }

    private fun parseLocationList(data: Any?): List<com.czt.bbt.api.GBusLocationItem> {
        val json = gson.toJson(data ?: return emptyList())
        return try { val list = if (json.startsWith("[")) gson.fromJson<List<com.czt.bbt.api.GBusLocationItem>>(json, object : TypeToken<List<com.czt.bbt.api.GBusLocationItem>>() {}.type) else listOf(gson.fromJson(json, com.czt.bbt.api.GBusLocationItem::class.java)); list.filterNotNull() } catch (e: Exception) { emptyList() }
    }
    
    private fun <T : Any> List<T>.indexOfMinByOrNull(selector: (T) -> Float): Int? {
        if (isEmpty()) return null
        var minIndex = 0; var minValue = selector(this[0])
        for (i in 1 until size) { val v = selector(this[i]); if (v < minValue) { minValue = v; minIndex = i } }
        return minIndex
    }

    private fun Any?.toIntSafe(): Int {
        if (this == null) return 0
        val s = this.toString()
        if (s.isEmpty() || s == "null") return 0
        return try { if (s.contains(".")) s.toDouble().toInt() else s.toInt() } catch (e: Exception) { 0 }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); fusedLocationClient.removeLocationUpdates(locationCallback); sensorManager.unregisterListener(this); serviceScope.cancel(); super.onDestroy() }
}
