package com.czt.bbt.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.czt.bbt.api.BusApiService
import com.czt.bbt.model.*
import com.czt.bbt.ui.BusAlertState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusRepository @Inject constructor(
    private val apiService: BusApiService,
    private val busDao: BusDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    private val apiPrefs = context.getSharedPreferences("api_usage_prefs", Context.MODE_PRIVATE)
    private val mainPrefs = context.getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE)
    private val serviceKey = "cdc5e165cd54d32593270eab28c0671f54386348ccad640e03daf36a681f5241"
    private val gson = Gson()
    private val firestore = FirebaseFirestore.getInstance()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var rideAlertsListener: ListenerRegistration? = null
    private var arrivalAlertsListener: ListenerRegistration? = null
    private var rideHistoriesListener: ListenerRegistration? = null


    // --- New Synchronization Logic ---

    private fun getUserId(): String {
        return mainPrefs.getString("google_user_id", "") ?: ""
    }

    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     *  Registers realtime listeners for cloud data. Should be called when user is logged in and online.
     */
    fun registerRealtimeSync() {
        val uid = getUserId()
        if (uid.isEmpty() || !isOnline()) {
            repositoryScope.launch { logSystem("SYNC_RT", "Realtime sync skipped: UID empty or offline.") }
            return
        }
        repositoryScope.launch { logSystem("SYNC_RT", "Registering realtime sync listeners.") }
        
        // Detach existing listeners to prevent duplicates
        detachRealtimeSync()

        val rideAlertsCollection = firestore.collection("users").document(uid).collection("ride_alerts")
        rideAlertsListener = rideAlertsCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                repositoryScope.launch { logSystem("SYNC_RT_FAIL", "RideAlerts listener error: ${e.message}") }
                return@addSnapshotListener
            }
            repositoryScope.launch {
                val cloudAlerts = snapshots?.toObjects(RideAlert::class.java) ?: emptyList()
                logSystem("SYNC_RT", "Received ${cloudAlerts.size} RideAlerts from cloud.")
                updateLocalFromCloud(cloudAlerts, busDao::getAllRideAlertsOnce, busDao::insertRideAlert, busDao::updateRideAlert, busDao::deleteRideAlertById)
            }
        }

        val arrivalAlertsCollection = firestore.collection("users").document(uid).collection("arrival_alerts")
        arrivalAlertsListener = arrivalAlertsCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                repositoryScope.launch { logSystem("SYNC_RT_FAIL", "ArrivalAlerts listener error: ${e.message}") }
                return@addSnapshotListener
            }
            repositoryScope.launch {
                val cloudAlerts = snapshots?.toObjects(ArrivalAlert::class.java) ?: emptyList()
                logSystem("SYNC_RT", "Received ${cloudAlerts.size} ArrivalAlerts from cloud.")
                updateLocalFromCloud(cloudAlerts, busDao::getAllArrivalAlertsOnce, busDao::insertArrivalAlert, busDao::updateArrivalAlert, busDao::deleteArrivalAlertById)
            }
        }

        val rideHistoriesCollection = firestore.collection("users").document(uid).collection("histories")
        rideHistoriesListener = rideHistoriesCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                repositoryScope.launch { logSystem("SYNC_RT_FAIL", "RideHistories listener error: ${e.message}") }
                return@addSnapshotListener
            }
            repositoryScope.launch {
                val cloudHistories = snapshots?.toObjects(RideHistory::class.java) ?: emptyList()
                logSystem("SYNC_RT", "Received ${cloudHistories.size} RideHistories from cloud.")
                updateLocalFromCloud(cloudHistories, busDao::getAllRideHistoriesOnce, busDao::insertRideHistory, busDao::updateRideHistory, busDao::deleteRideHistoryById)
            }
        }
    }
    
    /**
     * Detaches Firestore listeners. Should be called on user logout or when app goes to background.
     */
    fun detachRealtimeSync() {
        repositoryScope.launch { logSystem("SYNC_RT", "Detaching realtime sync listeners.") }
        rideAlertsListener?.remove()
        arrivalAlertsListener?.remove()
        rideHistoriesListener?.remove()
        rideAlertsListener = null
        arrivalAlertsListener = null
        rideHistoriesListener = null
    }

    private suspend inline fun <reified T : CloudSyncable> updateLocalFromCloud(
        cloudAlerts: List<T>,
        getLocalAlerts: suspend () -> List<T>,
        crossinline insertDb: suspend (T) -> Long,
        crossinline updateDb: suspend (T) -> Unit,
        crossinline deleteLocalDb: suspend (Long) -> Unit
    ) {
        val localAlerts = getLocalAlerts()
        val localMap = localAlerts.associateBy { it.id }
        val cloudMap = cloudAlerts.associateBy { it.id }

        // Add/Update local data based on cloud
        for (cloudAlert in cloudAlerts) {
            val localAlert = localMap[cloudAlert.id]
            if (localAlert == null) {
                insertDb(cloudAlert)
                logSystem("SYNC_RT_UPDATE", "Cloud->Local: Inserted new alert ID ${cloudAlert.id}")
            } else if (cloudAlert.lastModified > localAlert.lastModified) {
                updateDb(cloudAlert)
                logSystem("SYNC_RT_UPDATE", "Cloud->Local: Updated alert ID ${cloudAlert.id} (Cloud is newer)")
            }
        }

        // Delete from local if not in cloud
        for (localAlert in localAlerts) {
            if (!cloudMap.containsKey(localAlert.id)) {
                deleteLocalDb(localAlert.id)
                logSystem("SYNC_RT_UPDATE", "Cloud->Local: Deleted alert ID ${localAlert.id} (Not in cloud)")
            }
        }
    }
    
    /**
     *  Reconciles local pending changes with the cloud. Should be called when app comes online.
     */
    suspend fun reconcileWithCloud() {
        val uid = getUserId()
        if (uid.isEmpty() || !isOnline()) {
            logSystem("RECONCILE", "Reconciliation skipped: UID empty or offline.")
            return
        }
        logSystem("RECONCILE", "Starting data reconciliation.")

        val pendingChanges = busDao.getPendingChanges()
        if (pendingChanges.isEmpty()) {
            logSystem("RECONCILE", "No pending changes to sync.")
            // Even with no pending changes, a full sync is good practice.
            performFullSync()
            return
        }

        logSystem("RECONCILE", "Processing ${pendingChanges.size} pending changes.")
        for (change in pendingChanges) {
            try {
                when (change.objectType) {
                    "RideAlert" -> processPendingChange<RideAlert>(change, "ride_alerts")
                    "ArrivalAlert" -> processPendingChange<ArrivalAlert>(change, "arrival_alerts")
                    "RideHistory" -> processPendingChange<RideHistory>(change, "histories")
                }
            } catch (e: Exception) {
                logSystem("RECONCILE_FAIL", "Failed to process change ${change.id}: ${e.message}")
            }
        }
        busDao.clearPendingChanges()
        logSystem("RECONCILE", "Pending changes processed. Now performing full sync.")
        performFullSync()
        logSystem("RECONCILE", "Reconciliation complete.")
    }

    private suspend inline fun <reified T : CloudSyncable> processPendingChange(change: PendingChange, collectionName: String) {
        val collection = firestore.collection("users").document(getUserId()).collection(collectionName)
        val docRef = collection.document(change.objectId.toString())

        when (change.type) {
            "DELETE" -> {
                docRef.delete().await()
                logSystem("RECONCILE_PROC", "Processed PENDING DELETE for ${change.objectType} ID ${change.objectId}")
            }
            "ADD", "EDIT" -> {
                val pendingObject = gson.fromJson(change.objectJson, T::class.java)
                val cloudDoc = docRef.get().await()

                if (!cloudDoc.exists() || (cloudDoc.toObject(T::class.java)?.lastModified ?: 0) < pendingObject.lastModified) {
                    docRef.set(pendingObject).await()
                    logSystem("RECONCILE_PROC", "Processed PENDING ${change.type} for ${change.objectType} ID ${change.objectId} (Local was newer)")
                } else {
                     logSystem("RECONCILE_PROC", "Skipped PENDING ${change.type} for ${change.objectType} ID ${change.objectId} (Cloud was newer)")
                }
            }
        }
    }
    
    /**
     * Performs a two-way sync between local and cloud, respecting lastModified timestamp.
     * This is the "Last Write Wins" logic applied to the entire dataset.
     */
    private suspend fun performFullSync() {
        val uid = getUserId()
        if (uid.isEmpty() || !isOnline()) return

        logSystem("FULL_SYNC", "Starting full two-way sync.")
        syncCollection<RideAlert>("ride_alerts", busDao::getAllRideAlertsOnce, busDao::insertRideAlert, busDao::updateRideAlert, busDao::deleteRideAlertById)
        syncCollection<ArrivalAlert>("arrival_alerts", busDao::getAllArrivalAlertsOnce, busDao::insertArrivalAlert, busDao::updateArrivalAlert, busDao::deleteArrivalAlertById)
        syncCollection<RideHistory>("histories", { busDao.getAllRideHistoriesOnceSorted() }, busDao::insertRideHistory, busDao::updateRideHistory, { id -> busDao.deleteRideHistoriesByIds(listOf(id)) })
        logSystem("FULL_SYNC", "Full two-way sync complete.")
    }

    private suspend inline fun <reified T : CloudSyncable> syncCollection(
        collectionName: String,
        getLocalAlerts: suspend () -> List<T>,
        crossinline insertDb: suspend (T) -> Long,
        crossinline updateDb: suspend (T) -> Unit,
        crossinline deleteLocalDb: suspend (Long) -> Unit
    ) {
        val collection = firestore.collection("users").document(getUserId()).collection(collectionName)
        
        val localAlerts = getLocalAlerts()
        val cloudAlerts = try {
            collection.get().await().toObjects(T::class.java)
        } catch (e: Exception) {
            logSystem("FULL_SYNC_FAIL", "Failed to get cloud collection $collectionName: ${e.message}")
            return
        }

        val localMap = localAlerts.associateBy { it.id }
        val cloudMap = cloudAlerts.associateBy { it.id }
        val allIds = localMap.keys + cloudMap.keys

        for (id in allIds) {
            val local = localMap[id]
            val cloud = cloudMap[id]

            when {
                // Exists in both: "Last Write Wins"
                local != null && cloud != null -> {
                    if (local.lastModified > cloud.lastModified) {
                        collection.document(id.toString()).set(local) // Local is newer, update cloud
                        logSystem("FULL_SYNC_MERGE", "Local -> Cloud: ID $id in $collectionName")
                    } else if (cloud.lastModified > local.lastModified) {
                        updateDb(cloud) // Cloud is newer, update local
                         logSystem("FULL_SYNC_MERGE", "Cloud -> Local: ID $id in $collectionName")
                    }
                }
                // Exists only in local: Upload to cloud
                local != null && cloud == null -> {
                    collection.document(id.toString()).set(local)
                    logSystem("FULL_SYNC_MERGE", "Local -> Cloud (New): ID $id in $collectionName")
                }
                // Exists only in cloud: Add to local
                local == null && cloud != null -> {
                    insertDb(cloud)
                    logSystem("FULL_SYNC_MERGE", "Cloud -> Local (New): ID $id in $collectionName")
                }
            }
        }
    }

    // --- Modified CRUD Operations ---

    suspend fun insertRideAlert(alert: RideAlert): Long {
        val newAlert = alert.copy(lastModified = System.currentTimeMillis())
        val id = busDao.insertRideAlert(newAlert)
        val finalAlert = newAlert.copy(id = id)

        if (isOnline()) {
            uploadCloudObject("ride_alerts", finalAlert)
        } else {
            logSystem("OFFLINE", "Queuing ADD RideAlert ID $id")
            busDao.insertPendingChange(PendingChange(type = "ADD", objectType = "RideAlert", objectId = id, objectJson = gson.toJson(finalAlert)))
        }
        return id
    }

    suspend fun updateRideAlert(alert: RideAlert) {
        val updatedAlert = alert.copy(lastModified = System.currentTimeMillis())
        busDao.updateRideAlert(updatedAlert)

        if (isOnline()) {
            uploadCloudObject("ride_alerts", updatedAlert)
        } else {
            logSystem("OFFLINE", "Queuing EDIT RideAlert ID ${updatedAlert.id}")
            busDao.insertPendingChange(PendingChange(type = "EDIT", objectType = "RideAlert", objectId = updatedAlert.id, objectJson = gson.toJson(updatedAlert)))
        }
    }

    suspend fun deleteRideAlert(alert: RideAlert) {
        busDao.deleteRideAlert(alert)
        if (isOnline()) {
            deleteCloudObject("ride_alerts", alert.id)
        } else {
            logSystem("OFFLINE", "Queuing DELETE RideAlert ID ${alert.id}")
            busDao.insertPendingChange(PendingChange(type = "DELETE", objectType = "RideAlert", objectId = alert.id, objectJson = null, timestamp = System.currentTimeMillis()))
        }
    }
    
    suspend fun insertArrivalAlert(alert: ArrivalAlert): Long {
        val newAlert = alert.copy(lastModified = System.currentTimeMillis())
        val id = busDao.insertArrivalAlert(newAlert)
        val finalAlert = newAlert.copy(id = id)
        
        if (isOnline()) {
            uploadCloudObject("arrival_alerts", finalAlert)
        } else {
            logSystem("OFFLINE", "Queuing ADD ArrivalAlert ID $id")
            busDao.insertPendingChange(PendingChange(type = "ADD", objectType = "ArrivalAlert", objectId = id, objectJson = gson.toJson(finalAlert)))
        }
        return id
    }

    suspend fun updateArrivalAlert(alert: ArrivalAlert) {
        val updatedAlert = alert.copy(lastModified = System.currentTimeMillis())
        busDao.updateArrivalAlert(updatedAlert)
        
        if (isOnline()) {
            uploadCloudObject("arrival_alerts", updatedAlert)
        } else {
            logSystem("OFFLINE", "Queuing EDIT ArrivalAlert ID ${updatedAlert.id}")
            busDao.insertPendingChange(PendingChange(type = "EDIT", objectType = "ArrivalAlert", objectId = updatedAlert.id, objectJson = gson.toJson(updatedAlert)))
        }
    }

    suspend fun deleteArrivalAlert(alert: ArrivalAlert) {
        busDao.deleteArrivalAlert(alert)
        if (isOnline()) {
            deleteCloudObject("arrival_alerts", alert.id)
        } else {
            logSystem("OFFLINE", "Queuing DELETE ArrivalAlert ID ${alert.id}")
            busDao.insertPendingChange(PendingChange(type = "DELETE", objectType = "ArrivalAlert", objectId = alert.id, objectJson = null, timestamp = System.currentTimeMillis()))
        }
    }

    suspend fun updateArrivalAlertsOrder(alerts: List<ArrivalAlert>) {
        val updatedAlerts = alerts.map { it.copy(lastModified = System.currentTimeMillis()) }
        busDao.updateArrivalAlerts(updatedAlerts)
        
        if (isOnline()) {
            updatedAlerts.forEach { uploadCloudObject("arrival_alerts", it) }
        } else {
            updatedAlerts.forEach { alert ->
                busDao.insertPendingChange(PendingChange(type = "EDIT", objectType = "ArrivalAlert", objectId = alert.id, objectJson = gson.toJson(alert)))
            }
        }
    }

    suspend fun getFilteredRideHistories(busNo: String, startDate: String, endDate: String): List<RideHistory> {
        return busDao.getFilteredRideHistories(busNo, startDate, endDate)
    }

    suspend fun deleteRideHistories(ids: List<Long>) {
        busDao.deleteRideHistoriesByIds(ids)
        ids.forEach { id ->
            if (isOnline()) {
                deleteCloudObject("histories", id)
            } else {
                busDao.insertPendingChange(PendingChange(type = "DELETE", objectType = "RideHistory", objectId = id, objectJson = null, timestamp = System.currentTimeMillis()))
            }
        }
    }

    suspend fun getAllRideHistoriesSorted(): List<RideHistory> {
        return busDao.getAllRideHistoriesOnceSorted()
    }

    private suspend fun <T : CloudSyncable> uploadCloudObject(collectionName: String, alert: T) {
        val uid = getUserId()
        if (uid.isEmpty()) return
        try {
            firestore.collection("users").document(uid).collection(collectionName).document(alert.id.toString()).set(alert).await()
            logSystem("SYNC_UP", "Uploaded $collectionName ID ${alert.id}")
        } catch (e: Exception) {
            logSystem("SYNC_UP_FAIL", "Failed to upload $collectionName ID ${alert.id}: ${e.message}")
        }
    }
    
    private suspend fun deleteCloudObject(collectionName: String, id: Long) {
        val uid = getUserId()
        if (uid.isEmpty()) return
        try {
            firestore.collection("users").document(uid).collection(collectionName).document(id.toString()).delete().await()
            logSystem("SYNC_DEL", "Deleted from cloud: $collectionName ID $id")
        } catch (e: Exception) {
            logSystem("SYNC_DEL_FAIL", "Failed to delete from cloud: $collectionName ID $id: ${e.message}")
        }
    }

    // --- Other public methods (unchanged) ---

    fun getAllRideAlerts(): Flow<List<RideAlert>> = busDao.getAllRideAlerts()
    fun getAllArrivalAlerts(): Flow<List<ArrivalAlert>> = busDao.getAllArrivalAlerts()
    suspend fun insertRideHistory(history: RideHistory): Long {
        val newHistory = history.copy(lastModified = System.currentTimeMillis())
        val id = busDao.insertRideHistory(newHistory)
        val finalHistory = newHistory.copy(id = id)

        if (isOnline()) {
            uploadCloudObject("histories", finalHistory)
        } else {
            logSystem("OFFLINE", "Queuing ADD RideHistory ID $id")
            busDao.insertPendingChange(PendingChange(type = "ADD", objectType = "RideHistory", objectId = id, objectJson = gson.toJson(finalHistory)))
        }
        return id
    }

    fun getAllRideHistories(): Flow<List<RideHistory>> = busDao.getAllRideHistories()

    suspend fun updateRideHistory(history: RideHistory) {
        val updatedHistory = history.copy(lastModified = System.currentTimeMillis())
        busDao.updateRideHistory(updatedHistory)

        if (isOnline()) {
            uploadCloudObject("histories", updatedHistory)
        } else {
            logSystem("OFFLINE", "Queuing EDIT RideHistory ID ${updatedHistory.id}")
            busDao.insertPendingChange(PendingChange(type = "EDIT", objectType = "RideHistory", objectId = updatedHistory.id, objectJson = gson.toJson(updatedHistory)))
        }
    }

    suspend fun deleteRideHistory(history: RideHistory) {
        busDao.deleteRideHistory(history)
        if (isOnline()) {
            deleteCloudObject("histories", history.id)
        } else {
            logSystem("OFFLINE", "Queuing DELETE RideHistory ID ${history.id}")
            busDao.insertPendingChange(PendingChange(type = "DELETE", objectType = "RideHistory", objectId = history.id, objectJson = null, timestamp = System.currentTimeMillis()))
        }
    }

    suspend fun saveGoogleAppPasswordToCloud(password: String) {
        val uid = getUserId(); if (uid.isEmpty()) return
        firestore.collection("users").document(uid).set(mapOf("google_app_password" to password), com.google.firebase.firestore.SetOptions.merge())
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
    
    // Logs
    suspend fun logSystem(tag: String, message: String) {
        withContext(Dispatchers.IO) {
            android.util.Log.d("BusRepository", "[$tag] $message")
            busDao.insertSystemLog(SystemLog(tag = tag, message = message))
        }
    }
    fun getSystemLogs(): Flow<List<SystemLog>> = busDao.getRecentSystemLogs()
    suspend fun clearLogs() = busDao.clearSystemLogs()

    private fun updateApiCount(tag: String) {
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastDate = apiPrefs.getString("last_date", "")
        if (today != lastDate) { apiPrefs.edit().clear().putString("last_date", today).apply() }
        
        val currentCount = apiPrefs.getInt(tag, 1000)
        val newCount = (currentCount - 1).coerceAtLeast(0)
        apiPrefs.edit().putInt(tag, newCount).apply()
        
        val currentMap = BusAlertState.apiUsageFlow.value.toMutableMap()
        currentMap[tag] = newCount
        BusAlertState.apiUsageFlow.value = currentMap
    }

    private suspend fun <T> apiCall(tag: String, call: suspend () -> retrofit2.Response<T>): T {
        updateApiCount(tag)
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
