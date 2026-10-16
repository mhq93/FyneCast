package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Use case that tears down all alert tracking workers, clears saved alerts, and dismisses notifications.
 */
class CancelAllAlertTrackingUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertTrackingController: AlertTrackingController
) {
    /** Executes cleanup safely within a non-cancellable context. */
    suspend operator fun invoke() = withContext(NonCancellable) {
        val tracked = alertsRepository.getTrackedAlerts()
        tracked.forEach { alertTrackingController.cancelTrackingWorker(it.id) }
        tracked.map { it.location }.distinct().forEach { alertTrackingController.cancelLiftedCheck(it) }
        alertsRepository.deleteAllTrackedAlerts()
        alertTrackingController.showTrackingNotification(emptyList())
    }
}