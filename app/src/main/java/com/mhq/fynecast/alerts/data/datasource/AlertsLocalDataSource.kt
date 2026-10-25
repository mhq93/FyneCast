package com.mhq.fynecast.alerts.data.datasource

import com.mhq.fynecast.alerts.data.database.TrackedAlertEntity
import kotlinx.coroutines.flow.Flow

/** Where tracked alerts are stored on the phone. Works with database entities, never domain models. */
interface AlertsLocalDataSource {
    fun observeTrackedAlertIds(): Flow<List<String>>
    suspend fun getAll(): List<TrackedAlertEntity>
    suspend fun insert(alert: TrackedAlertEntity)
    suspend fun deleteById(alertId: String)
    suspend fun deleteAll()
}
