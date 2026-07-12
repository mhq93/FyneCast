package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.first

class ToggleAlertsUseCase(private val alertsRepository: AlertsRepository) {
    suspend operator fun invoke(alert: AlertDomainModel, cityName: String) {
        val trackedIds = alertsRepository.getTrackedAlertIds().first()

        if (trackedIds.contains(alert.id)) {
            alertsRepository.cancelTracking(alertId = alert.id, cancelWorker = true)
            return
        }

        val isExpired = alert.expiresMillis <= System.currentTimeMillis()
        if (isExpired) return

        alertsRepository.trackAlert(alert, cityName)
    }
}