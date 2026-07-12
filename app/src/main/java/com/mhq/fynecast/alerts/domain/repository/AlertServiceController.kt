package com.mhq.fynecast.alerts.domain.repository

interface AlertServiceController {
    fun startTracking(alertId: String, event: String, headline: String)
    fun stopTracking()
    fun removeAlertFromTracking(alertId: String)
    fun cancelTrackingWorker(alertId: String)
}