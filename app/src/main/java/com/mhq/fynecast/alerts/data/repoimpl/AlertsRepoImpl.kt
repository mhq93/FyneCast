package com.mhq.fynecast.alerts.data.repoimpl

import com.mhq.fynecast.alerts.data.database.AlertDao
import com.mhq.fynecast.alerts.data.mapper.toDomain
import com.mhq.fynecast.alerts.data.mapper.toEntity
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository

/**
 * Concrete implementation of the [AlertsRepository] using Room database operations.
 */
class AlertsRepoImpl(private val alertDao: AlertDao) : AlertsRepository {
    override fun getTrackedAlertIds() =
        alertDao.getTrackedAlertIds()

    override suspend fun getTrackedAlerts() =
        alertDao.getAllTrackedAlerts().map { it.toDomain() }

    override suspend fun saveTrackedAlert(alert: AlertDomainModel, location: TrackedLocation) =
        alertDao.insertTrackedAlert(alert.toEntity(location))

    override suspend fun deleteTrackedAlert(alertId: String) =
        alertDao.deleteTrackedAlertById(alertId)

    override suspend fun deleteAllTrackedAlerts() =
        alertDao.deleteAllTrackedAlerts()
}