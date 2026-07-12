package com.mhq.fynecast.alerts.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mhq.fynecast.BuildConfig
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val appContainer = (context.applicationContext as FyneCastApplication).container

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val trackedAlerts = appContainer.alertsRepository.getTrackedAlertsDirectly()

                if (trackedAlerts.isEmpty()) {
                    return@launch
                }

                val currentTime = System.currentTimeMillis()

                trackedAlerts.forEach { entity ->
                    if (currentTime >= entity.expiresTimeMillis) {
                        appContainer.alertsRepository.cancelTracking(entity.id, cancelWorker = true)
                    } else {

                        AlertsForegroundService.start(
                            context = context,
                            alertId = entity.id,
                            event = entity.event,
                            headline = entity.event
                        )

                        val checkInterval = if (BuildConfig.DEBUG) 1L else 15L
                        val inputData = workDataOf(
                            WeatherAlertTrackingWorker.KEY_ALERT_ID to entity.id,
                            WeatherAlertTrackingWorker.KEY_EVENT to entity.event,
                            WeatherAlertTrackingWorker.KEY_HEADLINE to entity.event,
                            WeatherAlertTrackingWorker.KEY_EXPIRES_MILLIS to entity.expiresTimeMillis
                        )

                        val periodicRequest =
                            PeriodicWorkRequestBuilder<WeatherAlertTrackingWorker>(
                                checkInterval, TimeUnit.MINUTES
                            )
                                .setInputData(inputData)
                                .setInitialDelay(checkInterval, TimeUnit.MINUTES)
                                .build()

                        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                            entity.id,
                            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                            periodicRequest
                        )

                    }
                }
            } catch (e: Exception) {

            } finally {
                pendingResult.finish()
            }
        }
    }
}