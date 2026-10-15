package com.mhq.fynecast.alerts.ui

sealed interface AlertsEvent {
    data object AlertAlreadyExpired : AlertsEvent
    data object AlertExpiryUnknown : AlertsEvent
    data class AlertTrackingStarted(val eventName: String) : AlertsEvent
    data object RequestNotificationPermission : AlertsEvent
    data object NotificationPermissionDenied : AlertsEvent
}