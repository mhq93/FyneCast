package com.mhq.fynecast.alerts.data.repoimpl

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertServiceController
import com.mhq.fynecast.alerts.notifications.AlertsForegroundService
import com.mhq.fynecast.alerts.notifications.WeatherAlertTrackingWorker
import com.mhq.fynecast.alerts.notifications.WeatherRefreshWorker
import java.util.concurrent.TimeUnit

class AlertServiceControllerImpl(
    private val context: Context
) : AlertServiceController {

    private val workManager
        get() = WorkManager.getInstance(context)

    // The OS may refuse this (background start rules); tracking still works via WorkManager.
    override fun startTracking(alertId: String, event: String, headline: String) {
        runCatching { AlertsForegroundService.start(context, alertId, event, headline) }
            .onFailure { Log.w(TAG, "Foreground service start refused", it) }
    }

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

    override fun removeAlertFromTracking(alertId: String) {
        runCatching { AlertsForegroundService.removeAlert(context, alertId) }
    }

    override fun cancelTrackingWorker(alertId: String) {
        workManager.cancelUniqueWork(alertId)
    }

    override fun stopTracking() { AlertsForegroundService.stop(context) }

    private companion object {
        const val TAG = "AlertServiceController"
        const val CHECK_INTERVAL_MINUTES = 15L
    }
}