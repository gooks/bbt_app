package com.czt.bbt.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "arrival_alerts")
data class ArrivalAlert(
    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
    val stationName: String = "",
    val stationId: String = "",
    val stationNo: String = "", // 정류소 번호 추가
    @TypeConverters(Converters::class)
    val targetBusNumbers: List<String> = emptyList(), // RouteIDs
    @TypeConverters(Converters::class)
    val targetBusNames: List<String> = emptyList(), // RouteNames (추가)
    override val lastModified: Long = System.currentTimeMillis()
) : CloudSyncable
