package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.core.domain.weather.AlertDomainModel
import com.mhq.fynecast.core.domain.location.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.core.domain.time.TimeProvider
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import kotlinx.coroutines.flow.first

/**
 * Use case responsible for validating expiration criteria, permission status, and initializing background tracking for a weather alert.
 */
class TrackAlertUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertTrackingController: AlertTrackingController,
    private val timeProvider: TimeProvider,
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase
) {
    /** Possible outcome states resulting from a tracking attempt. */
    sealed interface Result {
        data object Tracked : Result
        data object AlreadyExpired : Result
        data object ExpiryUnknown : Result
        data object NotificationsRequired : Result
    }

    /**
     * Attempts to track an [alert] at a specified [trackedLocation].
     *
     * Execution flow & edge cases:
     * 1. Returns [Result.ExpiryUnknown] if the alert lacks an expiration timestamp.
     * 2. Returns [Result.AlreadyExpired] if the expiration time has already passed.
     * 3. Returns [Result.NotificationsRequired] if notifications are disabled in user preferences or system permissions are not granted.
     * 4. Otherwise, saves the alert to the repository, shows tracking notifications, and schedules background worker tasks.
     */
    suspend operator fun invoke(alert: AlertDomainModel, trackedLocation: TrackedLocation): Result {
        val expiresMillis = alert.expiresMillis ?: return Result.ExpiryUnknown
        if (expiresMillis <= timeProvider.nowMillis()) return Result.AlreadyExpired

        val enabled = observeUserPreferencesUseCase().first().notificationsEnabled
        if (!enabled || !notificationPermissionChecker.isGranted()) return Result.NotificationsRequired

        alertsRepository.saveTrackedAlert(alert, trackedLocation)
        alertTrackingController.showTrackingNotification(alertsRepository.getTrackedAlerts())
        alertTrackingController.scheduleAlertTracking(alert.id)
        alertTrackingController.scheduleLiftedCheck(trackedLocation)
        return Result.Tracked
    }
}