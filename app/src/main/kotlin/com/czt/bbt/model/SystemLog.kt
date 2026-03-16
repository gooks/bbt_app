package com.czt.bbt.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_logs")
data class SystemLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String
)
