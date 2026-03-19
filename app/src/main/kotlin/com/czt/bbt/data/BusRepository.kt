package com.czt.bbt.data

import com.czt.bbt.api.BusApiService
import com.czt.bbt.model.*
import com.czt.bbt.ui.BusAlertState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusRepository @Inject constructor(
    private val apiService: BusApiService,
    private val busDao: BusDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    private val apiPrefs = context.getSharedPreferences("api_usage_prefs", android.content.Context.MODE_PRIVATE)
    private val serviceKey = "cdc5e165cd54d32593270eab28c0671f54386348ccad640e03daf36a681f5241"
    private val gson = Gson()

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
    suspend fun insertRideAlert(alert: RideAlert) = busDao.insertRideAlert(alert)
    fun getAllRideAlerts(): Flow<List<RideAlert>> = busDao.getAllRideAlerts()
    suspend fun updateRideAlert(alert: RideAlert) = busDao.updateRideAlert(alert)
    suspend fun deleteRideAlert(alert: RideAlert) = busDao.deleteRideAlert(alert)
    suspend fun insertArrivalAlert(alert: ArrivalAlert) = busDao.insertArrivalAlert(alert)
    fun getAllArrivalAlerts(): Flow<List<ArrivalAlert>> = busDao.getAllArrivalAlerts()
    suspend fun updateArrivalAlert(alert: ArrivalAlert) = busDao.updateArrivalAlert(alert)
    suspend fun deleteArrivalAlert(alert: ArrivalAlert) = busDao.deleteArrivalAlert(alert)
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
