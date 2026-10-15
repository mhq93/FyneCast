package com.mhq.fynecast.alerts.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts")
    suspend fun getAllTrackedAlerts(): List<TrackedAlertEntity>
    @Query("SELECT id FROM alerts")
    fun getTrackedAlertIds(): Flow<List<String>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackedAlert(alert: TrackedAlertEntity)
    @Query("DELETE FROM alerts WHERE id = :alertId")
    suspend fun deleteTrackedAlertById(alertId: String)
    @Query("DELETE FROM alerts")
    suspend fun deleteAllTrackedAlerts()
}