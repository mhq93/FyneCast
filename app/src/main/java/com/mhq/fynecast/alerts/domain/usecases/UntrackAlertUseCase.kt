package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository

/**
 * Use case for stopping the tracking of a specific alert and cleaning up associated workers.
 */
class UntrackAlertUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertTrackingController: AlertTrackingController
) {
    suspend operator fun invoke(alertId: String) {
        val removed = alertsRepository.getTrackedAlerts().firstOrNull { it.id == alertId }
        alertsRepository.deleteTrackedAlert(alertId)

        val remainingAlerts = alertsRepository.getTrackedAlerts()

        alertTrackingController.showTrackingNotification(remainingAlerts)
        if (removed != null && remainingAlerts.none {
                it.latitude == removed.latitude && it.longitude == removed.longitude
            }) {
            alertTrackingController.cancelLiftedCheck(removed.location)
        }

        alertTrackingController.cancelTrackingWorker(alertId)
    }
}