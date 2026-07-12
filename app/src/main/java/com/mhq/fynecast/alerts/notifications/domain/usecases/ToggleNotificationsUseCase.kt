package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase

class ToggleNotificationsUseCase(
    private val updatePreference: UpdatePreferenceUseCase,
    private val permissionChecker: NotificationPermissionChecker
) {
    sealed interface Result {
        data object Enabled : Result
        data object Disabled : Result
        data object PermissionRequired : Result
    }

    suspend operator fun invoke(wantsEnabled: Boolean): Result {
        if (!wantsEnabled) {
            updatePreference.notifications(false)
            return Result.Disabled
        }

        return if (permissionChecker.isGranted()) {
            updatePreference.notifications(true)
            Result.Enabled
        } else {
            Result.PermissionRequired
        }
    }
}