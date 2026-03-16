package com.czt.bbt.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "route_station_cache")
data class CachedRouteStation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: String,
    val stationId: String,
    val stationName: String,
    val stationSeq: Int,
    val x: Double,
    val y: Double,
    val mobileNo: String?,
    val timestamp: Long = System.currentTimeMillis()
)
