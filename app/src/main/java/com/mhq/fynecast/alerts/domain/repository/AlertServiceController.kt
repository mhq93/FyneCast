package com.mhq.fynecast.alerts.domain.repository

import com.mhq.fynecast.alerts.domain.models.TrackedLocation

interface AlertServiceController {
    fun startTracking(alertId: String, event: String, headline: String)
    fun scheduleAlertTracking(alertId: String)
    fun scheduleLiftedCheck(trackedLocation: TrackedLocation)
    fun cancelLiftedCheck(trackedLocation: TrackedLocation)
    fun removeAlertFromTracking(alertId: String)
    fun cancelTrackingWorker(alertId: String)
    fun stopTracking()
}