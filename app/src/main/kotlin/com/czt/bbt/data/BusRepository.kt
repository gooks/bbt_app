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
    private fun getUserId(): String {
        val uid = mainPrefs.getString("google_user_id", "") ?: ""
        android.util.Log.d("BusRepository", "getUserId: Retrieved UID=$uid")
        return uid
    }

    suspend fun syncWithCloud(): String? {
        val uid = getUserId()
        if (uid.isEmpty()) {
            logSystem("SYNC", "syncWithCloud: 사용자 ID가 비어 있습니다. 동기화 건너뛰기.")
            return null
        }
        logSystem("SYNC", "syncWithCloud: 클라우드 동기화 시작 (사용자 ID: $uid)")

        return try {
            // 1. 사용자 프로필(앱 비밀번호) 가져오기
            logSystem("SYNC", "syncWithCloud: 사용자 프로필 가져오기 시도...")
            val userDoc = firestore.collection("users").document(uid).get().await()
            val cloudAppPass = if (userDoc.exists()) userDoc.getString("google_app_password") ?: "" else ""
            if (cloudAppPass.isNotEmpty()) {
                mainPrefs.edit().putString("google_app_password", cloudAppPass).apply()
                logSystem("SYNC", "syncWithCloud: 클라우드에서 앱 비밀번호 업데이트됨.")
            } else {
                logSystem("SYNC", "syncWithCloud: 클라우드에 앱 비밀번호 없음 또는 비어 있음.")
            }

            // Perform two-way sync for RideAlerts
            logSystem("SYNC", "syncWithCloud: RideAlerts 양방향 동기화 시작.")
            val localRideAlerts = busDao.getAllRideAlertsOnce()
            syncAlerts(
                collectionName = "ride_alerts",
                localAlerts = localRideAlerts,
                insertDb = { busDao.insertRideAlert(it) },
                updateDb = { busDao.updateRideAlert(it) },
                deleteLocalDb = { busDao.deleteRideAlertById(it) },
                deleteCloudDb = { deleteRideAlertFromCloud(it) }
            )
            logSystem("SYNC", "syncWithCloud: RideAlerts 양방향 동기화 완료.")

            // Perform two-way sync for ArrivalAlerts
            logSystem("SYNC", "syncWithCloud: ArrivalAlerts 양방향 동기화 시작.")
            val localArrivalAlerts = busDao.getAllArrivalAlertsOnce()
            syncAlerts(
                collectionName = "arrival_alerts",
                localAlerts = localArrivalAlerts,
                insertDb = { busDao.insertArrivalAlert(it) },
                updateDb = { busDao.updateArrivalAlert(it) },
                deleteLocalDb = { busDao.deleteArrivalAlertById(it) },
                deleteCloudDb = { deleteArrivalAlertFromCloud(it) }
            )
            logSystem("SYNC", "syncWithCloud: ArrivalAlerts 양방향 동기화 완료.")
            
            logSystem("SYNC", "클라우드 양방향 동기화 전체 완료.")
            cloudAppPass
        } catch (e: Exception) {
            logSystem("SYNC_FAIL", "동기화 실패: ${e.message}")
            null
        }
    }

    private suspend inline fun <reified T> syncAlerts(
        collectionName: String,
        localAlerts: List<T>,
        crossinline insertDb: suspend (T) -> Unit,
        crossinline updateDb: suspend (T) -> Unit,
        crossinline deleteLocalDb: suspend (Long) -> Unit,
        crossinline deleteCloudDb: suspend (Long) -> Unit
    ) where T : CloudSyncable {
        val uid = getUserId(); if (uid.isEmpty()) {
            logSystem("SYNC", "syncAlerts($collectionName): 사용자 ID가 비어 있습니다. 건너뛰기.")
            return
        }
        logSystem("SYNC", "syncAlerts($collectionName): $collectionName 동기화 시작.")

        val collection = firestore.collection("users").document(uid).collection(collectionName)
        val cloudAlerts = collection.get().await().toObjects(T::class.java)
        logSystem("SYNC", "syncAlerts($collectionName): 로컬 ${localAlerts.size}개, 클라우드 ${cloudAlerts.size}개 항목 감지.")

        val localMap = localAlerts.associateBy { it.id }
        val cloudMap = cloudAlerts.associateBy { it.id }

        val mergedIds = mutableSetOf<Long>()

        // Phase 1: Process Cloud alerts (add to local, update local, or update cloud from local)
        cloudMap.values.forEach { cloudAlert ->
            val cloudId = cloudAlert.id
            val localAlert = localMap[cloudId]
            if (localAlert != null) {
                val localModified = localAlert.lastModified
                val cloudModified = cloudAlert.lastModified
                if (cloudModified > localModified) {
                    updateDb(cloudAlert)
                    logSystem("SYNC", "syncAlerts($collectionName): 클라우드가 최신, 로컬 업데이트: ID $cloudId")
                } else if (localModified > cloudModified) {
                    collection.document(cloudId.toString()).set(localAlert)
                    logSystem("SYNC", "syncAlerts($collectionName): 로컬이 최신, 클라우드 업데이트: ID $cloudId")
                } else {
                    logSystem("SYNC", "syncAlerts($collectionName): 로컬/클라우드 동일: ID $cloudId")
                }
                mergedIds.add(cloudId)
            } else {
                insertDb(cloudAlert)
                logSystem("SYNC", "syncAlerts($collectionName): 클라우드에만 존재, 로컬에 추가: ID $cloudId")
                mergedIds.add(cloudId)
            }
        }

        // Phase 2: Process Local-only alerts (upload to cloud)
        localMap.values.forEach { localAlert ->
            val localId = localAlert.id
            if (!cloudMap.containsKey(localId)) {
                collection.document(localId.toString()).set(localAlert)
                logSystem("SYNC", "syncAlerts($collectionName): 로컬에만 존재, 클라우드에 업로드: ID $localId")
                mergedIds.add(localId)
            }
        }

        // Phase 3: Delete local alerts that are no longer in cloud and weren't processed in mergedIds
        localMap.keys.forEach { localId ->
            if (!mergedIds.contains(localId)) {
                deleteLocalDb(localId)
                logSystem("SYNC", "syncAlerts($collectionName): 클라우드에 없음, 로컬에서 삭제: ID $localId")
            }
        }

        // Phase 4: Delete cloud alerts that are no longer in local and weren't processed in mergedIds
        cloudMap.keys.forEach { cloudId ->
            if (!mergedIds.contains(cloudId)) {
                deleteCloudDb(cloudId)
                logSystem("SYNC", "syncAlerts($collectionName): 로컬에 없음, 클라우드에서 삭제: ID $cloudId")
            }
        }
        logSystem("SYNC", "syncAlerts($collectionName): $collectionName 동기화 완료.")
    }

    suspend fun saveGoogleAppPasswordToCloud(password: String) {
        val uid = getUserId(); if (uid.isEmpty()) return
        firestore.collection("users").document(uid).set(mapOf("google_app_password" to password), com.google.firebase.firestore.SetOptions.merge())
    }

    private suspend fun uploadRideAlert(alert: RideAlert) {
        val uid = getUserId(); if (uid.isEmpty()) {
            logSystem("SYNC", "uploadRideAlert: 사용자 ID가 비어 있습니다. 업로드 건너뛰기.")
            return
        }
        logSystem("SYNC", "uploadRideAlert: RideAlert 업로드 시도: ID=${alert.id}, UID=$uid")
        val alertToUpload = alert.copy(
            shareEmails = emptyList(),
            shareKakao = false,
            shareKakaoTarget = "",
            shareMemo = ""
        )
        firestore.collection("users").document(uid).collection("ride_alerts").document(alert.id.toString()).set(alertToUpload)
        logSystem("SYNC", "uploadRideAlert: RideAlert 업로드 완료: ID=${alert.id}")
    }

    private suspend fun uploadArrivalAlert(alert: ArrivalAlert) {
        val uid = getUserId(); if (uid.isEmpty()) {
            logSystem("SYNC", "uploadArrivalAlert: 사용자 ID가 비어 있습니다. 업로드 건너뛰기.")
            return
        }
        logSystem("SYNC", "uploadArrivalAlert: ArrivalAlert 업로드 시도: ID=${alert.id}, UID=$uid")
        firestore.collection("users").document(uid).collection("arrival_alerts").document(alert.id.toString()).set(alert)
        logSystem("SYNC", "uploadArrivalAlert: ArrivalAlert 업로드 완료: ID=${alert.id}")
    }

    private suspend fun deleteRideAlertFromCloud(id: Long) {
        val uid = getUserId(); if (uid.isEmpty()) {
            logSystem("SYNC", "deleteRideAlertFromCloud: 사용자 ID가 비어 있습니다. 삭제 건너뛰기.")
            return
        }
        logSystem("SYNC", "deleteRideAlertFromCloud: RideAlert 클라우드에서 삭제 시도: ID=$id, UID=$uid")
        firestore.collection("users").document(uid).collection("ride_alerts").document(id.toString()).delete()
        logSystem("SYNC", "deleteRideAlertFromCloud: RideAlert 클라우드에서 삭제 완료: ID=$id")
    }

    private suspend fun deleteArrivalAlertFromCloud(id: Long) {
        val uid = getUserId(); if (uid.isEmpty()) {
            logSystem("SYNC", "deleteArrivalAlertFromCloud: 사용자 ID가 비어 있습니다. 삭제 건너뛰기.")
            return
        }
        logSystem("SYNC", "deleteArrivalAlertFromCloud: ArrivalAlert 클라우드에서 삭제 시도: ID=$id, UID=$uid")
        firestore.collection("users").document(uid).collection("arrival_alerts").document(id.toString()).delete()
        logSystem("SYNC", "deleteArrivalAlertFromCloud: ArrivalAlert 클라우드에서 삭제 완료: ID=${id}")
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
    suspend fun logSystem(tag: String, message: String) {
        android.util.Log.d("BusRepository", "[$tag] $message")
        busDao.insertSystemLog(SystemLog(tag = tag, message = message))
    }
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
