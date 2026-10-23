package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.alerts.domain.usecases.CancelAllAlertTrackingUseCase
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetNotificationsEnabledUseCase
import kotlinx.coroutines.flow.first

/**
 * Use case that synchronizes notification preferences with actual system runtime permissions.
 */
class SyncNotificationPermissionUseCase(
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val cancelAllAlertTrackingUseCase: CancelAllAlertTrackingUseCase
) {
    suspend operator fun invoke() {
        val current = observeUserPreferencesUseCase().first()
        if (current.notificationsEnabled && !notificationPermissionChecker.isGranted()) {
            // Permission was revoked in system settings: nothing can be shown, so stop tracking.
            cancelAllAlertTrackingUseCase()
            setNotificationsEnabledUseCase(false)
        }
    }
}