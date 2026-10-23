package com.mhq.fynecast.alerts.data.repoimpl

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.core.domain.location.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.notifications.WeatherAlertTrackingWorker
import com.mhq.fynecast.alerts.notifications.WeatherRefreshWorker
import com.mhq.fynecast.alerts.notifications.data.infra.AlertsNotificationManager
import java.util.concurrent.TimeUnit

/**
 * Concrete implementation of [AlertTrackingController] managing Android WorkManager tasks and notifications.
 */
class AlertTrackingControllerImpl(
    private val context: Context
) : AlertTrackingController {

    private val workManager get() = WorkManager.getInstance(context)
    private val notificationManager by lazy { AlertsNotificationManager(context) }

    override fun showTrackingNotification(alerts: List<TrackedAlertDomainModel>) =
        notificationManager.showTrackingNotification(alerts)

    override fun scheduleAlertTracking(alertId: String) {
        val request = PeriodicWorkRequestBuilder<WeatherAlertTrackingWorker>(
            CHECK_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setInputData(
                workDataOf(WeatherAlertTrackingWorker.KEY_ALERT_ID to alertId)
            )
            .setInitialDelay(
                CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            alertId,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun scheduleLiftedCheck(trackedLocation: TrackedLocation) =
        WeatherRefreshWorker.schedule(context, trackedLocation)

    override fun cancelLiftedCheck(trackedLocation: TrackedLocation) =
        WeatherRefreshWorker.cancel(context, trackedLocation)

    override fun cancelTrackingWorker(alertId: String) {
        workManager.cancelUniqueWork(alertId)
    }

    private companion object {
        const val TAG = "AlertTrackingController"
        const val CHECK_INTERVAL_MINUTES = 15L
    }
}