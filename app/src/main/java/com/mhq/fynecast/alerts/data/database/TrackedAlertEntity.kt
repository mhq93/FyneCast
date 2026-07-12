package com.mhq.fynecast.alerts.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class TrackedAlertEntity(
    @PrimaryKey val id: String,
    val cityName: String,
    val event: String,
    val effectiveTimeMillis: Long,
    val expiresTimeMillis: Long
)