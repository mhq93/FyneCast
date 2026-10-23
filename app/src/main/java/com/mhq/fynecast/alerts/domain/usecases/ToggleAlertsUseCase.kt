package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.core.domain.weather.AlertDomainModel
import com.mhq.fynecast.core.domain.location.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.first

/**
 * Use case for switching an alert between tracked and untracked states.
 */
class ToggleAlertsUseCase(
    private val alertsRepository: AlertsRepository,
    private val trackAlertUseCase: TrackAlertUseCase,
    private val untrackAlertUseCase: UntrackAlertUseCase
) {
    /** Result states of a toggle operation. */
    sealed interface Result {
        data object Tracked : Result
        data object Untracked : Result
        data object AlreadyExpired : Result
        data object ExpiryUnknown : Result
        data object NotificationsRequired : Result
    }

    /** Toggles tracking on or off depending on whether the alert is already tracked. */
    suspend operator fun invoke(alert: AlertDomainModel, trackedLocation: TrackedLocation): Result {
        if (alert.id in alertsRepository.getTrackedAlertIds().first()) {
            untrackAlertUseCase(alert.id)
            return Result.Untracked
        }
        return when (trackAlertUseCase(alert, trackedLocation)) {
            TrackAlertUseCase.Result.Tracked -> Result.Tracked
            TrackAlertUseCase.Result.AlreadyExpired -> Result.AlreadyExpired
            TrackAlertUseCase.Result.ExpiryUnknown -> Result.ExpiryUnknown
            TrackAlertUseCase.Result.NotificationsRequired -> Result.NotificationsRequired
        }
    }
}