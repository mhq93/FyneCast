package com.mhq.fynecast.alerts.ui

/**
 * One-off UI events emitted by [AlertsViewModel] for handling side effects like snackbars or permissions.
 */
sealed interface AlertsEvent {
    data object AlertAlreadyExpired : AlertsEvent
    data object AlertExpiryUnknown : AlertsEvent
    data class AlertTrackingStarted(val eventName: String) : AlertsEvent
    data object RequestNotificationPermission : AlertsEvent
    data object NotificationPermissionDenied : AlertsEvent
}