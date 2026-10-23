package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.alerts.domain.usecases.CancelAllAlertTrackingUseCase
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.settings.domain.usecases.profile.SetNotificationsEnabledUseCase

/**
 * Use case for enabling or disabling notifications, handling permissions and tracker cleanup.
 */
class ToggleNotificationsUseCase(
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val cancelAllAlertTrackingUseCase: CancelAllAlertTrackingUseCase
) {
    sealed interface Result {
        data object Enabled : Result
        data object Disabled : Result
        data object PermissionRequired : Result
    }

    suspend operator fun invoke(wantsEnabled: Boolean): Result {
        if (!wantsEnabled) {
            // Tear tracking down first. If this fails, the preference stays "enabled",
            // so the UI never claims notifications are off while workers still run.
            cancelAllAlertTrackingUseCase()
            setNotificationsEnabledUseCase(false)
            return Result.Disabled
        }

        return if (notificationPermissionChecker.isGranted()) {
            setNotificationsEnabledUseCase(true)
            Result.Enabled
        } else {
            Result.PermissionRequired
        }
    }
}