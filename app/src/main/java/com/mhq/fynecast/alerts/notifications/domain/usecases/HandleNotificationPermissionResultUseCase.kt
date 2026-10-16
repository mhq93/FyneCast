package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase

/**
 * Use case that saves the user's response to the notification permission request.
 */
class HandleNotificationPermissionResultUseCase(
    private val updatePreferenceUseCase: UpdatePreferenceUseCase
) {
    suspend operator fun invoke(granted: Boolean) {
        updatePreferenceUseCase.notifications(granted)
    }
}