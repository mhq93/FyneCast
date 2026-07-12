package com.mhq.fynecast.alerts.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mhq.fynecast.alerts.domain.usecases.CheckAlertConditionsUseCase
import com.mhq.fynecast.alerts.notifications.data.infra.AlertsNotificationManager
import com.mhq.fynecast.core.di.FyneCastApplication
import java.util.concurrent.TimeUnit

class WeatherRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val appContainer = (context.applicationContext as FyneCastApplication).container
    private val notificationManager = AlertsNotificationManager(context)
    private val checkAlertConditionsUseCase =
        CheckAlertConditionsUseCase(appContainer.alertsRepository)

    companion object {
        const val KEY_CITY = "city"
        const val KEY_LANG = "lang"
        const val WORK_NAME = "weather_refresh_work"

        fun schedule(context: Context, city: String, lang: String) {
            val inputData = workDataOf(
                KEY_CITY to city,
                KEY_LANG to lang
            )

            val request = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(
                15, TimeUnit.MINUTES
            )
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        val city = inputData.getString(KEY_CITY) ?: return Result.failure()
        val lang = inputData.getString(KEY_LANG) ?: return Result.failure()

        return try {
            val freshResponse = appContainer.weatherRepository.provideWeatherData(city, lang)
            val activeAlerts = freshResponse.alerts
            val clearedAlerts = checkAlertConditionsUseCase(activeAlerts = activeAlerts)

            if (clearedAlerts.isEmpty()) {
                return Result.success()
            }

            notificationManager.showAlarmNotification(clearedAlerts = clearedAlerts)

            clearedAlerts.forEach { alert ->
                appContainer.alertsRepository.cancelTracking(alert.id, cancelWorker = false)
                AlertsForegroundService.removeAlert(applicationContext, alert.id)
            }

            val remainingTracked = appContainer.alertsRepository.getTrackedAlertsDirectly()
            if (remainingTracked.isEmpty()) {
                AlertsForegroundService.stop(applicationContext)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}