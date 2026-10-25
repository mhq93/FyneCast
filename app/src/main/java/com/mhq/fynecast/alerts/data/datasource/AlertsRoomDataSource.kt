package com.mhq.fynecast.alerts.data.datasource

import com.mhq.fynecast.alerts.data.database.AlertDao
import com.mhq.fynecast.alerts.data.database.TrackedAlertEntity
import kotlinx.coroutines.flow.Flow

class AlertsRoomDataSource(
    private val alertDao: AlertDao
) : AlertsLocalDataSource {

    override fun observeTrackedAlertIds(): Flow<List<String>> = alertDao.getTrackedAlertIds()

    override suspend fun getAll(): List<TrackedAlertEntity> = alertDao.getAllTrackedAlerts()

    override suspend fun insert(alert: TrackedAlertEntity) = alertDao.insertTrackedAlert(alert)

    override suspend fun deleteById(alertId: String) = alertDao.deleteTrackedAlertById(alertId)

    override suspend fun deleteAll() = alertDao.deleteAllTrackedAlerts()
}
