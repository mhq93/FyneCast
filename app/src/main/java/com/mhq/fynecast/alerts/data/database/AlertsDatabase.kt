package com.mhq.fynecast.alerts.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database abstract class serving as the main access point for the tracked alerts persistence layer.
 */
@Database(
    entities = [TrackedAlertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AlertsDatabase : RoomDatabase() {
    /** Provides access to the [AlertDao] data operations. */
    abstract fun alertDao(): AlertDao
}