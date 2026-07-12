package com.mhq.fynecast.alerts.notifications.domain.infra

interface NotificationPermissionChecker {
    fun isGranted(): Boolean
}