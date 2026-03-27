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
import kotlin.math.abs
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
    private var rideJob: Job? = null
    private val activeArrivalJobs = mutableMapOf<Long, Job>()
    private val activeArrivalAlerts = mutableMapOf<Long, ArrivalAlert>()
    private val routeStationsCache = mutableMapOf<String, List<CachedRouteStation>>()

    private val lastArrivalAlertStops = mutableMapOf<Long, Int>()
    private val lastArrivalAlertPlate = mutableMapOf<Long, String>()
    private val lastAnnouncedRouteId = mutableMapOf<Long, String>()
    private val garageDepartureAnnounced = mutableMapOf<Long, MutableSet<String>>() // alertId -> set of routeIds
    private var lastApproachingPlate: String? = null // 최근 도착 안내된 차량 번호
    private var lastApproachingRouteId: String? = null // 최근 도착 안내된 노선 ID

    private var boardingStationName: String? = null
    private var boardingStationNo: String? = null
    private var isBoardingDetected = false
    private var isAlightingDetected = false
    private var potentialBoardingTime: Long = 0L 
    private var potentialPlateNo: String? = null // 승차 확인 중 캡처된 차량번호
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
                    val targetId = intent.getLongExtra(EXTRA_ALERT_ID, -1)
                    if (mode == Mode.RIDE) { if (isBoardingDetected) checkRideStatus() else updateNotification("승차 확인 중...") }
                    if (targetId != -1L) {
                        checkArrivalStatus(targetId)
                    } else {
                        activeArrivalAlerts.keys.forEach { checkArrivalStatus(it) }
                    }
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
                    boardingStationName = nearest?.stationName
                    boardingStationNo = nearest?.mobileNo
                    updateNotification("승차 대기 중: ${boardingStationName ?: "위치 확인 중"}")
                    sensorManager.registerListener(this@BusAlertService, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
                    
                    // 승차 전까지 주기적으로 가장 가까운 버스 갱신 루틴
                    launch {
                        while (mode == Mode.RIDE && !isBoardingDetected) {
                            try {
                                val locRes = repository.getBusLocations(alert.busRouteId)
                                val busLocs = parseLocationList(locRes.response.msgBody?.busLocationList).filterNotNull()
                                val boardingSeq = stations.indexOfFirst { it.stationName == boardingStationName }.takeIf { it != -1 } ?: 0
                                
                                // 내 정류장(boardingSeq)을 기준으로 가장 가까운 버스 찾기 (지나간 버스도 한 정거장 정도는 후보 유지)
                                val candidates = busLocs.filter { it.stationSeq >= boardingSeq - 1 && it.stationSeq <= boardingSeq + 1 }
                                val nearestBus = candidates.minByOrNull { abs(it.stationSeq - boardingSeq) }
                                    ?: busLocs.filter { it.stationSeq < boardingSeq }.maxByOrNull { it.stationSeq }

                                if (nearestBus != null) {
                                    lastApproachingPlate = nearestBus.plateNo
                                    Log.d(TAG, "Approaching bus updated: $lastApproachingPlate (seq: ${nearestBus.stationSeq} / mine: $boardingSeq)")
                                }
                            } catch (e: Exception) { }
                            delay(20000) // 20초마다 갱신
                        }
                    }
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
        activeArrivalJobs[alertId]?.cancel()
        serviceScope.launch {
            val alerts = repository.getAllArrivalAlerts().first(); val alert = alerts.find { it.id == alertId } ?: return@launch
            activeArrivalAlerts[alertId] = alert
            val prefs = getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE); val currentIds = prefs.getString("active_arrival_ids", "") ?: ""
            val idList = currentIds.split(",").filter { it.isNotEmpty() }.toMutableList()
            if (!idList.contains(alertId.toString())) { idList.add(alertId.toString()); prefs.edit().putString("active_arrival_ids", idList.joinToString(",")).commit() }
            updateArrivalNotification(alertId, "버스도착알림 : ${alert.stationName}", "도착 정보 확인 중...", "도착 정보 확인 중...")
            notifyWidgetUpdate(); lastArrivalAlertStops[alertId] = -1; lastArrivalAlertPlate[alertId] = ""
            val job = launch { while (isActive) { val nextDelay = checkArrivalStatus(alertId); delay(nextDelay) } }
            activeArrivalJobs[alertId] = job
        }
    }

    private suspend fun checkArrivalStatus(alertId: Long): Long {
        val alert = activeArrivalAlerts[alertId] ?: return 300000L
        val title = "버스도착알림 : ${alert.stationName}"
        try {
            val results = mutableListOf<Triple<String, String, Int>>(); val displayMessages = mutableMapOf<String, String>(); val detailMessages = mutableMapOf<String, String>()
            val res = repository.getBusArrivalListV2(alert.stationId)
            val arrivalList = parseArrivalList(res.response.msgBody?.busArrivalList)
            
            alert.targetBusNumbers.forEachIndexed { index, rId ->
                try {
                    val busName = alert.targetBusNames.getOrNull(index) ?: "버스"; val item = arrivalList.find { it.routeId.toIntSafe().toString() == rId }; val p1 = item?.predictTimeSec1.toIntSafe()
                    if (item != null && p1 > 0) {
                        val stateStr = when(item.stateCd1.toString()) { "0" -> "지나 교차로 통과"; "1" -> "정류소 도착"; "2" -> "정류소 출발"; else -> "지나 운행 중" }
                        val mins = p1 / 60; val secs = p1 % 60; val timeStr = if (mins > 0) "${mins}분 ${secs}초" else "${secs}초"
                        val stopsLeftStr = if (item.locationNo1.toIntSafe() > 0) " (${item.locationNo1.toIntSafe()}정거장 전)" else ""
                        displayMessages[rId] = "[$busName] $timeStr 후 도착$stopsLeftStr"
                        detailMessages[rId] = "- 버스(차량)번호 : $busName (${item.plateNo1})\n- 도착예정시간 : $timeStr 후 도착\n- 현재위치 : ${item.locationNo1.toIntSafe()}정거장 전 ${item.stationNm1} $stateStr"
                        results.add(Triple(busName, rId, p1))

                        // 차고지 출발 알림 (최초 1회)
                        val announcedSet = garageDepartureAnnounced.getOrPut(alertId) { mutableSetOf() }
                        if (!announcedSet.contains(rId)) {
                            announcedSet.add(rId)
                            val ttsMsg = "차고지에서 ${busName}번 버스가 출발했습니다. ${mins}분 ${secs}초 후 도착 예정입니다."
                            speak(ttsMsg)
                            Log.d(TAG, "Garage departure announced: $ttsMsg")
                        }
                    } else {
                        val stations = routeStationsCache[rId] ?: repository.getBusRouteStations(rId).also { routeStationsCache[rId] = it }
                        val myStation = stations.find { it.stationId == alert.stationId } 
                            ?: stations.find { it.mobileNo == alert.stationNo }
                            ?: stations.find { it.stationName == alert.stationName }
                        
                        val seq = myStation?.stationSeq ?: item?.staOrder.toIntSafe()
                        
                        if (seq > 0) {
                            val locRes = repository.getBusLocations(rId)
                            val busLocs = parseLocationList(locRes.response.msgBody?.busLocationList).filterNotNull()
                            
                            // 1. 현재 정류장(seq)보다 이전에 있는 가장 가까운 버스 찾기
                            val approachingBus = busLocs.filter { it.stationSeq < seq }.maxByOrNull { it.stationSeq }
                            
                            if (approachingBus != null) {
                                val stopsLeft = seq - approachingBus.stationSeq
                                val totalEstSec = stopsLeft * 120 // 정류장당 2분 가정
                                val mins = totalEstSec / 60
                                val timeStr = if (mins > 0) "${mins}분" else "잠시 후"
                                displayMessages[rId] = "[$busName] $timeStr 후 도착 (계산됨, ${stopsLeft}정거장 전)"
                                detailMessages[rId] = "- 버스(차량)번호 : $busName (${approachingBus.plateNo})\n- 도착예정시간 : $timeStr 후 도착\n- 현재위치 : ${stopsLeft}정거장 전 (계산된 위치)"
                                results.add(Triple(busName, rId, totalEstSec))
                            } else {
                                // 2. 이전에 버스가 없으면 (모두 지나갔거나 차고지 대기 중), 차고지 기반 계산
                                val lastBus = busLocs.maxByOrNull { it.stationSeq }
                                val totalStations = stations.size
                                val timeToFinish = if (lastBus != null) (totalStations - lastBus.stationSeq).coerceAtLeast(0) * 120.0 else 0.0
                                val garageToMe = (seq - 1) * 120.0
                                val totalEstSec = (timeToFinish + garageToMe).toInt()
                                
                                val mins = totalEstSec / 60
                                val timeStr = if (mins > 0) "${mins}분" else "잠시 후"
                                displayMessages[rId] = "[$busName] $timeStr 후 도착 (차고지 출발 전)"
                                detailMessages[rId] = "- 버스(차량)번호 : $busName (정보없음)\n- 도착예정시간 : $timeStr 후 도착\n- 현재위치 : 차고지 출발 전"
                                results.add(Triple(busName, rId, totalEstSec))
                            }
                        } else {
                            displayMessages[rId] = "[$busName] 운행 정보 확인 불가"
                            detailMessages[rId] = "- 버스(차량)번호 : $busName\n- 상태 : 정류소 정보 매칭 실패"
                            results.add(Triple(busName, rId, 999999))
                        }
                    }
                } catch (e: Exception) { }
            }
            if (results.isEmpty()) { updateArrivalNotification(alertId, title, "운행 정보 없음", "운행 정보 없음"); return 60000L }
            val sorted = results.sortedBy { it.third };
            val firstSortedItem = sorted.first()
            val busName = firstSortedItem.first
            val busRouteId = firstSortedItem.second
            val minTimeSec = firstSortedItem.third

            val detailMsg = detailMessages[busRouteId]
            val plateNo = detailMsg?.substringAfter("버스(차량)번호 : $busName (")?.substringBefore(")") ?: ""

            // 버스가 바뀌었거나 거리가 멀어지면 알림 상태 초기화
            if (lastArrivalAlertPlate[alertId] != plateNo || minTimeSec > 300) {
                lastArrivalAlertStops[alertId] = -1
                lastArrivalAlertPlate[alertId] = plateNo
            }

            val plateTts = if (plateNo.isNotEmpty() && !plateNo.contains("정보없음")) ", 차량번호 ${Regex("""\d{4}$""").find(plateNo)?.value}가" else "가"
            if (minTimeSec <= 180) { 
                val threshold = if (minTimeSec <= 65) 1 else 3
                if (lastArrivalAlertStops[alertId] != threshold) { 
                    triggerAlertEffects()
                    val msg = if (threshold == 1) "[도착 알림] $busName 번 버스$plateTts 잠시 후 도착합니다." else "[도착 알림] $busName 번 버스$plateTts 약 3분 후에 도착합니다."
                    speak(msg)
                    lastArrivalAlertStops[alertId] = threshold 
                } 
            }
            val contentPopup = sorted.joinToString("\n") { displayMessages[it.second] ?: "" }
            val contentDetail = "<[${alert.stationNo}] ${alert.stationName} 버스도착정보>\n\n" + sorted.joinToString("\n\n") { detailMessages[it.second] ?: "" }
            updateArrivalNotification(alertId, title, contentPopup, contentDetail)
            return when { minTimeSec > 300 -> 60000L; minTimeSec > 180 -> 30000L; else -> 15000L }
        } catch (e: Exception) { 
            updateArrivalNotification(alertId, title, "정보 조회 중 오류 발생", "서버 응답을 처리하는 중 오류가 발생했습니다.")
            return 60000L 
        }
    }

    private fun parseArrivalList(data: Any?): List<com.czt.bbt.api.GBusArrivalInfoItem> {
        val json = gson.toJson(data ?: return emptyList())
        return try {
            if (json.startsWith("[")) {
                gson.fromJson(json, object : TypeToken<List<com.czt.bbt.api.GBusArrivalInfoItem>>() {}.type)
            } else {
                listOf(gson.fromJson(json, com.czt.bbt.api.GBusArrivalInfoItem::class.java))
            }
        } catch (e: Exception) { emptyList() }
    }



    private fun updateArrivalNotification(alertId: Long, title: String, contentPopup: String, contentDetail: String) {
        val currentStatus = BusAlertState.liveStatusFlow.value.toMutableMap(); currentStatus[alertId] = contentPopup; BusAlertState.liveStatusFlow.value = currentStatus
        val currentDetail = BusAlertState.liveStatusDetailFlow.value.toMutableMap(); currentDetail[alertId] = contentDetail; BusAlertState.liveStatusDetailFlow.value = currentDetail
        BusAlertState.activeIdsFlow.value = activeArrivalAlerts.keys.toList()

        // 알림 클릭 시 앱을 열고 해당 알림으로 포커스 이동을 위한 인텐트
        val contentIntent = PendingIntent.getActivity(
            this, alertId.toInt(),
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_TARGET_ALERT_ID", alertId)
                putExtra("ACTION", "FOCUS_ARRIVAL")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(this, alertId.toInt() + 100, Intent(this, BusAlertService::class.java).setAction(ACTION_STOP_ALERT).putExtra(EXTRA_ALERT_ID, alertId), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle(title).setContentText(contentPopup.split("\n")[0]).setStyle(NotificationCompat.BigTextStyle().bigText(contentPopup)).setSmallIcon(R.mipmap.ic_launcher_foreground).setContentIntent(contentIntent).setOngoing(true).setOnlyAlertOnce(true).addAction(android.R.drawable.ic_menu_close_clear_cancel, "알림중지", stopIntent).build()
        if (activeArrivalJobs.size == 1 && mode != Mode.RIDE) startForeground(NOTIFICATION_ID + alertId.toInt(), notification) else notificationManager.notify(NOTIFICATION_ID + alertId.toInt(), notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (mode == Mode.RIDE && !isBoardingDetected && event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]; val y = event.values[1]; val z = event.values[2]; val acc = sqrt(x*x + y*y + z*z)
            if (acc > 13 && potentialBoardingTime == 0L) { 
                potentialBoardingTime = System.currentTimeMillis()
                potentialPlateNo = lastApproachingPlate // 승차 진동 감지 시점의 다가오는 차량번호 캡처
                updateNotification("승차 확인 중...") 
            }
        }
    }

    private suspend fun calculateChainedEstimate(destStationId: String, routeId: String, targetStationId: String): Int {
        Log.d(TAG, "[ChainEst] START destStationId=$destStationId, routeId=$routeId, targetStationId=$targetStationId")
        val locRes = repository.getBusLocations(routeId)
        val busLocs = parseLocationList(locRes.response.msgBody?.busLocationList).filterNotNull()
        Log.d(TAG, "[ChainEst] busLocs count=${busLocs.size}")
        if (busLocs.isEmpty()) { Log.d(TAG, "[ChainEst] FAIL: busLocs empty → return -1"); return -1 }

        busLocs.forEach { Log.d(TAG, "[ChainEst] busLoc: vehId=${it.vehId}, plateNo=${it.plateNo}, stationId=${it.stationId}, stationSeq=${it.stationSeq}") }

        var totalSec = 0
        var currentStationId = destStationId
        val visitedVehIds = mutableSetOf<Long>()
        val maxIterations = 30

        for (i in 0 until maxIterations) {
            Log.d(TAG, "[ChainEst] iter=$i, currentStationId=$currentStationId")
            val arrRes = repository.getBusArrivalItemV2(currentStationId, routeId, "1")
            val item = arrRes.response.msgBody?.busArrivalItem
            if (item == null) { Log.d(TAG, "[ChainEst] iter=$i BREAK: busArrivalItem is null"); break }

            val vehIdLong = item.vehId1.toIntSafe().toLong()
            val predSec = item.predictTimeSec1.toIntSafe()
            Log.d(TAG, "[ChainEst] iter=$i arrivalItem: vehId1=${item.vehId1}, vehIdLong=$vehIdLong, predictTimeSec1=${item.predictTimeSec1}, plateNo1=${item.plateNo1}, stationNm1=${item.stationNm1}, locationNo1=${item.locationNo1}")

            if (vehIdLong == 0L) { Log.d(TAG, "[ChainEst] iter=$i BREAK: vehIdLong is 0"); break }
            if (predSec <= 0) { Log.d(TAG, "[ChainEst] iter=$i BREAK: predictTimeSec1=$predSec <= 0"); break }
            if (!visitedVehIds.add(vehIdLong)) { Log.d(TAG, "[ChainEst] iter=$i BREAK: vehIdLong=$vehIdLong already visited (visited=$visitedVehIds)"); break }

            totalSec += predSec
            Log.d(TAG, "[ChainEst] iter=$i totalSec=$totalSec (added $predSec)")

            val busLoc = busLocs.find { it.vehId == vehIdLong }
            if (busLoc == null) { Log.d(TAG, "[ChainEst] iter=$i BREAK: vehIdLong=$vehIdLong not found in busLocs"); break }
            val foundStationId = busLoc.stationId.toString()
            Log.d(TAG, "[ChainEst] iter=$i matched busLoc: vehIdLong=$vehIdLong → stationId=$foundStationId, stationSeq=${busLoc.stationSeq}, plateNo=${busLoc.plateNo}")

            if (foundStationId == targetStationId) { Log.d(TAG, "[ChainEst] iter=$i DONE: reached targetStationId=$targetStationId"); break }
            currentStationId = foundStationId
        }

        val result = if (totalSec > 0) totalSec / 60 else -1
        Log.d(TAG, "[ChainEst] END totalSec=$totalSec, result=$result min")
        return result
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
                
                val currentBoardingStation = stations.getOrNull(boardingSeq)
                boardingStationNo = currentBoardingStation?.mobileNo?.trim()
                val bStationId = currentBoardingStation?.stationId ?: ""

                val myBus = locs.find { it.plateNo == potentialPlateNo && potentialPlateNo != null }
                    ?: locs.find { it.plateNo == lastApproachingPlate && lastApproachingPlate != null }
                    ?: locs.filter { it.stationSeq >= boardingSeq - 1 && it.stationSeq <= boardingSeq + 1 }.minByOrNull { Math.abs(it.stationSeq - boardingSeq) }
                    ?: locs.minByOrNull { Math.abs(it.stationSeq - boardingSeq) }
                
                var estMin = 0
                val vId = myBus?.vehId?.toString()
                if (myBus != null) {
                    currentBusPlate = myBus.plateNo
                    val stopsRemaining = (destStationIndex - boardingSeq).coerceAtLeast(1)
                    val distToDestRes = FloatArray(1); Location.distanceBetween(lastLocation?.latitude ?: 0.0, lastLocation?.longitude ?: 0.0, stations[destStationIndex].y, stations[destStationIndex].x, distToDestRes)
                    val distKm = distToDestRes[0] / 1000.0
                    estMin = ((distKm * 1.5) + (stopsRemaining * 0.5)).toInt().coerceAtLeast(1)
                    updateNotification("승차 확인! 버스: ${alert.busNumber} ($currentBusPlate)")
                } else {
                    currentBusPlate = "확인 불가 (GPS 추적)"
                    estMin = (destStationIndex - boardingSeq).coerceAtLeast(1) * 2
                    updateNotification("승차 확인! [GPS 추적]")
                }

                val dateNow = Date(boardingTime)
                repository.insertRideHistory(RideHistory(
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN).format(dateNow), 
                    dayOfWeek = SimpleDateFormat("E", Locale.KOREAN).format(dateNow),
                    boardingTime = boardingTime, 
                    boardingStationName = boardingStationName ?: "", 
                    boardingStationNo = boardingStationNo,
                    boardingStationId = bStationId,
                    busNumber = alert.busNumber, 
                    busRouteId = alert.busRouteId,
                    plateNumber = currentBusPlate, 
                    vehicleId = vId,
                    alightStationName = alert.destinationStationName,
                    alightStationNo = stations.find { it.stationId == alert.destinationStationId }?.mobileNo?.trim(),
                    alightStationId = alert.destinationStationId,
                    estimatedMinutes = estMin
                ))
                shareStatus("승차", alert.busNumber, currentBusPlate ?: "확인 불가", boardingTime, boardingStationName ?: "", extraInfo = "도착예상: ${SimpleDateFormat("HH:mm").format(Date(boardingTime + estMin * 60000L))} (${estMin}분 소요 예상)")
            } catch (e: Exception) { }
        }
    }

    private fun shareStatus(type: String, busNo: String, plateNo: String, time: Long, station: String, summary: String = "", extraInfo: String = "") {
        val alert = activeRideAlert ?: return
        val routeId = alert.busRouteId
        val stations = routeStationsCache[routeId] ?: emptyList()

        val formatCombined = { name: String ->
            val no = if (name == boardingStationName && boardingStationNo != null) boardingStationNo?.trim()
                    else stations.find { it.stationName == name }?.mobileNo?.trim()
            if (!no.isNullOrEmpty()) "[$no]$name" else name
        }

        serviceScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd (E)", Locale.KOREAN).format(Date(time))
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))

            val main = if (type == "승차") {
                val bStation = boardingStationName ?: ""
                val dStation = alert.destinationStationName
                "버스: ${busNo}번 ($plateNo)\n" +
                "일자: $dateStr\n" +
                "승차시간: $timeStr\n" +
                "$extraInfo\n" +
                "승차정류장: ${formatCombined(bStation)}\n" +
                "목적정류장: ${formatCombined(dStation)}"
            } else {
                val bStation = boardingStationName ?: ""
                val dStation = station // alightStation
                val bTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(boardingTime))
                val diffMin = (time - boardingTime) / 60000
                val durationStr = if (diffMin >= 60) "${diffMin / 60}시간 ${diffMin % 60}분 소요" else "${diffMin}분 소요"

                "버스: ${busNo}번 ($plateNo)\n" +
                "일자: $dateStr\n" +
                "탑승시간: $bTimeStr ~ $timeStr ($durationStr)\n" +
                "이동구간: ${formatCombined(bStation)} ~ ${formatCombined(dStation)}"
            }

            alert.shareEmails.forEach { com.czt.bbt.util.NotificationHelper.sendEmail(this@BusAlertService, busNo, plateNo, timeStr, station, type, main) }
            if (alert.shareKakao) com.czt.bbt.util.NotificationHelper.sendKakaoMessage(this@BusAlertService, busNo, plateNo, timeStr, station, type, main)
        }
    }

    private fun checkRideStatus() {
        if (!isBoardingDetected) return
        val alert = activeRideAlert ?: return; val loc = lastLocation ?: return; val stations = routeStationsCache[alert.busRouteId] ?: return
        val nearestIdx = stations.indexOfMinByOrNull { val res = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, it.y, it.x, res); res[0] } ?: -1
        if (nearestIdx != -1 && nearestIdx > lastStationIndex) lastStationIndex = nearestIdx
        if (lastStationIndex != -1 && destStationIndex != -1) {
            val stopsRemaining = (destStationIndex - lastStationIndex).coerceAtLeast(0)
            val distToDestRes = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, stations[destStationIndex].y, stations[destStationIndex].x, distToDestRes)
            val distToDestKm = distToDestRes[0] / 1000.0
            if (distToDestRes[0] < 150) { handleAlight(); return }
            serviceScope.launch {
                try {
                    // 1. 하이브리드 예상 시간 계산 (평균 35~40km/h 가정)
                    var estimatedTotalMin = ((distToDestKm * 1.6) + (stopsRemaining * 0.4)).toInt().coerceAtLeast(1)
                    
                    // 2. 실시간 목적지 도착 정보 확인 (내가 탄 차량번호 기준)
                    try {
                        val res = repository.getBusArrivalListV2(alert.destinationStationId)
                        val arrivalList = parseArrivalList(res.response.msgBody?.busArrivalList)
                        val routeArrival = arrivalList.find { it.routeId.toIntSafe().toString() == alert.busRouteId }
                        if (routeArrival != null) {
                            if (routeArrival.plateNo1 == currentBusPlate && routeArrival.predictTimeSec1.toIntSafe() > 0) {
                                estimatedTotalMin = routeArrival.predictTimeSec1.toIntSafe() / 60
                            } else if (routeArrival.plateNo2 == currentBusPlate && routeArrival.predictTimeSec2.toIntSafe() > 0) {
                                estimatedTotalMin = routeArrival.predictTimeSec2.toIntSafe() / 60
                            }
                        }
                    } catch (e: Exception) { }

                    // 3. 비정상적인 과다 시간 캡핑 (물리적 한계치 적용)
                    val maxCap = (distToDestKm * 3.5).toInt().coerceAtLeast(3)
                    if (estimatedTotalMin > maxCap) estimatedTotalMin = maxCap

                    val histories = repository.getAllRideHistories().first()
                    val currentHistory = histories.maxByOrNull { it.boardingTime }
                    if (currentHistory != null && currentHistory.alightTime == null) {
                        repository.updateRideHistory(currentHistory.copy(
                            estimatedMinutes = estimatedTotalMin,
                            stopsRemaining = stopsRemaining,
                            lastModified = System.currentTimeMillis()
                        ))
                    }

                    val next = if (lastStationIndex + 1 < stations.size) stations[lastStationIndex + 1].stationName else "종점"
                    val arrivalTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(System.currentTimeMillis() + estimatedTotalMin * 60000L))
                    updateNotification("다음: $next | 약 ${estimatedTotalMin}분 (${stopsRemaining}정거장 전) | 도착예정: $arrivalTimeStr")
                    
                    if (stopsRemaining <= 2 && stopsRemaining != lastAlertStops) { 
                        triggerAlertEffects()
                        if (stopsRemaining == 2) speak("[이동 알림] 도착 2정류장 전입니다. 하차 준비하세요.") 
                        else speak("[이동 알림] 다음이 목적지입니다. 이번에 하차하세요.")
                        lastAlertStops = stopsRemaining 
                    }
                } catch (e: Exception) { }
            }
        }
    }

    private fun handleAlight() {
        if (isAlightingDetected) return; isAlightingDetected = true; triggerAlertEffects(); speak("[이동 알림] 목적지에 도착했습니다."); updateNotification("하차 완료: 종료합니다.")
        serviceScope.launch {
            val alert = activeRideAlert
            val routeId = alert?.busRouteId ?: ""
            val stations = routeStationsCache[routeId] ?: emptyList()
            val histories = repository.getAllRideHistories().first(); val last = histories.maxByOrNull { it.boardingTime }
            if (last != null && last.alightTime == null) {
                val alightTime = System.currentTimeMillis()
                val alightStation = alert?.destinationStationName ?: "목적지"
                val alightStationNo = stations.find { it.stationName == alightStation }?.mobileNo?.trim()
                val alightStationId = alert?.destinationStationId
                val durationMin = ((alightTime - last.boardingTime) / 60000).toInt()
                
                repository.updateRideHistory(last.copy(
                    alightTime = alightTime, 
                    alightStationName = alightStation,
                    alightStationNo = alightStationNo,
                    alightStationId = alightStationId,
                    durationMinutes = durationMin
                ))
                
                // 하차 알림 전송 추가
                if (alert != null) {
                    shareStatus("하차", alert.busNumber, currentBusPlate ?: "확인 불가", alightTime, alightStation)
                }
            }
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
        
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("ACTION", "FOCUS_RIDE_HISTORY")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentIntent(contentIntent)
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
