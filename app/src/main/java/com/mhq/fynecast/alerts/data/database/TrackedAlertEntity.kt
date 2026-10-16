package com.mhq.fynecast.alerts.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity representing a weather alert that the user has chosen to track.
 */
@Entity(tableName = "alerts")
data class TrackedAlertEntity(
    @PrimaryKey
    val id: String,
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val headline: String,
    val event: String,
    val effectiveTimeMillis: Long,
    val expiresTimeMillis: Long
)