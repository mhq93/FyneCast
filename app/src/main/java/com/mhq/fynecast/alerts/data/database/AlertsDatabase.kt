package com.mhq.fynecast.alerts.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TrackedAlertEntity::class], version = 1, exportSchema = false)
abstract class AlertsDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
}