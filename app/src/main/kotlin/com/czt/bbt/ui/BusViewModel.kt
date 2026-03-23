package com.czt.bbt.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.czt.bbt.data.BusRepository
import com.czt.bbt.model.*
import com.czt.bbt.service.BusAlertService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class BusViewModel @Inject constructor(
    private val repository: BusRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE)
    
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "active_ride_id" || key == "active_arrival_ids") {
            loadStateFromPrefs()
        }
    }

    // 데이터 흐름 (Flows)
    val rideAlerts = repository.getAllRideAlerts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val arrivalAlerts = repository.getAllArrivalAlerts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val history = repository.getAllRideHistories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val systemLogs = repository.getSystemLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // 이동 알림 UI 상태
    var rideBusSearchQuery = mutableStateOf("")
    var rideBusSearchResult = mutableStateListOf<BusInfo>() 
    var rideSelectedBus = mutableStateOf<BusInfo?>(null)
    var rideRouteStations = mutableStateListOf<StationInfo>()
    var rideSelectedDestination = mutableStateOf<StationInfo?>(null)
    var editingRideAlert = mutableStateOf<RideAlert?>(null)
    var rideShareEmails = mutableStateListOf<String>()
    var rideShareKakao = mutableStateOf(false)
    var rideShareType = mutableStateOf("REALTIME")
    var rideShareKakaoTarget = mutableStateOf("")
    var rideShareMemo = mutableStateOf("")
    var rideTempEmail = mutableStateOf("")

    // 도착 알림 UI 상태
    var arrivalStationSearchQuery = mutableStateOf("")
    var arrivalStationSearchResult = mutableStateListOf<StationInfo>() 
    var arrivalSelectedStation = mutableStateOf<StationInfo?>(null)
    var arrivalBusList = mutableStateListOf<BusInfo>() 
    var arrivalSelectedBuses = mutableStateListOf<String>() 
    var arrivalSelectedBusNames = mutableStateListOf<String>() 
    var editingArrivalAlert = mutableStateOf<ArrivalAlert?>(null)

    var errorMessage = mutableStateOf<String?>(null)
    var isLoading = mutableStateOf(false)

    // 구글 계정 및 메일 설정
    var googleEmail = mutableStateOf(prefs.getString("google_email", "") ?: "")
    var googleAppPassword = mutableStateOf(prefs.getString("google_app_password", "") ?: "")
    var googleUserId = mutableStateOf(prefs.getString("google_user_id", "") ?: "")
    var isGoogleLoggedIn = mutableStateOf(googleUserId.value.isNotEmpty())
    
    // 카카오 계정 설정
    var isKakaoLoggedIn = mutableStateOf(false)
    var kakaoNickname = mutableStateOf("")

    fun checkKakaoLoginStatus() {
        com.kakao.sdk.user.UserApiClient.instance.me { user, error ->
            if (user != null) {
                isKakaoLoggedIn.value = true
                kakaoNickname.value = user.kakaoAccount?.profile?.nickname ?: "사용자"
            } else {
                isKakaoLoggedIn.value = false
                kakaoNickname.value = ""
            }
        }
    }

    fun logoutKakao() {
        com.kakao.sdk.user.UserApiClient.instance.logout {
            isKakaoLoggedIn.value = false
            kakaoNickname.value = ""
        }
    }

    fun onGoogleSignInSuccess(email: String, userId: String) {
        googleEmail.value = email
        googleUserId.value = userId
        isGoogleLoggedIn.value = true
        prefs.edit().putString("google_email", email).putString("google_user_id", userId).apply()
        
        // 로그인 성공 시 동기화 시작
        syncDataWithCloud()
    }

    private fun syncDataWithCloud() {
        val uid = googleUserId.value
        if (uid.isEmpty()) return
        
        viewModelScope.launch {
            repository.syncWithCloud()
        }
    }

    fun saveGoogleAccount(email: String, appPassword: String) {
        googleEmail.value = email
        googleAppPassword.value = appPassword
        isGoogleLoggedIn.value = email.isNotEmpty()
        prefs.edit().putString("google_email", email).putString("google_app_password", appPassword).apply()
        
        // 클라우드에 앱 비밀번호 저장
        viewModelScope.launch {
            repository.saveGoogleAppPasswordToCloud(appPassword)
        }
    }

    fun logoutGoogle() {
        googleEmail.value = ""
        googleAppPassword.value = ""
        isGoogleLoggedIn.value = false
        prefs.edit().remove("google_email").remove("google_app_password").apply()
    }

    // API 현황 상태
    var apiUsage = mutableStateMapOf<String, Int>()

    // 실행 상태 추적
    var activeRideAlertId = mutableStateOf<Long?>(null)
    var activeArrivalIds = mutableStateListOf<Long>()

    // 실시간 도착 현황 상태 (BusAlertState 구독)
    private val _arrivalLiveStatus = MutableStateFlow<Map<Long, String>>(emptyMap())
    val arrivalLiveStatus = _arrivalLiveStatus.asStateFlow()

    private val _arrivalLiveStatusDetail = MutableStateFlow<Map<Long, String>>(emptyMap())
    val arrivalLiveStatusDetail = _arrivalLiveStatusDetail.asStateFlow()

    private val _arrivalLiveStatusIds = MutableStateFlow<List<Long>>(emptyList())
    val arrivalLiveStatusIds = _arrivalLiveStatusIds.asStateFlow()

    init {
        loadStateFromPrefs()
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        loadApiUsage()
        
        viewModelScope.launch {
            BusAlertState.liveStatusFlow.collect { _arrivalLiveStatus.value = it }
        }
        viewModelScope.launch {
            BusAlertState.liveStatusDetailFlow.collect { _arrivalLiveStatusDetail.value = it }
        }
        viewModelScope.launch {
            BusAlertState.activeIdsFlow.collect { updateArrivalLiveStatusIds(it) }
        }
        viewModelScope.launch {
            BusAlertState.apiUsageFlow.collect { map ->
                map.forEach { (tag, count) -> apiUsage[tag] = count }
            }
        }
    }

    fun updateArrivalLiveStatusIds(ids: List<Long>) {
        val currentOrder = _arrivalLiveStatusIds.value
        val newIds = ids.toSet()
        val keptOrder = currentOrder.filter { it in newIds }
        val addedIds = ids.filter { it !in currentOrder.toSet() }
        _arrivalLiveStatusIds.value = keptOrder + addedIds
    }

    fun moveArrivalItem(fromIndex: Int, toIndex: Int) {
        val list = _arrivalLiveStatusIds.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _arrivalLiveStatusIds.value = list
        }
    }

    fun loadApiUsage() {
        val usagePrefs = context.getSharedPreferences("api_usage_prefs", Context.MODE_PRIVATE)
        val tags = listOf("노선검색", "위치조회", "주변정류소", "경유정류소", "정류소검색", "경유노선조회", "도착정보목록", "도착정보항목")
        val initialMap = mutableMapOf<String, Int>()
        tags.forEach { tag -> initialMap[tag] = usagePrefs.getInt(tag, 1000) }
        BusAlertState.apiUsageFlow.value = initialMap
    }

    private fun loadStateFromPrefs() {
        val rideId = prefs.getLong("active_ride_id", -1L)
        activeRideAlertId.value = if (rideId != -1L) rideId else null
        val arrivalIdsStr = prefs.getString("active_arrival_ids", "") ?: ""
        activeArrivalIds.clear()
        if (arrivalIdsStr.isNotEmpty()) {
            activeArrivalIds.addAll(arrivalIdsStr.split(",").mapNotNull { it.toLongOrNull() })
        }
    }

    private fun saveState() {
        prefs.edit().apply {
            putLong("active_ride_id", activeRideAlertId.value ?: -1L)
            putString("active_arrival_ids", activeArrivalIds.joinToString(","))
            commit()
        }
        val widgetIntent = Intent("com.czt.bbt.ACTION_WIDGET_REFRESH").apply { setPackage(context.packageName) }
        context.sendBroadcast(widgetIntent)
    }

    override fun onCleared() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onCleared()
    }

    // 데이터 클래스
    data class BusInfo(val name: String, val routeId: String, val type: String, val region: String)
    data class StationInfo(val name: String, val id: String, val no: String, val seq: Int = 0)
    
    data class GBusRouteItem(val routeId: Long, val routeName: Any?, val routeTypeName: String?, val regionName: String?)
    data class GBusRouteStationItem(val stationId: Long, val stationName: Any?, val stationSeq: Int, val mobileNo: Any?)
    data class GBusStationItem(val stationId: Long, val stationName: Any?, val mobileNo: Any?, val regionName: String?)
    data class GBusStationViaRouteItem(val routeId: Long, val routeName: Any?, val routeTypeName: String?, val regionName: String?)
    data class GBusStationAroundItem(val stationId: Long, val stationName: Any?, val mobileNo: Any?, val regionName: String?, val distance: Int)

    private fun formatRouteName(name: Any?): String {
        val str = name?.toString() ?: return "번호없음"
        return if (str.endsWith(".0")) str.substring(0, str.length - 2) else str
    }

    private inline fun <reified T> parseList(data: Any?): List<T> {
        if (data == null) return emptyList()
        val gson = Gson(); val json = gson.toJson(data)
        return try { 
            if (json.startsWith("[")) { gson.fromJson(json, object : TypeToken<List<T>>() {}.type) } 
            else { listOf(gson.fromJson(json, T::class.java)) }
        } catch (e: Exception) { emptyList() }
    }

    fun searchBusForRide() {
        errorMessage.value = null; rideBusSearchResult.clear(); isLoading.value = true
        viewModelScope.launch {
            try {
                val res = repository.searchBusRoute(rideBusSearchQuery.value)
                val list = parseList<GBusRouteItem>(res.response.msgBody?.busRouteList)
                if (list.isNotEmpty()) {
                    rideBusSearchResult.addAll(list.map { BusInfo(formatRouteName(it.routeName), it.routeId.toString(), it.routeTypeName ?: "일반", it.regionName ?: "") })
                } else { errorMessage.value = "검색 결과가 없습니다." }
            } catch (e: Exception) { errorMessage.value = "버스 검색 오류" }
            finally { isLoading.value = false; loadApiUsage() }
        }
    }

    fun selectBusForRide(bus: BusInfo) {
        rideSelectedBus.value = bus
        loadRouteStations(bus.routeId)
    }

    private fun loadRouteStations(routeId: String) {
        viewModelScope.launch {
            try {
                val stationRes = repository.getBusRouteStations(routeId)
                rideRouteStations.clear()
                rideRouteStations.addAll(stationRes.map { StationInfo(it.stationName, it.stationId, it.mobileNo ?: "", it.stationSeq) })
            } catch (e: Exception) { errorMessage.value = "정류소 목록 조회 오류" }
            finally { loadApiUsage() }
        }
    }

    fun setEditRideAlert(alert: RideAlert) {
        editingRideAlert.value = alert
        rideBusSearchQuery.value = alert.busNumber
        rideSelectedBus.value = BusInfo(alert.busNumber, alert.busRouteId, "", "")
        rideSelectedDestination.value = StationInfo(alert.destinationStationName, alert.destinationStationId, "", alert.destinationStationSeq)
        rideShareEmails.clear(); rideShareEmails.addAll(alert.shareEmails)
        rideShareKakao.value = alert.shareKakao; rideShareType.value = alert.shareType
        rideShareKakaoTarget.value = alert.shareKakaoTarget; rideShareMemo.value = alert.shareMemo
        loadRouteStations(alert.busRouteId)
    }

    fun addShareEmail() {
        val email = rideTempEmail.value.trim()
        if (email.isNotEmpty() && email.contains("@") && !rideShareEmails.contains(email)) { rideShareEmails.add(email); rideTempEmail.value = "" }
    }

    fun removeShareEmail(email: String) = rideShareEmails.remove(email)

    fun loginWithKakao(context: Context) {
        val callback: (com.kakao.sdk.auth.model.OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                errorMessage.value = "카카오 로그인 실패: ${error.message}"
            } else if (token != null) {
                viewModelScope.launch { 
                    repository.logSystem("KAKAO_LOGIN", "카카오 로그인 성공")
                    checkKakaoLoginStatus()
                    android.widget.Toast.makeText(context, "카카오톡 연동이 완료되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (com.kakao.sdk.user.UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            com.kakao.sdk.user.UserApiClient.instance.loginWithKakaoTalk(context, callback = callback)
        } else {
            com.kakao.sdk.user.UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
        }
    }

    fun saveRideAlert() {
        val bus = rideSelectedBus.value ?: return; val dest = rideSelectedDestination.value ?: return
        viewModelScope.launch {
            val alert = RideAlert(id = editingRideAlert.value?.id ?: 0, busNumber = bus.name, busRouteId = bus.routeId, destinationStationName = dest.name, destinationStationId = dest.id, destinationStationSeq = dest.seq, shareEmails = rideShareEmails.toList(), shareKakao = rideShareKakao.value, shareType = rideShareType.value, shareKakaoTarget = rideShareKakaoTarget.value, shareMemo = rideShareMemo.value)
            if (editingRideAlert.value == null) repository.insertRideAlert(alert) else repository.updateRideAlert(alert)
            resetRideForm()
        }
    }

    fun resetRideForm() {
        rideSelectedBus.value = null; rideBusSearchResult.clear(); rideRouteStations.clear(); rideBusSearchQuery.value = ""
        rideSelectedDestination.value = null; editingRideAlert.value = null; rideShareEmails.clear()
        rideShareKakao.value = false; rideShareType.value = "REALTIME"; rideShareKakaoTarget.value = ""; rideShareMemo.value = ""; rideTempEmail.value = ""
    }

    fun searchStationForArrival() {
        errorMessage.value = null; arrivalStationSearchResult.clear(); isLoading.value = true
        viewModelScope.launch {
            try {
                val res = repository.searchStation(arrivalStationSearchQuery.value)
                val list = parseList<GBusStationItem>(res.response.msgBody?.busStationList)
                if (list.isNotEmpty()) {
                    arrivalStationSearchResult.addAll(list.map { StationInfo(it.stationName?.toString() ?: "이름없음", it.stationId.toString(), it.mobileNo?.toString() ?: "") })
                } else { errorMessage.value = "검색 결과가 없습니다." }
            } catch (e: Exception) { errorMessage.value = "정류소 검색 오류" }
            finally { isLoading.value = false; loadApiUsage() }
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    suspend fun searchNearbyStationsForArrival() {
        errorMessage.value = null; arrivalStationSearchResult.clear(); isLoading.value = true
        try {
            val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            if (location != null) {
                val res = repository.getBusStationAroundList(location.longitude, location.latitude)
                val list = parseList<GBusStationAroundItem>(res.response.msgBody?.busStationAroundList)
                arrivalStationSearchResult.addAll(list.map { StationInfo(it.stationName?.toString() ?: "이름없음", it.stationId.toString(), it.mobileNo?.toString() ?: "") })
            } else { errorMessage.value = "위치 정보를 가져올 수 없습니다." }
        } catch (e: Exception) { errorMessage.value = "주변 검색 오류" }
        finally { isLoading.value = false; loadApiUsage() }
    }

    fun selectStationForArrival(station: StationInfo) { arrivalSelectedStation.value = station; loadBusesAtStation(station.id) }

    private fun loadBusesAtStation(stationId: String) {
        viewModelScope.launch {
            try {
                val routeRes = repository.getBusStationViaRouteList(stationId)
                val routes = parseList<GBusStationViaRouteItem>(routeRes.response.msgBody?.busRouteList)
                arrivalBusList.clear()
                arrivalBusList.addAll(routes.map { BusInfo(formatRouteName(it.routeName), it.routeId.toString(), it.routeTypeName ?: "일반", it.regionName ?: "") })
            } catch (e: Exception) { errorMessage.value = "경유 노선 조회 오류" }
            finally { loadApiUsage() }
        }
    }

    fun setEditArrivalAlert(alert: ArrivalAlert) {
        editingArrivalAlert.value = alert; arrivalStationSearchQuery.value = alert.stationName
        arrivalSelectedStation.value = StationInfo(alert.stationName, alert.stationId, alert.stationNo)
        arrivalSelectedBuses.clear(); arrivalSelectedBuses.addAll(alert.targetBusNumbers)
        arrivalSelectedBusNames.clear(); arrivalSelectedBusNames.addAll(alert.targetBusNames)
        loadBusesAtStation(alert.stationId)
    }

    fun saveArrivalAlert() {
        val station = arrivalSelectedStation.value ?: return
        if (arrivalSelectedBuses.isEmpty()) return
        viewModelScope.launch {
            val alert = ArrivalAlert(id = editingArrivalAlert.value?.id ?: 0, stationName = station.name, stationId = station.id, stationNo = station.no, targetBusNumbers = arrivalSelectedBuses.toList(), targetBusNames = arrivalSelectedBusNames.toList())
            if (editingArrivalAlert.value == null) repository.insertArrivalAlert(alert) else repository.updateArrivalAlert(alert)
            resetArrivalForm()
        }
    }

    fun resetArrivalForm() {
        arrivalSelectedStation.value = null; arrivalStationSearchResult.clear(); arrivalBusList.clear(); arrivalStationSearchQuery.value = ""
        arrivalSelectedBuses.clear(); arrivalSelectedBusNames.clear(); editingArrivalAlert.value = null
    }

    fun toggleArrivalBusSelection(bus: BusInfo) {
        if (arrivalSelectedBuses.contains(bus.routeId)) { arrivalSelectedBuses.remove(bus.routeId); arrivalSelectedBusNames.remove(bus.name) } 
        else { arrivalSelectedBuses.add(bus.routeId); arrivalSelectedBusNames.add(bus.name) }
    }

    fun startRideAlert(alert: RideAlert) { activeRideAlertId.value = alert.id; saveState(); sendServiceAction(BusAlertService.ACTION_START_RIDE, alert.id) }
    fun startArrivalAlert(alert: ArrivalAlert) { if (!activeArrivalIds.contains(alert.id)) { activeArrivalIds.add(alert.id) }; saveState(); sendServiceAction(BusAlertService.ACTION_START_ARRIVAL, alert.id) }
    fun stopRideAlert() { activeRideAlertId.value = null; saveState(); sendServiceAction(BusAlertService.ACTION_STOP_RIDE) }
    fun stopArrivalAlert(alertId: Long) { activeArrivalIds.remove(alertId); saveState(); sendServiceAction("ACTION_STOP_ALERT", alertId) }
    fun stopArrivalAll() { activeArrivalIds.clear(); saveState(); sendServiceAction(BusAlertService.ACTION_STOP_ARRIVAL_ALL) }
    fun stopAllServices() { activeRideAlertId.value = null; activeArrivalIds.clear(); saveState(); sendServiceAction(BusAlertService.ACTION_STOP) }

    private fun sendServiceAction(action: String, alertId: Long = -1) {
        val intent = Intent(context, BusAlertService::class.java).apply { this.action = action; if (alertId != -1L) putExtra(BusAlertService.EXTRA_ALERT_ID, alertId) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
    }

    fun deleteRideAlert(alert: RideAlert) = viewModelScope.launch { repository.deleteRideAlert(alert) }
    fun deleteArrivalAlert(alert: ArrivalAlert) = viewModelScope.launch { repository.deleteArrivalAlert(alert) }
    fun deleteHistory(history: RideHistory) = viewModelScope.launch { repository.deleteRideHistory(history) }
}
