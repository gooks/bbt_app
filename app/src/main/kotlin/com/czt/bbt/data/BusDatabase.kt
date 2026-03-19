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

    @Update
    suspend fun updateRideAlert(alert: RideAlert)

    @Delete
    suspend fun deleteRideAlert(alert: RideAlert)

    // Arrival Alert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArrivalAlert(alert: ArrivalAlert): Long

    @Query("SELECT * FROM arrival_alerts")
    fun getAllArrivalAlerts(): Flow<List<ArrivalAlert>>

    @Update
    suspend fun updateArrivalAlert(alert: ArrivalAlert)

    @Delete
    suspend fun deleteArrivalAlert(alert: ArrivalAlert)

    // Ride History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRideHistory(history: RideHistory): Long

    @Query("SELECT * FROM ride_histories ORDER BY boardingTime DESC")
    fun getAllRideHistories(): Flow<List<RideHistory>>

    @Update
    suspend fun updateRideHistory(history: RideHistory)

    @Delete
    suspend fun deleteRideHistory(history: RideHistory)

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
}

@Database(
    entities = [RideAlert::class, ArrivalAlert::class, RideHistory::class, SystemLog::class, CachedRouteStation::class],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BusDatabase : RoomDatabase() {
    abstract fun busDao(): BusDao
}
