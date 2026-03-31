package com.czt.bbt.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ride_histories")
data class RideHistory(
    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
    val uuid: String = java.util.UUID.randomUUID().toString(), // 유니크 키 추가
    override val lastModified: Long = System.currentTimeMillis(),
    
    val date: String = "",            // 일자 (yyyy-MM-dd)
    val dayOfWeek: String = "",       // 요일
    val boardingTime: Long = 0L,       // 승차시간 (ms)
    val alightTime: Long? = null,        // 하차시간 (ms)
    val durationMinutes: Int? = null,    // 소요시간 (분)
    
    val busNumber: String = "",        // 버스 번호 (예: 700-2)
    val busRouteId: String = "",       // 노선 ID
    val vehicleId: String? = null,       // 차량 ID
    val plateNumber: String? = null,     // 차량 번호
    
    val boardingStationName: String = "", // 승차정류장명
    val boardingStationNo: String? = null,   // 승차정류장번호
    val boardingStationId: String = "",     // 승차정류장ID
    val boardingStationSeq: Int = 0,         // 승차정류장 순번 (중요: 하차 알림 기준점)
    
    val alightStationName: String? = null,   // 하차정류장명
    val alightStationNo: String? = null,    // 하차정류장번호
    val alightStationId: String? = null,    // 하차정류장ID
    
    val isManuallyStopped: Boolean = false,
    val estimatedMinutes: Int? = null,
    val stopsRemaining: Int? = null
) : CloudSyncable
