package com.mhq.fynecast.alerts.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing persistent tracked weather alerts in the Alerts database.
 */
@Dao
interface AlertDao {
    /** Retrieves all currently tracked weather alerts as a one-shot list. */
    @Query("SELECT * FROM alerts")
    suspend fun getAllTrackedAlerts(): List<TrackedAlertEntity>

    /** Observes a live stream of all tracked alert identifier strings. */
    @Query("SELECT id FROM alerts")
    fun getTrackedAlertIds(): Flow<List<String>>

    /** Inserts or updates a tracked alert in local storage, replacing conflicts. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackedAlert(alert: TrackedAlertEntity)

    /** Deletes a specific tracked alert by its unique identifier (ID). */
    @Query("DELETE FROM alerts WHERE id = :alertId")
    suspend fun deleteTrackedAlertById(alertId: String)

    /** Clears all tracked weather alerts from the Alerts database. */
    @Query("DELETE FROM alerts")
    suspend fun deleteAllTrackedAlerts()
}