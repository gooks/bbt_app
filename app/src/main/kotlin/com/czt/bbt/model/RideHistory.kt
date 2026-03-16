package com.czt.bbt.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ride_histories")
data class RideHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val boardingTime: Long,
    val boardingStationName: String,
    val busNumber: String,
    val plateNumber: String?,
    val alightTime: Long? = null,
    val alightStationName: String? = null,
    val isManuallyStopped: Boolean = false
)
