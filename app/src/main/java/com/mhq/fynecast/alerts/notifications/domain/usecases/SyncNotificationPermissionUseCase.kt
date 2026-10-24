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
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val cancelAllAlertTrackingUseCase: CancelAllAlertTrackingUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val notificationPermissionChecker: NotificationPermissionChecker
) {
    suspend operator fun invoke() {
        val current = observeUserPreferencesUseCase().first()
        val systemGranted = notificationPermissionChecker.isGranted()

        if (current.notificationsEnabled && !systemGranted) {
            /** System permission was revoked: turn tracking off locally */
            cancelAllAlertTrackingUseCase()
            setNotificationsEnabledUseCase(false)
        } else if (!current.notificationsEnabled && systemGranted) {
            /** System permission was granted from Settings:
             * sync the local preference toggle to on */
            setNotificationsEnabledUseCase(true)
        }
    }
}
