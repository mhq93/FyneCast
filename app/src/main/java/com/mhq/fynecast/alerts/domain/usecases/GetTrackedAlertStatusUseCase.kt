package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.core.domain.time.TimeProvider

class GetTrackedAlertStatusUseCase(
    private val alertsRepository: AlertsRepository,
    private val timeProvider: TimeProvider
) {
    sealed interface Status {
        data object NotTracked : Status
        data object Active : Status
        data class Expired(val alert: TrackedAlertDomainModel) : Status
    }

    suspend operator fun invoke(alertId: String): Status {
        val alert = alertsRepository.getTrackedAlerts().firstOrNull { it.id == alertId }
            ?: return Status.NotTracked

        return if (timeProvider.nowMillis() >= alert.expiresTimeMillis)
            Status.Expired(alert)
        else Status.Active
    }
}