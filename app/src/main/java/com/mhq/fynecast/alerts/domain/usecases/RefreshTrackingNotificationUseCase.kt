package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository

class RefreshTrackingNotificationUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertTrackingController: AlertTrackingController
) {
    /** Refreshes the ongoing tracking notification with current tracked alerts. */
    suspend operator fun invoke() =
        alertTrackingController.showTrackingNotification(alertsRepository.getTrackedAlerts())
}