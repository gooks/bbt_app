package com.czt.bbt.data

import androidx.room.*
import com.czt.bbt.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BusDao {
    // Ride Alert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRideAlert(alert: RideAlert): Long

    @Query("SELECT * FROM ride_alerts")
    fun getAllRideAlerts(): Flow<List<RideAlert>>

    @Query("SELECT * FROM ride_alerts")
    suspend fun getAllRideAlertsOnce(): List<RideAlert>

    @Update
    suspend fun updateRideAlert(alert: RideAlert)

    @Delete
    suspend fun deleteRideAlert(alert: RideAlert)

    // Arrival Alert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArrivalAlert(alert: ArrivalAlert): Long

    @Query("SELECT * FROM arrival_alerts ORDER BY sortOrder ASC")
    fun getAllArrivalAlerts(): Flow<List<ArrivalAlert>>

    @Query("SELECT * FROM arrival_alerts ORDER BY sortOrder ASC")
    suspend fun getAllArrivalAlertsOnce(): List<ArrivalAlert>

    @Update
    suspend fun updateArrivalAlert(alert: ArrivalAlert)

    @Update
    suspend fun updateArrivalAlerts(alerts: List<ArrivalAlert>)

    @Delete
    suspend fun deleteArrivalAlert(alert: ArrivalAlert)

    @Query("DELETE FROM ride_alerts WHERE id = :alertId")
    suspend fun deleteRideAlertById(alertId: Long)

    @Query("DELETE FROM arrival_alerts WHERE id = :alertId")
    suspend fun deleteArrivalAlertById(alertId: Long)

    // Ride History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRideHistory(history: RideHistory): Long

    @Query("SELECT * FROM ride_histories ORDER BY boardingTime DESC")
    fun getAllRideHistories(): Flow<List<RideHistory>>

    @Query("SELECT * FROM ride_histories")
    suspend fun getAllRideHistoriesOnce(): List<RideHistory>

    @Query("DELETE FROM ride_histories WHERE id = :historyId")
    suspend fun deleteRideHistoryById(historyId: Long)

    @Query("SELECT * FROM ride_histories WHERE (busNumber LIKE '%' || :busNo || '%') AND (date BETWEEN :startDate AND :endDate) ORDER BY date DESC, boardingTime DESC")
    suspend fun getFilteredRideHistories(busNo: String, startDate: String, endDate: String): List<RideHistory>

    @Query("SELECT * FROM ride_histories ORDER BY date ASC, boardingTime ASC")
    suspend fun getAllRideHistoriesOnceSorted(): List<RideHistory>

    @Update
    suspend fun updateRideHistory(history: RideHistory)

    @Delete
    suspend fun deleteRideHistory(history: RideHistory)

    @Query("DELETE FROM ride_histories WHERE id IN (:ids)")
    suspend fun deleteRideHistoriesByIds(ids: List<Long>)

    // Route Station Cache
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedStations(stations: List<CachedRouteStation>)

    @Query("SELECT * FROM route_station_cache WHERE routeId = :routeId ORDER BY stationSeq ASC")
    suspend fun getCachedStations(routeId: String): List<CachedRouteStation>

    @Query("DELETE FROM route_station_cache WHERE timestamp < :expiryTime")
    suspend fun clearOldCache(expiryTime: Long)

    // System Logs
    @Insert
    suspend fun insertSystemLog(log: SystemLog)

    @Query("SELECT * FROM system_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentSystemLogs(): Flow<List<SystemLog>>

    @Query("DELETE FROM system_logs")
    suspend fun clearSystemLogs()
    // Clear for Sync
    @Query("DELETE FROM ride_alerts")
    suspend fun clearAllRideAlerts()
    @Query("DELETE FROM arrival_alerts")
    suspend fun clearAllArrivalAlerts()

    // Pending Changes
    @Insert
    suspend fun insertPendingChange(pendingChange: PendingChange)

    @Query("SELECT * FROM pending_changes ORDER BY timestamp ASC")
    suspend fun getPendingChanges(): List<PendingChange>

    @Query("DELETE FROM pending_changes")
    suspend fun clearPendingChanges()
}

@Database(
    entities = [RideAlert::class, ArrivalAlert::class, RideHistory::class, SystemLog::class, CachedRouteStation::class, PendingChange::class],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BusDatabase : RoomDatabase() {
    abstract fun busDao(): BusDao
}
