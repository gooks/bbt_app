package com.czt.bbt.data

import com.czt.bbt.api.BusApiService
import com.czt.bbt.model.*
import com.czt.bbt.ui.BusAlertState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusRepository @Inject constructor(
    private val apiService: BusApiService,
    private val busDao: BusDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    private val apiPrefs = context.getSharedPreferences("api_usage_prefs", android.content.Context.MODE_PRIVATE)
    private val mainPrefs = context.getSharedPreferences("bus_alert_prefs", android.content.Context.MODE_PRIVATE)
    private val serviceKey = "cdc5e165cd54d32593270eab28c0671f54386348ccad640e03daf36a681f5241"
    private val gson = Gson()
    private val firestore = FirebaseFirestore.getInstance()

    // 동기화 관련
    private fun getUserId(): String = mainPrefs.getString("google_user_id", "") ?: ""

    suspend fun syncWithCloud() {
        val uid = getUserId()
        if (uid.isEmpty()) return

        try {
            // 1. 사용자 프로필(앱 비밀번호 등) 가져오기
            val userDoc = firestore.collection("users").document(uid).get().await()
            if (userDoc.exists()) {
                val cloudAppPass = userDoc.getString("google_app_password") ?: ""
                if (cloudAppPass.isNotEmpty()) {
                    mainPrefs.edit().putString("google_app_password", cloudAppPass).apply()
                }
            }

            // 2. 알람 데이터 가져오기
            val rideSnapshot = firestore.collection("users").document(uid).collection("ride_alerts").get().await()
            val arrivalSnapshot = firestore.collection("users").document(uid).collection("arrival_alerts").get().await()

            val cloudRideAlerts = rideSnapshot.toObjects(RideAlert::class.java)
            val cloudArrivalAlerts = arrivalSnapshot.toObjects(ArrivalAlert::class.java)

            cloudRideAlerts.forEach { busDao.insertRideAlert(it) }
            cloudArrivalAlerts.forEach { busDao.insertArrivalAlert(it) }
            
            logSystem("SYNC", "클라우드 동기화 완료 (이동:${cloudRideAlerts.size}, 도착:${cloudArrivalAlerts.size})")
        } catch (e: Exception) {
            logSystem("SYNC_FAIL", "동기화 실패: ${e.message}")
        }
    }

    suspend fun saveGoogleAppPasswordToCloud(password: String) {
        val uid = getUserId(); if (uid.isEmpty()) return
        firestore.collection("users").document(uid).set(mapOf("google_app_password" to password), com.google.firebase.firestore.SetOptions.merge())
    }

    private suspend fun uploadRideAlert(alert: RideAlert) {
        val uid = getUserId(); if (uid.isEmpty()) return
        firestore.collection("users").document(uid).collection("ride_alerts").document(alert.id.toString()).set(alert)
    }

    private suspend fun uploadArrivalAlert(alert: ArrivalAlert) {
        val uid = getUserId(); if (uid.isEmpty()) return
        firestore.collection("users").document(uid).collection("arrival_alerts").document(alert.id.toString()).set(alert)
    }

    private suspend fun deleteRideAlertFromCloud(id: Long) {
        val uid = getUserId(); if (uid.isEmpty()) return
        firestore.collection("users").document(uid).collection("ride_alerts").document(id.toString()).delete()
    }

    private suspend fun deleteArrivalAlertFromCloud(id: Long) {
        val uid = getUserId(); if (uid.isEmpty()) return
        firestore.collection("users").document(uid).collection("arrival_alerts").document(id.toString()).delete()
    }

    private fun updateApiCount(tag: String) {
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastDate = apiPrefs.getString("last_date", "")
        if (today != lastDate) { apiPrefs.edit().clear().putString("last_date", today).apply() }
        
        val currentCount = apiPrefs.getInt(tag, 1000)
        val newCount = (currentCount - 1).coerceAtLeast(0)
        apiPrefs.edit().putInt(tag, newCount).apply()
        
        // UI 실시간 동기화: BusAlertState의 Flow 업데이트
        val currentMap = BusAlertState.apiUsageFlow.value.toMutableMap()
        currentMap[tag] = newCount
        BusAlertState.apiUsageFlow.value = currentMap
    }

    suspend fun searchBusRoute(keyword: String): com.czt.bbt.api.GBusRouteResponse {
        return apiCall("노선검색") { apiService.getBusRouteList(serviceKey, keyword) }
    }

    suspend fun getBusLocations(routeId: String): com.czt.bbt.api.GBusLocationResponse {
        return apiCall("위치조회") { apiService.getBusLocationList(serviceKey, routeId) }
    }

    suspend fun getBusStationAroundList(x: Double, y: Double): com.czt.bbt.api.GBusStationAroundResponse {
        return apiCall("주변정류소") { apiService.getBusStationAroundList(serviceKey, x.toString(), y.toString()) }
    }

    suspend fun getBusRouteStations(routeId: String): List<CachedRouteStation> {
        val cached = busDao.getCachedStations(routeId)
        if (cached.isNotEmpty()) return cached

        val res = apiCall("경유정류소") { apiService.getBusRouteStationList(serviceKey, routeId) }
        val items = parseRouteStations(res.response.msgBody?.busRouteStationList)
        
        val toCache = items.map { item ->
            CachedRouteStation(
                routeId = routeId,
                stationId = item.stationId.toString(),
                stationName = item.stationName?.toString() ?: "",
                stationSeq = item.stationSeq,
                x = item.x,
                y = item.y,
                mobileNo = item.mobileNo?.toString()
            )
        }
        busDao.insertCachedStations(toCache)
        return toCache
    }

    private fun parseRouteStations(data: Any?): List<com.czt.bbt.api.GBusRouteStationItem> {
        val json = gson.toJson(data ?: return emptyList())
        return try {
            if (json.startsWith("[")) {
                gson.fromJson(json, object : TypeToken<List<com.czt.bbt.api.GBusRouteStationItem>>() {}.type)
            } else {
                listOf(gson.fromJson(json, com.czt.bbt.api.GBusRouteStationItem::class.java))
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun searchStation(keyword: String): com.czt.bbt.api.GBusStationListResponse {
        return apiCall("정류소검색") { apiService.getBusStationList(serviceKey, keyword) }
    }

    suspend fun getBusStationViaRouteList(stationId: String): com.czt.bbt.api.GBusStationViaRouteResponse {
        return apiCall("경유노선조회") { apiService.getBusStationViaRouteList(serviceKey, stationId) }
    }

    suspend fun getBusArrivalListV2(stationId: String): com.czt.bbt.api.GBusArrivalListResponseV2 {
        return apiCall("도착정보목록") { apiService.getBusArrivalListV2(serviceKey, stationId) }
    }

    suspend fun getBusArrivalItemV2(stationId: String, routeId: String, staOrder: String): com.czt.bbt.api.GBusArrivalItemResponseV2 {
        return apiCall("도착정보항목") { apiService.getBusArrivalItemV2(serviceKey, stationId, routeId, staOrder) }
    }

    // Alerts & History
    suspend fun insertRideAlert(alert: RideAlert) {
        val id = busDao.insertRideAlert(alert)
        uploadRideAlert(alert.copy(id = id))
    }
    fun getAllRideAlerts(): Flow<List<RideAlert>> = busDao.getAllRideAlerts()
    suspend fun updateRideAlert(alert: RideAlert) {
        busDao.updateRideAlert(alert)
        uploadRideAlert(alert)
    }
    suspend fun deleteRideAlert(alert: RideAlert) {
        busDao.deleteRideAlert(alert)
        deleteRideAlertFromCloud(alert.id)
    }
    suspend fun insertArrivalAlert(alert: ArrivalAlert) {
        val id = busDao.insertArrivalAlert(alert)
        uploadArrivalAlert(alert.copy(id = id))
    }
    fun getAllArrivalAlerts(): Flow<List<ArrivalAlert>> = busDao.getAllArrivalAlerts()
    suspend fun updateArrivalAlert(alert: ArrivalAlert) {
        busDao.updateArrivalAlert(alert)
        uploadArrivalAlert(alert)
    }
    suspend fun deleteArrivalAlert(alert: ArrivalAlert) {
        busDao.deleteArrivalAlert(alert)
        deleteArrivalAlertFromCloud(alert.id)
    }
    suspend fun insertRideHistory(history: RideHistory) = busDao.insertRideHistory(history)
    fun getAllRideHistories(): Flow<List<RideHistory>> = busDao.getAllRideHistories()
    suspend fun updateRideHistory(history: RideHistory) = busDao.updateRideHistory(history)
    suspend fun deleteRideHistory(history: RideHistory) = busDao.deleteRideHistory(history)

    // Logs
    suspend fun logSystem(tag: String, message: String) = busDao.insertSystemLog(SystemLog(tag = tag, message = message))
    fun getSystemLogs(): Flow<List<SystemLog>> = busDao.getRecentSystemLogs()
    suspend fun clearLogs() = busDao.clearSystemLogs()

    private suspend fun <T> apiCall(tag: String, call: suspend () -> retrofit2.Response<T>): T {
        updateApiCount(tag) // 여기서 카운트를 실시간으로 처리
        try {
            val response = call()
            if (response.isSuccessful && response.body() != null) { return response.body()!! } 
            else { throw Exception("API Error: ${response.code()}") }
        } catch (e: Exception) {
            logSystem("API_FAIL", "$tag 예외: ${e.message}")
            throw e
        }
    }
}
