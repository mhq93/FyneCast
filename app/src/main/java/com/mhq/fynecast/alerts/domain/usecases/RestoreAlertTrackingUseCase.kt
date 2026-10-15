package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertServiceController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.core.domain.time.TimeProvider

class RestoreAlertTrackingUseCase(
    private val alertsRepository: AlertsRepository,
    private val serviceController: AlertServiceController,
    private val untrackAlertUseCase: UntrackAlertUseCase,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke() {
        val now = timeProvider.nowMillis()

        val (expired, live) =
            alertsRepository.getTrackedAlerts()
                .partition { now >= it.expiresTimeMillis }

        expired.forEach {
            untrackAlertUseCase(it.id)
        }

        live.forEach {
            serviceController.startTracking(it.id, it.event, it.headline)
            serviceController.scheduleAlertTracking(it.id)
        }

        live.map { it.location }.distinct().forEach {
            serviceController.scheduleLiftedCheck(it)
        }
    }
}