package com.mhq.fynecast.alerts.domain.repository

import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation

interface AlertTrackingController {
    /** Shows the ongoing "tracking" notification for these alerts; an empty list removes it. */
    fun showTrackingNotification(alerts: List<TrackedAlertDomainModel>)
    fun scheduleAlertTracking(alertId: String)
    fun cancelTrackingWorker(alertId: String)
    fun scheduleLiftedCheck(trackedLocation: TrackedLocation)
    fun cancelLiftedCheck(trackedLocation: TrackedLocation)
}