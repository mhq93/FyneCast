package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase
import kotlinx.coroutines.flow.first

class SyncNotificationPermissionUseCase(
    private val observeUserPreferences: ObserveUserPreferencesUseCase,
    private val updatePreference: UpdatePreferenceUseCase,
    private val permissionChecker: NotificationPermissionChecker
) {
    suspend operator fun invoke() {
        val current = observeUserPreferences().first()
        if (current.notificationsEnabled && !permissionChecker.isGranted()) {
            updatePreference.notifications(false)
        }
    }
}