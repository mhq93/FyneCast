package com.mhq.fynecast.alerts.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mhq.fynecast.BuildConfig
import com.mhq.fynecast.alerts.notifications.data.infra.AlertsNotificationManager
import com.mhq.fynecast.core.di.FyneCastApplication
import java.util.concurrent.TimeUnit

class WeatherAlertTrackingWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val appContainer = (context.applicationContext as FyneCastApplication).container
    private val notificationManager = AlertsNotificationManager(context)

    companion object {
        private const val TAG = "AlertTrackingWorker"
        const val KEY_ALERT_ID = "alert_id"
        const val KEY_EVENT = "event"
        const val KEY_HEADLINE = "headline"
        const val KEY_EXPIRES_MILLIS = "expires_millis"

        private val CHECK_INTERVAL_MILLIS = if (BuildConfig.DEBUG) 1L else 15L
        private val CHECK_INTERVAL_UNIT = TimeUnit.MINUTES
    }

    override suspend fun doWork(): Result {
        val alertId = inputData.getString(KEY_ALERT_ID)
        val event = inputData.getString(KEY_EVENT)
        val headline = inputData.getString(KEY_HEADLINE)
        val expiresMillis = inputData.getLong(KEY_EXPIRES_MILLIS, 0L)

        if (alertId == null || event == null || headline == null || expiresMillis == 0L) {
            return Result.failure()
        }

        return try {
            val currentTime = System.currentTimeMillis()
            val trackedAlerts = appContainer.alertsRepository.getTrackedAlertsDirectly()

            val isStillTracked = trackedAlerts.any { it.id == alertId }

            if (!isStillTracked) {
                AlertsForegroundService.removeAlert(applicationContext, alertId)
                return Result.success()
            }

            if (currentTime >= expiresMillis) {
                val matchedAlert = trackedAlerts.firstOrNull { it.id == alertId }
                if (matchedAlert != null) {
                    notificationManager.showAlarmNotification(listOf(matchedAlert))
                }

                AlertsForegroundService.removeAlert(applicationContext, alertId)
                appContainer.alertsRepository.cancelTracking(alertId, cancelWorker = false)

                return Result.success()
            } else {
                val remainingSeconds = (expiresMillis - currentTime) / 1000
                val remainingMinutes = (remainingSeconds / 60).coerceAtLeast(1)
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}