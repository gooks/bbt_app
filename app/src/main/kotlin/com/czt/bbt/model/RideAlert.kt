package com.czt.bbt.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "ride_alerts")
data class RideAlert(
    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
    val busNumber: String = "",
    val busRouteId: String = "",
    val destinationStationName: String = "",
    val destinationStationId: String = "",
    val destinationStationSeq: Int = 0,
    @TypeConverters(Converters::class)
    val shareEmails: List<String> = emptyList(),
    val shareKakao: Boolean = false,
    val shareType: String = "REALTIME", // "REALTIME" 또는 "HISTORY"
    val shareKakaoTarget: String = "", // 공유 대상 이름 (예: 엄마, 친구)
    val shareMemo: String = "", // 추가
    override val lastModified: Long = System.currentTimeMillis()
) : CloudSyncable
