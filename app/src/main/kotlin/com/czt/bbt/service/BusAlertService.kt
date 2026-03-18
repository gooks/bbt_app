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
import androidx.core.app.NotificationCompat
import com.czt.bbt.MainActivity
import com.czt.bbt.R
import com.czt.bbt.data.BusRepository
import com.czt.bbt.model.*
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
        
        // 3분이 지났는데 아직 승차가 확정되지 않았다면 대기 상태로 회귀 (위치 정보 유무와 무관)
        if (now - potentialBoardingTime > 3 * 60 * 1000) {
            revertToWaitingStatus()
            return
        }

        val loc = lastLocation ?: return
        val routeId = activeRideAlert?.busRouteId ?: return
        val stations = routeStationsCache[routeId] ?: return
        val station = stations.find { it.stationName == boardingStationName } ?: return
        val dist = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, station.y, station.x, dist)

        // 정류장을 70m 이상 확실히 벗어나면 승차 확정 (GPS 오차 고려하여 50m -> 70m 상향)
        if (dist[0] > 70) {
            confirmBoarding()
        }
    }

    private fun revertToWaitingStatus() {
        if (isBoardingDetected) return
        potentialBoardingTime = 0L
        updateNotification("승차 대기 중: ${boardingStationName ?: "위치 확인 중"}")
        serviceScope.launch { repository.logSystem("RIDE_REVERT", "3분 경과 미이탈로 승차 대기 전환") }
        
        // 안전하게 리스너 재등록
        sensorManager.unregisterListener(this)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private val stationOrderCache = mutableMapOf<String, Int>()

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
        val alert = activeArrivalAlerts[alertId] ?: return 300000L // 5분
        
        try {
            val alertBuses = alert.targetBusNumbers.toSet()
            val results = mutableListOf<Triple<String, String, Int>>() // busName, routeId, predictTimeSec

            // 단일 버스 + 캐시된 staOrder가 있는 경우
            if (alertBuses.size == 1) {
                val routeId = alertBuses.first()
                val cacheKey = "$routeId/${alert.stationId}"
                val staOrder = stationOrderCache[cacheKey]
                if (staOrder != null) {
                    val res = repository.getBusArrivalItemV2(alert.stationId, routeId, staOrder.toString())
                    res.response.msgBody?.busArrivalItem?.let {
                        val busName = alert.targetBusNames.firstOrNull() ?: "버스"
                        it.predictTimeSec1?.let { time -> results.add(Triple(busName, routeId, time)) }
                    }
                }
            }
            
            // 그 외의 경우 (다수 버스, 캐시 없음 등)
            if (results.isEmpty()) {
                val res = repository.getBusArrivalListV2(alert.stationId)
                res.response.msgBody?.busArrivalList?.filter { it.routeId.toString() in alertBuses }?.forEach { item ->
                    val busName = alert.targetBusNames.getOrNull(alert.targetBusNumbers.indexOf(item.routeId.toString())) ?: item.routeName ?: "버스"
                    item.predictTimeSec1?.let { time ->
                        results.add(Triple(busName, item.routeId.toString(), time))
                        // staOrder 캐싱
                        val cacheKey = "${item.routeId}/${alert.stationId}"
                        if (!stationOrderCache.containsKey(cacheKey)) {
                            stationOrderCache[cacheKey] = item.staOrder
                        }
                    }
                }
            }

            val title = "버스도착알림 : ${alert.stationName}"
            if (results.isEmpty()) {
                updateArrivalNotification(alertId, title, "운행 중인 버스 정보 없음")
                return 300000L // 5분
            }

            val sorted = results.sortedBy { it.third }
            val earliestBus = sorted.first()
            val minTimeSec = earliestBus.third
            val earliestRouteId = earliestBus.second
            val busName = earliestBus.first

            // 가장 빨리 도착하는 버스가 변경되었다면 안내 상태 초기화
            if (lastAnnouncedRouteId[alertId] != earliestRouteId) {
                lastAnnouncedRouteId[alertId] = earliestRouteId
                lastArrivalAlertStops[alertId] = -1
            }

            // TTS 알림 (버스 이동 알림처럼 특정 임계치에서 안내)
            if (minTimeSec <= 180) { // 3분 이내
                val threshold = when {
                    minTimeSec <= 65 -> 1  // 약 1분 전 (잠시 후)
                    minTimeSec <= 185 -> 3 // 약 3분 전
                    else -> -1
                }

                if (threshold != -1 && lastArrivalAlertStops[alertId] != threshold) {
                    triggerAlertEffects()
                    if (threshold == 1) {
                        speak("$busName 버스가 잠시 후 도착합니다. 승차 준비를 해주세요.")
                    } else {
                        speak("$busName 버스가 약 3분 후에 도착합니다.")
                    }
                    lastArrivalAlertStops[alertId] = threshold
                }
            }

            val content = sorted.joinToString("\n") { (name, _, time) ->
                val minutes = time / 60
                val seconds = time % 60
                "[$name] ${if (minutes > 0) "${minutes}분 ${seconds}초" else "${seconds}초"} 후 도착"
            }
            updateArrivalNotification(alertId, title, content)
            
            // 동적 딜레이 반환
            return when {
                minTimeSec > 300 -> 60000L // 5분 이상 남음: 60초
                minTimeSec > 180 -> 30000L // 3-5분 남음: 30초
                else -> 15000L // 3분 이하 남음: 15초
            }
        } catch (e: Exception) {
            repository.logSystem("ARRIVAL_ERROR", "알림ID($alertId) 갱신 실패: ${e.message}")
            updateArrivalNotification(alertId, "버스도착알림 : ${alert.stationName}", "정보 갱신 중 오류 발생: ${e.message}")
            return 60000L // 오류 시 1분 후 재시도
        }
    }

    private fun updateArrivalNotification(alertId: Long, title: String, content: String) {
        val refreshIntent = PendingIntent.getService(this, alertId.toInt(), Intent(this, BusAlertService::class.java).setAction(ACTION_REFRESH), PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = PendingIntent.getService(this, alertId.toInt() + 100, Intent(this, BusAlertService::class.java).setAction(ACTION_STOP_ALERT).putExtra(EXTRA_ALERT_ID, alertId), PendingIntent.FLAG_IMMUTABLE)
        val notificationId = NOTIFICATION_ID + alertId.toInt()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title).setContentText(content.split("\n")[0]).setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(R.mipmap.ic_launcher_foreground).setContentIntent(refreshIntent).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "알림중지", stopIntent).build()
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
                val locs = parseLocationList(res.response.msgBody?.busLocationList)
                val boardingSeq = stations.indexOfFirst { it.stationName == boardingStationName }.takeIf { it != -1 } ?: 0
                lastStationIndex = boardingSeq
                val myBus = locs.minByOrNull { bus -> kotlin.math.abs(bus.stationSeq - boardingSeq) }
                currentBusPlate = myBus?.plateNo ?: "확인 불가"
                
                // 예상 소요 시간 계산 (승차 시점 기준)
                val destSeq = stations.indexOfFirst { it.stationId == activeRideAlert?.destinationStationId }.takeIf { it != -1 } ?: (boardingSeq + 1)
                val estStops = (destSeq - boardingSeq).coerceAtLeast(1)
                val estDurationMin = estStops * 2 // 기본 2분 기준
                val estArrivalTime = boardingTime + (estDurationMin * 60000L)
                val estArrivalStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(estArrivalTime))
                
                val dbDate = SimpleDateFormat("yyyy-MM-dd (E)", Locale.KOREAN).format(Date(boardingTime))
                repository.insertRideHistory(RideHistory(
                    date = dbDate, 
                    boardingTime = boardingTime, 
                    boardingStationName = boardingStationName ?: "알 수 없는 정류장", 
                    busNumber = activeRideAlert!!.busNumber, 
                    plateNumber = currentBusPlate,
                    alightStationName = activeRideAlert!!.destinationStationName // UI에서 목적지 표시용으로 미리 저장
                ))
                
                updateNotification("승차 확인! 버스번호: ${activeRideAlert!!.busNumber} (${currentBusPlate})")
                
                val estText = "도착예상: $estArrivalStr (${estDurationMin}분 소요 예상)"
                shareStatus("승차", activeRideAlert!!.busNumber, currentBusPlate ?: "확인 불가", boardingTime, boardingStationName ?: "", extraInfo = estText)
            } catch (e: Exception) { }
        }
    }

    private suspend fun calculateDetailedEstimate(routeId: String, targetStationId: String, targetPlateNo: String?): Pair<Int, String>? {
        try {
            val stations = repository.getBusRouteStations(routeId)
            val locRes = repository.getBusLocations(routeId)
            val busLocs = parseLocationList(locRes.response.msgBody?.busLocationList)
            
            // 1. 목적지 정류소 도착 정보 조회
            val arrRes = repository.getBusArrivalListV2(targetStationId)
            val arrivalItems = arrRes.response.msgBody?.busArrivalList?.filter { it.routeId.toString() == routeId } ?: emptyList()
            
            // 2. 내 버스 찾기
            val myBusLoc = if (targetPlateNo != null) busLocs.find { it.plateNo == targetPlateNo } 
                           else busLocs.minByOrNull { it.stationSeq } // 기본값으로 첫번째 버스
            val mySeq = myBusLoc?.stationSeq ?: return null
            
            // 3. 직접 매칭 확인
            val directMatch = arrivalItems.firstOrNull { it.plateNo1 == targetPlateNo || it.plateNo2 == targetPlateNo }
            if (directMatch != null) {
                val sec = if (directMatch.plateNo1 == targetPlateNo) directMatch.predictTimeSec1 else directMatch.predictTimeSec2
                return (sec ?: 0) to "실시간"
            }
            
            // 4. 재귀적 구간 합산 (Chaining)
            if (arrivalItems.isNotEmpty()) {
                val leadBusItem = arrivalItems.first()
                val leadPlate = leadBusItem.plateNo1
                val leadSec = leadBusItem.predictTimeSec1 ?: 0
                val leadLoc = busLocs.find { it.plateNo == leadPlate }
                
                if (leadLoc != null && mySeq < leadLoc.stationSeq) {
                    val stopDiff = leadLoc.stationSeq - mySeq
                    val estSec = leadSec + (stopDiff * 120)
                    return estSec to "체인추적"
                }
            }
            
            // 5. 실시간 정보 없음 -> 순환 간격 평균 계산
            if (busLocs.size >= 2) {
                val totalStations = stations.size
                val activeBusCount = busLocs.size
                val avgIntervalStops = totalStations / activeBusCount
                
                val nearestAhead = busLocs.filter { it.stationSeq > mySeq }.minByOrNull { it.stationSeq - mySeq }
                                   ?: busLocs.minByOrNull { it.stationSeq }
                
                nearestAhead?.let { ahead ->
                    val stopsToAhead = if (ahead.stationSeq > mySeq) ahead.stationSeq - mySeq 
                                       else (totalStations - mySeq) + ahead.stationSeq
                    
                    val aheadArrRes = repository.getBusArrivalListV2(targetStationId)
                    val aheadItem = aheadArrRes.response.msgBody?.busArrivalList?.find { it.plateNo1 == ahead.plateNo }
                    
                    val baseSec = aheadItem?.predictTimeSec1 ?: (avgIntervalStops * 120)
                    val finalEstSec = baseSec + (stopsToAhead * 120)
                    return finalEstSec to "순환평균"
                }
            }
        } catch (e: Exception) { }
        return null
    }

    private fun shareStatus(type: String, busNo: String, plateNo: String, time: Long, station: String, summary: String = "", extraInfo: String = "") {
        val alert = activeRideAlert ?: return
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))
        val dateWithDay = SimpleDateFormat("yyyy-MM-dd (E)", Locale.KOREAN).format(Date(time))
        
        serviceScope.launch {
            // 해당 노선(busNumber)의 전체 이력 가져오기
            val allHistories = repository.getAllRideHistories().first()
            val filteredLogs = allHistories
                .filter { it.busNumber == busNo }
                .sortedByDescending { it.boardingTime }

            // 과거 기록 구성
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

            // 메시지 구성
            val mainContent = if (summary.isNotEmpty()) summary else {
                "버스: ${busNo}번 ($plateNo)\n" +
                "일자: $dateWithDay\n" +
                "탑승시간: $timeStr\n" +
                (if (extraInfo.isNotEmpty()) "$extraInfo\n" else "") +
                "승차정류장: ${station.replace("(", " [").replace(")", "]")}\n" +
                "목적정류장: ${alert.destinationStationName.replace("(", " [").replace(")", "]")}"
            }
            val finalContent = mainContent + historySection

            // 이메일 전송
            alert.shareEmails.forEach { email -> 
                com.czt.bbt.util.NotificationHelper.sendEmail(this@BusAlertService, busNo, plateNo, timeStr, station, type, finalContent) 
            }

            // 카카오톡 나에게 보내기 (자동)
            if (alert.shareKakao) {
                com.czt.bbt.util.NotificationHelper.sendKakaoMessage(this@BusAlertService, busNo, plateNo, timeStr, station, type, finalContent)
                repository.logSystem("SHARE_AUTO", "카톡 나에게 자동 전송 완료: $type")
            }
        }
    }

    private fun checkRideStatus() {        if (!isBoardingDetected) return
        val alert = activeRideAlert ?: return; val loc = lastLocation ?: return
        val stations = routeStationsCache[alert.busRouteId] ?: return
        
        // lastStationIndex 이후의 정류장만 검색하여 역주행 방지
        val searchRange = if (lastStationIndex != -1) stations.drop(lastStationIndex) else stations
        val offset = if (lastStationIndex != -1) lastStationIndex else 0
        
        val nearestIdx = searchRange.indexOfMinByOrNull { val res = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, it.y, it.x, res); res[0] } ?: -1
        
        if (nearestIdx != -1) {
            val currentIdx = nearestIdx + offset
            if (currentIdx > lastStationIndex) {
                lastStationIndex = currentIdx
            }
        }
        
        val currentIdx = lastStationIndex
        
        if (currentIdx != -1 && destStationIndex != -1) {
            val stopsRemaining = destStationIndex - currentIdx; val destStation = stations[destStationIndex]
            val distToDest = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, destStation.y, destStation.x, distToDest)
            
            if (distToDest[0] < 150) { 
                handleAlight() 
            } else {
                val nextStationName = if (currentIdx + 1 < stations.size) stations[currentIdx + 1].stationName else "종점"
                var timeText = "약 ${stopsRemaining * 2}분"
                val distText = "${String.format("%.3f", distToDest[0]/1000.0)}km"

                // 고급 계산 로직 (연쇄적 계산) - 1분 주기로 수행
                val now = System.currentTimeMillis()
                if (now - lastAdvancedCheckTime > 60000L || stopsRemaining <= 3) {
                    lastAdvancedCheckTime = now
                    serviceScope.launch {
                        try {
                            // 1. 전체 차량 위치 정보 가져오기
                            val locRes = repository.getBusLocations(alert.busRouteId)
                            val busLocs = parseLocationList(locRes.response.msgBody?.busLocationList)
                            
                            // 2. 목적지 정류소의 도착 예정 목록 가져오기
                            val arrRes = repository.getBusArrivalListV2(alert.destinationStationId)
                            val arrivalItems = arrRes.response.msgBody?.busArrivalList?.filter { it.routeId.toString() == alert.busRouteId } ?: emptyList()
                            
                            // 3. 내 버스의 stationSeq 확인
                            val myLoc = busLocs.find { it.plateNo == currentBusPlate }
                            val mySeq = myLoc?.stationSeq ?: stations[currentIdx].stationSeq
                            
                            // 4. 도착 예정 정보를 가진 앵커 버스들 매핑 (Seq, 남은시간)
                            val anchors = mutableListOf<Pair<Int, Int>>() // stationSeq, predictTimeSec
                            arrivalItems.forEach { item ->
                                item.plateNo1?.let { p -> busLocs.find { it.plateNo == p }?.let { loc -> anchors.add(loc.stationSeq to (item.predictTimeSec1 ?: 0)) } }
                                item.plateNo2?.let { p -> busLocs.find { it.plateNo == p }?.let { loc -> anchors.add(loc.stationSeq to (item.predictTimeSec2 ?: 0)) } }
                            }
                            // 목적지에 가까운 순(Seq 큰 순)으로 정렬
                            anchors.sortByDescending { it.first }
                            
                            var bestEstimateSec: Int? = null
                            
                            // 내 버스가 목록에 직접 있는 경우 (가장 정확)
                            val directMatch = arrivalItems.firstOrNull { it.plateNo1 == currentBusPlate || it.plateNo2 == currentBusPlate }
                            if (directMatch != null) {
                                bestEstimateSec = if (directMatch.plateNo1 == currentBusPlate) directMatch.predictTimeSec1 else directMatch.predictTimeSec2
                            } else if (anchors.isNotEmpty()) {
                                // 다중 앵커를 이용한 동적 구간 속도 계산
                                val nearestAhead = anchors.filter { it.first > mySeq }.lastOrNull() // 내 바로 앞 버스
                                val furthestAhead = anchors.filter { it.first > mySeq }.firstOrNull() // 가장 앞서가는 버스
                                
                                if (nearestAhead != null) {
                                    // 기본 정류장당 소요 시간 (100초로 설정, 실시간 데이터 없을 때의 기본값)
                                    var dynamicSecPerStop = 100.0
                                    
                                    // 앵커가 2개 이상이면 실제 구간 속도 계산
                                    if (anchors.size >= 2) {
                                        val firstA = anchors[0]
                                        val lastA = anchors.last()
                                        if (firstA.first != lastA.first) {
                                            dynamicSecPerStop = (lastA.second - firstA.second).toDouble() / (firstA.first - lastA.first).toDouble()
                                            // 비정상 데이터 방지 (30초 ~ 300초 사이로 제한)
                                            dynamicSecPerStop = dynamicSecPerStop.coerceIn(30.0, 300.0)
                                        }
                                    }
                                    
                                    val stopDiff = nearestAhead.first - mySeq
                                    bestEstimateSec = nearestAhead.second + (stopDiff * dynamicSecPerStop).toInt()
                                }
                            }
                            
                            if (bestEstimateSec != null) {
                                val minutes = bestEstimateSec / 60
                                val seconds = bestEstimateSec % 60
                                val accurateTimeText = if (minutes > 0) "약 ${minutes}분 ${seconds}초" else "약 ${seconds}초"
                                updateNotification("다음: $nextStationName | $accurateTimeText (${stopsRemaining}전) | $distText")
                                return@launch
                            }
                        } catch (e: Exception) {
                            repository.logSystem("ADV_TIME_ERR", "고급 계산 실패: ${e.message}")
                        }
                    }
                }
                
                updateNotification("다음: $nextStationName | $timeText (${stopsRemaining}전) | $distText")
                
                if (stopsRemaining <= 2 && stopsRemaining != lastAlertStops) {
                    if (stopsRemaining == 2) { 
                        triggerAlertEffects()
                        speak("도착 2정류장 전입니다.")
                        shareStatus("도착 2정거장 전", alert.busNumber, currentBusPlate ?: "확인불가", System.currentTimeMillis(), alert.destinationStationName)
                        lastAlertStops = 2
                    }
                    else if (stopsRemaining == 1) { 
                        triggerAlertEffects()
                        speak("이제 도착합니다. 이번에 하차하세요.")
                        lastAlertStops = 1
                    }
                }
            }
        }
    }

    private fun handleAlight() {
        if (isAlightingDetected) return
        isAlightingDetected = true
        triggerAlertEffects()
        speak("목적지에 도착하여 서비스를 종료합니다.")
        updateNotification("하차 완료: 서비스를 종료합니다.")
        
        serviceScope.launch {
            val alert = activeRideAlert; val plate = currentBusPlate ?: "확인불가"
            val now = System.currentTimeMillis()
            if (alert != null) {
                val bTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(boardingTime))
                val aTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
                val durationMin = (now - boardingTime) / 60000
                
                val summary = "[주행 완료 보고]\n" +
                        "버스: ${alert.busNumber} ($plate)\n" +
                        "승차: $bTime (${boardingStationName ?: "알 수 없음"})\n" +
                        "하차: $aTime (${alert.destinationStationName})\n" +
                        "소요 시간: ${durationMin}분"
                shareStatus("하차", alert.busNumber, plate, now, alert.destinationStationName, summary)
            }
            val histories = repository.getAllRideHistories().first(); val last = histories.maxByOrNull { it.boardingTime }
            if (last != null && last.alightTime == null) { repository.updateRideHistory(last.copy(alightTime = System.currentTimeMillis(), alightStationName = activeRideAlert?.destinationStationName)) }
            
            mode = Mode.IDLE
            activeRideAlert = null
            lastAlertStops = -1
            getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE).edit().putLong("active_ride_id", -1L).commit()
            notifyWidgetUpdate()
            
            delay(10000) // 전송 시간을 위해 대기 시간 연장
            if (activeArrivalJobs.isEmpty()) stopSelf() else notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    private fun triggerAlertEffects() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1)) } 
        else { @Suppress("DEPRECATION") vibrator.vibrate(1000) }
        try { android.media.RingtoneManager.getRingtone(applicationContext, android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)).play() } catch (e: Exception) {}
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000).setMinUpdateIntervalMillis(10000).build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { notificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "버스 알림 서비스", NotificationManager.IMPORTANCE_LOW)) }
    }

    private fun createNotification(content: String): Notification {
        val refreshIntent = PendingIntent.getService(this, 1, Intent(this, BusAlertService::class.java).setAction(ACTION_REFRESH), PendingIntent.FLAG_IMMUTABLE)
        val busNumber = activeRideAlert?.busNumber ?: ""
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (busNumber.isNotEmpty()) "버스이동알림 : $busNumber" else "버스이동알림").setContentText(content).setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentIntent(refreshIntent).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "알림중지", PendingIntent.getService(this, 0, Intent(this, BusAlertService::class.java).setAction(ACTION_STOP_RIDE), PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    private fun updateNotification(content: String) { notificationManager.notify(NOTIFICATION_ID, createNotification(content)) }

    private fun parseRouteStations(data: Any?): List<com.czt.bbt.api.GBusRouteStationItem> {
        val json = gson.toJson(data ?: return emptyList())
        return try { if (json.startsWith("[")) gson.fromJson(json, object : TypeToken<List<com.czt.bbt.api.GBusRouteStationItem>>() {}.type) else listOf(gson.fromJson(json, com.czt.bbt.api.GBusRouteStationItem::class.java)) } catch (e: Exception) { emptyList() }
    }

    private fun parseLocationList(data: Any?): List<com.czt.bbt.api.GBusLocationItem> {
        val json = gson.toJson(data ?: return emptyList())
        return try { if (json.startsWith("[")) gson.fromJson(json, object : TypeToken<List<com.czt.bbt.api.GBusLocationItem>>() {}.type) else listOf(gson.fromJson(json, com.czt.bbt.api.GBusLocationItem::class.java)) } catch (e: Exception) { emptyList() }
    }
    
    private fun <T : Any> List<T>.indexOfMinByOrNull(selector: (T) -> Float): Int? {
        if (isEmpty()) return null
        var minIndex = 0; var minValue = selector(this[0])
        for (i in 1 until size) { val v = selector(this[i]); if (v < minValue) { minValue = v; minIndex = i } }
        return minIndex
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); fusedLocationClient.removeLocationUpdates(locationCallback); sensorManager.unregisterListener(this); serviceScope.cancel(); super.onDestroy() }
}
