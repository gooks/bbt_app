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

import com.czt.bbt.ui.BusAlertState

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

    private val lastArrivalAlertStops = mutableMapOf<Long, Int>()
    private val lastAnnouncedRouteId = mutableMapOf<Long, String>()

    private var boardingStationName: String? = null
    private var isBoardingDetected = false
    private var isAlightingDetected = false
    private var potentialBoardingTime: Long = 0L 
    private var currentBusPlate: String? = null
    private var boardingTime: Long = 0
    private var lastLocation: Location? = null
    private var destStationIndex: Int = -1
    private var lastStationIndex: Int = -1
    private var lastAlertStops: Int = -1

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
            ACTION_START_RIDE -> startRideMode(intent.getLongExtra(EXTRA_ALERT_ID, -1))
            ACTION_START_ARRIVAL -> startArrivalMode(intent.getLongExtra(EXTRA_ALERT_ID, -1))
            ACTION_REFRESH -> {
                serviceScope.launch {
                    if (mode == Mode.RIDE) { if (isBoardingDetected) checkRideStatus() else updateNotification("승차 확인 중...") }
                    activeArrivalAlerts.keys.forEach { checkArrivalStatus(it) }
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
            ACTION_STOP_ALERT -> stopIndividualAlert(intent.getLongExtra(EXTRA_ALERT_ID, -1))
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
            if (last != null && last.alightTime == null) repository.deleteRideHistory(last)
        }
        getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE).edit().putLong("active_ride_id", -1L).commit()
        notifyWidgetUpdate(); if (activeArrivalJobs.isEmpty()) stopSelf()
    }

    private fun stopArrivalAll() {
        activeArrivalJobs.keys.toList().forEach { id -> activeArrivalJobs[id]?.cancel(); notificationManager.cancel(NOTIFICATION_ID + id.toInt()) }
        activeArrivalJobs.clear(); activeArrivalAlerts.clear(); lastArrivalAlertStops.clear()
        BusAlertState.liveStatusFlow.value = emptyMap(); BusAlertState.liveStatusDetailFlow.value = emptyMap(); BusAlertState.activeIdsFlow.value = emptyList()
        getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE).edit().putString("active_arrival_ids", "").commit()
        notifyWidgetUpdate(); if (mode != Mode.RIDE) stopSelf()
    }

    private fun handleManualStop() {
        serviceScope.launch {
            val histories = repository.getAllRideHistories().first(); val last = histories.maxByOrNull { it.boardingTime }
            if (last != null && last.alightTime == null) repository.deleteRideHistory(last)
            getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE).edit().putLong("active_ride_id", -1L).putString("active_arrival_ids", "").commit()
            lastAlertStops = -1; lastArrivalAlertStops.clear()
            BusAlertState.liveStatusFlow.value = emptyMap(); BusAlertState.liveStatusDetailFlow.value = emptyMap(); BusAlertState.activeIdsFlow.value = emptyList()
            notifyWidgetUpdate(); stopSelf()
        }
    }

    private fun stopIndividualAlert(alertId: Long) {
        activeArrivalJobs[alertId]?.cancel(); activeArrivalJobs.remove(alertId); activeArrivalAlerts.remove(alertId); lastArrivalAlertStops.remove(alertId)
        notificationManager.cancel(NOTIFICATION_ID + alertId.toInt())
        BusAlertState.liveStatusFlow.value = BusAlertState.liveStatusFlow.value.toMutableMap().apply { remove(alertId) }
        BusAlertState.liveStatusDetailFlow.value = BusAlertState.liveStatusDetailFlow.value.toMutableMap().apply { remove(alertId) }
        BusAlertState.activeIdsFlow.value = activeArrivalAlerts.keys.toList()
        val prefs = getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE)
        val newIds = (prefs.getString("active_arrival_ids", "") ?: "").split(",").filter { it != alertId.toString() && it.isNotEmpty() }.joinToString(",")
        prefs.edit().putString("active_arrival_ids", newIds).commit()
        notifyWidgetUpdate(); if (activeArrivalJobs.isEmpty() && mode != Mode.RIDE) stopSelf()
    }

    private fun startRideMode(alertId: Long) {
        if (mode == Mode.RIDE) sensorManager.unregisterListener(this)
        mode = Mode.RIDE; isBoardingDetected = false; potentialBoardingTime = 0L; lastAlertStops = -1; lastStationIndex = -1
        getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE).edit().putLong("active_ride_id", alertId).commit()
        startForeground(NOTIFICATION_ID, createNotification("버스 이동 알림 준비 중..."))
        startLocationUpdates(); notifyWidgetUpdate()
        serviceScope.launch {
            val histories = repository.getAllRideHistories().first(); val last = histories.maxByOrNull { it.boardingTime }
            if (last != null && last.alightTime == null) repository.deleteRideHistory(last)
            try {
                val alert = repository.getAllRideAlerts().first().find { it.id == alertId } ?: run { stopSelf(); return@launch }
                activeRideAlert = alert; val stations = repository.getBusRouteStations(alert.busRouteId); routeStationsCache[alert.busRouteId] = stations
                destStationIndex = stations.indexOfFirst { it.stationId == alert.destinationStationId }
                var retries = 0; while (lastLocation == null && retries < 5) { delay(1000); retries++ }
                lastLocation?.let { loc ->
                    val nearest = stations.minByOrNull { val r = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, it.y, it.x, r); r[0] }
                    boardingStationName = nearest?.stationName; updateNotification("승차 대기 중: ${boardingStationName ?: "위치 확인 중"}")
                    sensorManager.registerListener(this@BusAlertService, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
                } ?: stopSelf()
            } catch (e: Exception) { stopSelf() }
        }
    }

    private fun checkBoardingStatusWithTimeout() {
        if (isBoardingDetected || potentialBoardingTime == 0L) return
        val now = System.currentTimeMillis(); if (now - potentialBoardingTime > 3 * 60 * 1000) { revertToWaitingStatus(); return }
        val loc = lastLocation ?: return; val routeId = activeRideAlert?.busRouteId ?: return; val stations = routeStationsCache[routeId] ?: return; val station = stations.find { it.stationName == boardingStationName } ?: return
        val dist = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, station.y, station.x, dist)
        if (dist[0] > 70) confirmBoarding()
    }

    private fun revertToWaitingStatus() { potentialBoardingTime = 0L; updateNotification("승차 대기 중: ${boardingStationName ?: "위치 확인 중"}"); sensorManager.unregisterListener(this); sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL) }

    private fun startArrivalMode(alertId: Long) {
        serviceScope.launch {
            val alerts = repository.getAllArrivalAlerts().first(); val alert = alerts.find { it.id == alertId } ?: return@launch
            activeArrivalAlerts[alertId] = alert
            val prefs = getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE); val currentIds = prefs.getString("active_arrival_ids", "") ?: ""
            val idList = currentIds.split(",").filter { it.isNotEmpty() }.toMutableList()
            if (!idList.contains(alertId.toString())) { idList.add(alertId.toString()); prefs.edit().putString("active_arrival_ids", idList.joinToString(",")).commit() }
            updateArrivalNotification(alertId, "버스도착알림 : ${alert.stationName}", "도착 정보 확인 중...", "도착 정보 확인 중...")
            notifyWidgetUpdate(); lastArrivalAlertStops[alertId] = -1
            val job = launch { while (isActive) { val nextDelay = checkArrivalStatus(alertId); delay(nextDelay) } }
            activeArrivalJobs[alertId] = job
        }
    }

    private suspend fun checkArrivalStatus(alertId: Long): Long {
        val alert = activeArrivalAlerts[alertId] ?: return 300000L
        try {
            val results = mutableListOf<Triple<String, String, Int>>(); val displayMessages = mutableMapOf<String, String>(); val detailMessages = mutableMapOf<String, String>()
            val res = repository.getBusArrivalListV2(alert.stationId); val arrivalList = res.response.msgBody?.busArrivalList ?: emptyList()
            alert.targetBusNumbers.forEachIndexed { index, rId ->
                try {
                    val busName = alert.targetBusNames.getOrNull(index) ?: "버스"; val item = arrivalList.find { it.routeId.toIntSafe().toString() == rId }; val p1 = item?.predictTimeSec1.toIntSafe()
                    if (item != null && p1 > 0) {
                        val stateStr = when(item.stateCd1.toString()) { "0" -> "지나 교차로 통과"; "1" -> "정류소 도착"; "2" -> "정류소 출발"; else -> "지나 운행 중" }
                        val mins = p1 / 60; val secs = p1 % 60; val timeStr = if (mins > 0) "${mins}분 ${secs}초" else "${secs}초"
                        displayMessages[rId] = "[$busName/${item.plateNo1}] $timeStr 후 도착."
                        detailMessages[rId] = "- 버스(차량)번호 : $busName (${item.plateNo1})\n- 도착예정시간 : $timeStr 후 도착\n- 현재위치 : ${item.stationNm1} $stateStr"
                        results.add(Triple(busName, rId, p1))
                    } else {
                        val stations = repository.getBusRouteStations(rId); val myStation = stations.find { it.stationId == alert.stationId }
                        val seq = myStation?.stationSeq ?: item?.staOrder.toIntSafe()
                        if (seq > 0) {
                            val locRes = repository.getBusLocations(rId); val busLocs = parseLocationList(locRes.response.msgBody?.busLocationList).filterNotNull()
                            val lastBus = busLocs.maxByOrNull { it.stationSeq }; val totalStations = stations.size
                            val timeToFinish = if (lastBus != null) (totalStations - lastBus.stationSeq).coerceAtLeast(0) * 120.0 else 0.0
                            val garageToMe = (seq - 1) * 120.0; val totalEstSec = (timeToFinish + garageToMe).toInt()
                            val mins = totalEstSec / 60; val timeStr = if (mins > 0) "${mins}분" else "잠시 후"
                            displayMessages[rId] = "[$busName] $timeStr 후 도착 (차고지 출발 전)"
                            detailMessages[rId] = "- 버스(차량)번호 : $busName (정보없음)\n- 도착예정시간 : $timeStr 후 도착\n- 현재위치 : 차고지 출발 전"
                            results.add(Triple(busName, rId, totalEstSec / 60 * 60 + 59))
                        }
                    }
                } catch (e: Exception) { }
            }
            val title = "버스도착알림 : ${alert.stationName}"
            if (results.isEmpty()) { updateArrivalNotification(alertId, title, "운행 정보 없음", "운행 정보 없음"); return 60000L }
            val sorted = results.sortedBy { it.third }; val minTimeSec = sorted.first().third; val busName = sorted.first().first
            if (minTimeSec <= 180) { val threshold = if (minTimeSec <= 65) 1 else 3; if (lastArrivalAlertStops[alertId] != threshold) { triggerAlertEffects(); speak(if (threshold == 1) "$busName 버스가 잠시 후 도착합니다." else "$busName 버스가 약 3분 후에 도착합니다."); lastArrivalAlertStops[alertId] = threshold } }
            val contentPopup = sorted.joinToString("\n") { displayMessages[it.second] ?: "" }
            val contentDetail = "<[${alert.stationNo}] ${alert.stationName} 버스도착정보>\n\n" + sorted.joinToString("\n\n") { detailMessages[it.second] ?: "" }
            updateArrivalNotification(alertId, title, contentPopup, contentDetail)
            return when { minTimeSec > 300 -> 60000L; minTimeSec > 180 -> 30000L; else -> 15000L }
        } catch (e: Exception) { return 60000L }
    }

    private fun updateArrivalNotification(alertId: Long, title: String, contentPopup: String, contentDetail: String) {
        val currentStatus = BusAlertState.liveStatusFlow.value.toMutableMap(); currentStatus[alertId] = contentPopup; BusAlertState.liveStatusFlow.value = currentStatus
        val currentDetail = BusAlertState.liveStatusDetailFlow.value.toMutableMap(); currentDetail[alertId] = contentDetail; BusAlertState.liveStatusDetailFlow.value = currentDetail
        BusAlertState.activeIdsFlow.value = activeArrivalAlerts.keys.toList()
        val refreshIntent = PendingIntent.getService(this, alertId.toInt(), Intent(this, BusAlertService::class.java).setAction(ACTION_REFRESH), PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = PendingIntent.getService(this, alertId.toInt() + 100, Intent(this, BusAlertService::class.java).setAction(ACTION_STOP_ALERT).putExtra(EXTRA_ALERT_ID, alertId), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle(title).setContentText(contentPopup.split("\n")[0]).setStyle(NotificationCompat.BigTextStyle().bigText(contentPopup)).setSmallIcon(R.mipmap.ic_launcher_foreground).setContentIntent(refreshIntent).setOngoing(true).setOnlyAlertOnce(true).addAction(android.R.drawable.ic_menu_close_clear_cancel, "알림중지", stopIntent).build()
        if (activeArrivalJobs.size == 1 && mode != Mode.RIDE) startForeground(NOTIFICATION_ID + alertId.toInt(), notification) else notificationManager.notify(NOTIFICATION_ID + alertId.toInt(), notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (mode == Mode.RIDE && !isBoardingDetected && event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]; val y = event.values[1]; val z = event.values[2]; val acc = sqrt(x*x + y*y + z*z)
            if (acc > 13 && potentialBoardingTime == 0L) { potentialBoardingTime = System.currentTimeMillis(); updateNotification("승차 확인 중...") }
        }
    }

    private fun confirmBoarding() {
        if (isBoardingDetected) return
        isBoardingDetected = true; sensorManager.unregisterListener(this); boardingTime = System.currentTimeMillis()
        serviceScope.launch {
            try {
                val alert = activeRideAlert ?: return@launch; val routeId = alert.busRouteId; val stations = routeStationsCache[routeId] ?: return@launch
                val res = repository.getBusLocations(routeId); val locs = parseLocationList(res.response.msgBody?.busLocationList).filterNotNull()
                val boardingSeq = stations.indexOfFirst { it.stationName == boardingStationName }.takeIf { it != -1 } ?: 0
                lastStationIndex = boardingSeq
                val myBus = locs.filter { it.stationSeq <= boardingSeq + 1 }.maxByOrNull { it.stationSeq } ?: locs.minByOrNull { kotlin.math.abs(it.stationSeq - boardingSeq) }
                
                var estMin = (destStationIndex - boardingSeq).coerceAtLeast(1) * 2
                if (myBus != null) {
                    currentBusPlate = myBus.plateNo
                    try {
                        val arrRes = repository.getBusArrivalListV2(alert.destinationStationId)
                        val match = arrRes.response.msgBody?.busArrivalList?.find { it.routeId.toIntSafe().toString() == routeId && it.plateNo1 == currentBusPlate }
                        if (match != null) estMin = match.predictTimeSec1.toIntSafe() / 60
                    } catch (e: Exception) { }
                    updateNotification("승차 확인! 버스: ${alert.busNumber} ($currentBusPlate)")
                } else {
                    currentBusPlate = "전세 (GPS 추적)"
                    updateNotification("승차 확인! [전세 버스]")
                }
                
                repository.insertRideHistory(RideHistory(date = SimpleDateFormat("yyyy-MM-dd (E)", Locale.KOREAN).format(Date(boardingTime)), boardingTime = boardingTime, boardingStationName = boardingStationName ?: "알 수 없음", busNumber = alert.busNumber, plateNumber = currentBusPlate, alightStationName = alert.destinationStationName))
                shareStatus("승차", alert.busNumber, currentBusPlate ?: "확인 불가", boardingTime, boardingStationName ?: "", extraInfo = "도착예상: ${SimpleDateFormat("HH:mm").format(Date(boardingTime + estMin * 60000L))} (${estMin}분 소요 예상)")
            } catch (e: Exception) { }
        }
    }

    private fun shareStatus(type: String, busNo: String, plateNo: String, time: Long, station: String, summary: String = "", extraInfo: String = "") {
        val alert = activeRideAlert ?: return; val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))
        serviceScope.launch {
            val allHistories = repository.getAllRideHistories().first()
            val filteredLogs = allHistories.filter { it.busNumber == busNo }.sortedByDescending { it.boardingTime }
            val prev = filteredLogs.filter { it.boardingTime != (if (type == "승차") time else boardingTime) }.take(3)
            val hist = if (prev.isNotEmpty()) "\n\n[이 노선 과거 이용 기록]\n" + prev.joinToString("\n") { "• ${it.date} ${SimpleDateFormat("HH:mm").format(Date(it.boardingTime))} ${it.plateNumber} (${it.boardingStationName} → ${it.alightStationName})" } else ""
            val main = if (summary.isNotEmpty()) summary else "버스: ${busNo}번 ($plateNo)\n일자: ${SimpleDateFormat("yyyy-MM-dd (E)", Locale.KOREAN).format(Date(time))}\n탑승시간: $timeStr\n${if (extraInfo.isNotEmpty()) "$extraInfo\n" else ""}승차정류장: $station\n목적정류장: ${alert.destinationStationName}"
            alert.shareEmails.forEach { com.czt.bbt.util.NotificationHelper.sendEmail(this@BusAlertService, busNo, plateNo, timeStr, station, type, main + hist) }
            if (alert.shareKakao) com.czt.bbt.util.NotificationHelper.sendKakaoMessage(this@BusAlertService, busNo, plateNo, timeStr, station, type, main + hist)
        }
    }

    private fun checkRideStatus() {
        if (!isBoardingDetected) return
        val alert = activeRideAlert ?: return; val loc = lastLocation ?: return; val stations = routeStationsCache[alert.busRouteId] ?: return
        val nearestIdx = stations.indexOfMinByOrNull { val res = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, it.y, it.x, res); res[0] } ?: -1
        if (nearestIdx != -1 && nearestIdx > lastStationIndex) lastStationIndex = nearestIdx
        if (lastStationIndex != -1 && destStationIndex != -1) {
            val stopsRemaining = destStationIndex - lastStationIndex; val distToDest = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, stations[destStationIndex].y, stations[destStationIndex].x, distToDest)
            if (distToDest[0] < 150) { handleAlight(); return }
            serviceScope.launch {
                try {
                    var tStr = "약 ${stopsRemaining * 2}분"
                    if (currentBusPlate != null && !currentBusPlate!!.contains("GPS")) {
                        val arrRes = repository.getBusArrivalListV2(alert.destinationStationId)
                        val match = arrRes.response.msgBody?.busArrivalList?.find { it.routeId.toIntSafe().toString() == alert.busRouteId && it.plateNo1 == currentBusPlate }
                        if (match != null && match.predictTimeSec1.toIntSafe() > 0) { val s = match.predictTimeSec1.toIntSafe(); tStr = "약 ${s/60}분 ${s%60}초" }
                    }
                    val next = if (lastStationIndex + 1 < stations.size) stations[lastStationIndex + 1].stationName else "종점"
                    updateNotification("다음: $next | $tStr (${stopsRemaining}전) | ${String.format("%.1f", distToDest[0]/1000.0)}km")
                    if (stopsRemaining <= 2 && stopsRemaining != lastAlertStops) { triggerAlertEffects(); if (stopsRemaining == 2) speak("도착 2정류장 전입니다. 하차 준비하세요.") else speak("다음이 목적지입니다. 이번에 하차하세요."); lastAlertStops = stopsRemaining }
                } catch (e: Exception) { }
            }
        }
    }

    private fun handleAlight() {
        if (isAlightingDetected) return; isAlightingDetected = true; triggerAlertEffects(); speak("목적지에 도착했습니다."); updateNotification("하차 완료: 종료합니다.")
        serviceScope.launch {
            val histories = repository.getAllRideHistories().first(); val last = histories.maxByOrNull { it.boardingTime }
            if (last != null && last.alightTime == null) repository.updateRideHistory(last.copy(alightTime = System.currentTimeMillis(), alightStationName = activeRideAlert?.destinationStationName))
            mode = Mode.IDLE; activeRideAlert = null; getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE).edit().putLong("active_ride_id", -1L).commit(); notifyWidgetUpdate(); delay(5000); stopSelf()
        }
    }

    private fun triggerAlertEffects() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1)) else vibrator.vibrate(1000) }
    @SuppressLint("MissingPermission") private fun startLocationUpdates() { fusedLocationClient.requestLocationUpdates(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000).build(), locationCallback, Looper.getMainLooper()) }
    private fun createNotificationChannel() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "버스 알림", NotificationManager.IMPORTANCE_LOW)) }
    private fun createNotification(content: String): Notification {
        val busNumber = activeRideAlert?.busNumber ?: ""
        val isGpsTracking = currentBusPlate?.contains("GPS") == true
        val title = if (isGpsTracking) "버스이동알림 : $busNumber (GPS 추적)" else "버스이동알림 : $busNumber"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
    private fun updateNotification(content: String) { notificationManager.notify(NOTIFICATION_ID, createNotification(content)) }
    private fun parseLocationList(data: Any?): List<com.czt.bbt.api.GBusLocationItem> { val json = gson.toJson(data ?: return emptyList()); return try { if (json.startsWith("[")) gson.fromJson(json, object : TypeToken<List<com.czt.bbt.api.GBusLocationItem>>() {}.type) else listOf(gson.fromJson(json, com.czt.bbt.api.GBusLocationItem::class.java)) } catch (e: Exception) { emptyList() } }
    private fun <T : Any> List<T>.indexOfMinByOrNull(selector: (T) -> Float): Int? { if (isEmpty()) return null; var minIndex = 0; var minValue = selector(this[0]); for (i in 1 until size) { val v = selector(this[i]); if (v < minValue) { minValue = v; minIndex = i } }; return minIndex }
    private fun Any?.toIntSafe(): Int { val s = this?.toString() ?: ""; return try { if (s.contains(".")) s.toDouble().toInt() else s.toInt() } catch (e: Exception) { 0 } }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); fusedLocationClient.removeLocationUpdates(locationCallback); sensorManager.unregisterListener(this); serviceScope.cancel(); super.onDestroy() }
}
