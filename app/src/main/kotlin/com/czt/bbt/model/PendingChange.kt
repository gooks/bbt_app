package com.czt.bbt.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_changes")
data class PendingChange(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "ADD", "EDIT", "DELETE"
    val objectType: String, // "RideAlert", "ArrivalAlert"
    val objectId: Long,
    val objectJson: String?, // ADD/EDIT 시 객체의 JSON 데이터, DELETE 시 null
    val timestamp: Long = System.currentTimeMillis()
)
