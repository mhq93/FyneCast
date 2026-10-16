package com.mhq.fynecast.alerts.domain.repository

import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation

/**
 * Controller interface responsible for scheduling background workers and managing alert notifications.
 */
interface AlertTrackingController {
    /** Shows the ongoing "tracking" notification for active alerts; an empty list removes it. */
    fun showTrackingNotification(alerts: List<TrackedAlertDomainModel>)

    /** Schedules background periodic checks for a specific tracked alert. */
    fun scheduleAlertTracking(alertId: String)

    /** Cancels the background tracking worker associated with an alert ID. */
    fun cancelTrackingWorker(alertId: String)

    /** Schedules periodic checks to see if alerts have been lifted for a location. */
    fun scheduleLiftedCheck(trackedLocation: TrackedLocation)

    /** Cancels periodic lifted-check workers for a specific location. */
    fun cancelLiftedCheck(trackedLocation: TrackedLocation)
}