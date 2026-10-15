package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.core.domain.time.TimeProvider

class RestoreAlertTrackingUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertTrackingController: AlertTrackingController,
    private val untrackAlertUseCase: UntrackAlertUseCase,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke() {
        val now = timeProvider.nowMillis()
        val (expired, live) = alertsRepository.getTrackedAlerts()
            .partition { now >= it.expiresTimeMillis }
        expired.forEach { untrackAlertUseCase(it.id) }
        live.forEach { alertTrackingController.scheduleAlertTracking(it.id) }
        live.map { it.location }.distinct()
            .forEach { alertTrackingController.scheduleLiftedCheck(it) }
        alertTrackingController.showTrackingNotification(live)
    }
}