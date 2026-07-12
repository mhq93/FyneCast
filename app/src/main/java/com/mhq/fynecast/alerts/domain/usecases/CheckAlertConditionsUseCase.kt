package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository

class CheckAlertConditionsUseCase(
    private val alertsRepository: AlertsRepository
) {

    suspend operator fun invoke(
        activeAlerts: List<AlertDomainModel> = emptyList(),
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<TrackedAlertDomainModel> {
        val trackedAlerts = alertsRepository.getTrackedAlertsDirectly()
        if (trackedAlerts.isEmpty()) return emptyList()

        val activeAlertIds = activeAlerts.map { it.id }.toSet()

        return trackedAlerts.filter { tracked ->
            val isExpired = currentTimeMillis >= tracked.expiresTimeMillis
            val isClearedFromApi = activeAlerts.isNotEmpty() && tracked.id !in activeAlertIds
            isExpired || isClearedFromApi
        }
    }
}