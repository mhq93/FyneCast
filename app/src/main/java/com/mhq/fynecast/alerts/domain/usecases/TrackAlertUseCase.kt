package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertServiceController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.core.domain.time.TimeProvider
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import kotlinx.coroutines.flow.first

class TrackAlertUseCase(
    private val alertsRepository: AlertsRepository,
    private val serviceController: AlertServiceController,
    private val timeProvider: TimeProvider,
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase
) {
    sealed interface Result {
        data object Tracked : Result
        data object AlreadyExpired : Result
        data object NotificationsRequired : Result
    }

    suspend operator fun invoke(alert: AlertDomainModel, trackedLocation: TrackedLocation): Result {
        if (alert.expiresMillis <= timeProvider.nowMillis()) return Result.AlreadyExpired

        val enabled = observeUserPreferencesUseCase().first().notificationsEnabled
        if (!enabled || !notificationPermissionChecker.isGranted()) return Result.NotificationsRequired

        alertsRepository.saveTrackedAlert(alert, trackedLocation)
        serviceController.startTracking(alert.id, alert.event, alert.headline)
        serviceController.scheduleAlertTracking(alert.id)
        serviceController.scheduleLiftedCheck(trackedLocation)
        return Result.Tracked
    }
}