package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.settings.domain.usecases.profile.SetNotificationsEnabledUseCase

/**
 * Use case that saves the user's response to the notification permission request.
 */
class HandleNotificationPermissionResultUseCase(
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase
) {
    suspend operator fun invoke(granted: Boolean) {
        setNotificationsEnabledUseCase(granted)
    }
}