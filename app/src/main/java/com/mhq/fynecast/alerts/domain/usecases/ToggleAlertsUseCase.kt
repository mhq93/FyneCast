package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.first

class ToggleAlertsUseCase(
    private val alertsRepository: AlertsRepository,
    private val trackAlertUseCase: TrackAlertUseCase,
    private val untrackAlertUseCase: UntrackAlertUseCase
) {
    sealed interface Result {
        data object Tracked : Result
        data object Untracked : Result
        data object AlreadyExpired : Result
        data object NotificationsRequired : Result
    }

    suspend operator fun invoke(alert: AlertDomainModel, trackedLocation: TrackedLocation): Result {
        if (alert.id in alertsRepository.getTrackedAlertIds().first()) {
            untrackAlertUseCase(alert.id)
            return Result.Untracked
        }
        return when (trackAlertUseCase(alert, trackedLocation)) {
            TrackAlertUseCase.Result.Tracked -> Result.Tracked
            TrackAlertUseCase.Result.AlreadyExpired -> Result.AlreadyExpired
            TrackAlertUseCase.Result.NotificationsRequired -> Result.NotificationsRequired
        }
    }
}