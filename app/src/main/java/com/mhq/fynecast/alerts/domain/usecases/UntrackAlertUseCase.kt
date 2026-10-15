package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertServiceController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository

class UntrackAlertUseCase(
    private val alertsRepository: AlertsRepository,
    private val serviceController: AlertServiceController
) {
    suspend operator fun invoke(alertId: String) {
        val removed = alertsRepository.getTrackedAlerts().firstOrNull { it.id == alertId }
        alertsRepository.deleteTrackedAlert(alertId)

        val remainingAlerts = alertsRepository.getTrackedAlerts()

        if (remainingAlerts.isEmpty())
            serviceController.stopTracking()
        else
            serviceController.removeAlertFromTracking(alertId)

        if (
            removed != null && remainingAlerts.none {
                it.latitude == removed.latitude && it.longitude == removed.longitude
            }
        ) {
            serviceController.cancelLiftedCheck(removed.location)
        }

        serviceController.cancelTrackingWorker(alertId)
    }
}