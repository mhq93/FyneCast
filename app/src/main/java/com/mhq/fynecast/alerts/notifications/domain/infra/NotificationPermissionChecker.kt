package com.mhq.fynecast.alerts.notifications.domain.infra

/**
 * Interface for checking whether notification permissions have been granted.
 */
interface NotificationPermissionChecker {
    fun isGranted(): Boolean
}