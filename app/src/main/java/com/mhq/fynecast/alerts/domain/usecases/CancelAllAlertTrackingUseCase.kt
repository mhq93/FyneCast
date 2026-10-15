package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertServiceController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class CancelAllAlertTrackingUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertServiceController: AlertServiceController
) {
    suspend operator fun invoke() = withContext(NonCancellable) {
        val trackedAlerts = alertsRepository.getTrackedAlerts()

        trackedAlerts.forEach {
            alertServiceController.cancelTrackingWorker(it.id)
        }

        trackedAlerts
            .map { it.location }
            .distinct()
            .forEach {
                alertServiceController.cancelLiftedCheck(it)
            }

        alertsRepository.deleteAllTrackedAlerts()
        alertServiceController.stopTracking()
    }
}