package com.mhq.fynecast.alerts.data.repoimpl

import com.mhq.fynecast.alerts.data.database.AlertDao
import com.mhq.fynecast.alerts.data.mapper.toDomain
import com.mhq.fynecast.alerts.data.mapper.toEntity
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.domain.repository.AlertServiceController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class AlertsRepoImpl(
    private val alertDao: AlertDao,
    private val alertServiceController: AlertServiceController
) : AlertsRepository {

    override fun getAlerts(): Flow<List<AlertDomainModel>> {
        return flowOf(emptyList())
    }

    override fun getTrackedAlertIds(): Flow<List<String>> =
        alertDao.getTrackedAlertIds()

    override suspend fun getTrackedAlertsDirectly(): List<TrackedAlertDomainModel> =
        alertDao.getAllTrackedAlerts().map { it.toDomain() }

    override suspend fun trackAlert(alert: AlertDomainModel, cityName: String) {
        alertDao.insertTrackedAlert(alert.toEntity(cityName))
        alertServiceController.startTracking(alert.id, alert.event, alert.headline)
    }

    override suspend fun cancelTracking(alertId: String, cancelWorker: Boolean) {
        alertDao.deleteTrackedAlertById(alertId)

        if (cancelWorker) {
            alertServiceController.cancelTrackingWorker(alertId)
        }

        val remaining = alertDao.getAllTrackedAlerts()
        if (remaining.isEmpty()) {
            alertServiceController.stopTracking()
        } else {
            alertServiceController.removeAlertFromTracking(alertId)
        }
    }

    override suspend fun cancelAllTracking() {
        val remaining = alertDao.getAllTrackedAlerts()

        remaining.forEach {
            alertDao.deleteTrackedAlertById(it.id)
            alertServiceController.cancelTrackingWorker(it.id)
        }

        alertServiceController.stopTracking()
    }
}