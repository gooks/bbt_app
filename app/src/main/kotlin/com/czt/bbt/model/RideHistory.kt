package com.czt.bbt.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ride_histories")
data class RideHistory(
    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
    override val lastModified: Long = System.currentTimeMillis(),
    
    val date: String,            // 일자 (yyyy-MM-dd)
    val dayOfWeek: String,      // 요일 (E)
    val boardingTime: Long,      // 승차시간 (Timestamp)
    val alightTime: Long? = null, // 하차시간 (Timestamp)
    val durationMinutes: Int? = null, // 소요시간(분)
    
    val busNumber: String,       // 버스번호 (예: M4130)
    val busRouteId: String,      // 버스ID (RouteId)
    val plateNumber: String?,    // 차량번호 (예: 경기77바1176)
    val vehicleId: String?,      // 차량ID (VehId)
    
    val boardingStationName: String, // 승차정류장명
    val boardingStationNo: String?,   // 승차정류장번호
    val boardingStationId: String,   // 승차정류장ID
    
    val alightStationName: String?,  // 하차정류장명
    val alightStationNo: String?,    // 하차정류장번호
    val alightStationId: String?,    // 하차정류장ID
    
    val isManuallyStopped: Boolean = false,
    val estimatedMinutes: Int? = null,
    val stopsRemaining: Int? = null
) : CloudSyncable
