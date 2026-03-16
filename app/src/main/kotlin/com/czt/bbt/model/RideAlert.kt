package com.czt.bbt.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "ride_alerts")
data class RideAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val busNumber: String,
    val busRouteId: String,
    val destinationStationName: String,
    val destinationStationId: String,
    val destinationStationSeq: Int,
    @TypeConverters(Converters::class)
    val shareEmails: List<String> = emptyList(),
    val shareKakao: Boolean = false,
    val shareMemo: String = "" // 추가
)
