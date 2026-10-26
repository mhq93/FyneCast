package com.mhq.fynecast.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mhq.fynecast.alerts.data.database.AlertDao
import com.mhq.fynecast.alerts.data.database.TrackedAlertEntity
import com.mhq.fynecast.favorites.data.database.FavoriteDao
import com.mhq.fynecast.favorites.data.database.FavoriteEntity

/**
 * The app's single Room database. Its schema is exported to app/schemas on every build,
 * so every future change must bump [version] and ship a Migration.
 */
@Database(
    entities = [FavoriteEntity::class, TrackedAlertEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun alertDao(): AlertDao

    companion object {
        const val NAME = "fynecast.db"
    }
}