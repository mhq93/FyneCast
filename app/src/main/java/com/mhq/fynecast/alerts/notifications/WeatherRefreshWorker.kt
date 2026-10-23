package com.mhq.fynecast.alerts.notifications

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mhq.fynecast.alerts.notifications.data.infra.AlertsNotificationManager
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.core.domain.location.TrackedLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Background CoroutineWorker that periodically fetches weather updates
 * for monitored locations and triggers alarms if alerts are lifted.
 */
class WeatherRefreshWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val container =
        (context.applicationContext as FyneCastApplication).container

    private val alertsNotificationManager =
        AlertsNotificationManager(context)

    companion object {
        private const val TAG = "WeatherRefreshWorker"
        const val KEY_NAME = "name"
        const val KEY_LAT = "lat"
        const val KEY_LON = "lon"

        private fun workName(trackedLocation: TrackedLocation) =
            "weather_refresh_${trackedLocation.trackedLocationQuery}"

        /**
         * Schedules a unique periodic work request with network constraints
         * to check for lifted alerts every 15 minutes.
         */
        fun schedule(context: Context, trackedLocation: TrackedLocation) {
            val request = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(
                15,
                TimeUnit.MINUTES
            )
                .setInitialDelay(15, TimeUnit.MINUTES)
                .setInputData(
                    workDataOf(
                        KEY_NAME to trackedLocation.trackedLocationName,
                        KEY_LAT to trackedLocation.trackedLocationLatitude,
                        KEY_LON to trackedLocation.trackedLocationLongitude
                    )
                )
                .setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    workName(trackedLocation),
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        /**
         * Cancels unique periodic refresh work for a specific tracked location.
         */
        fun cancel(context: Context, trackedLocation: TrackedLocation) {
            WorkManager
                .getInstance(context)
                .cancelUniqueWork(
                    workName(
                        trackedLocation
                    )
                )
        }
    }

    /**
     * Executes the background refresh work:
     * fetches live weather data, evaluates alert conditions,
     * displays alarm notifications for cleared alerts, and untracks them.
     *
     * Edge case handling: Re-throws [CancellationException]
     * while catching other exceptions to request a retry.
     */
    override suspend fun doWork(): Result {
        val lat = inputData.getDouble(KEY_LAT, Double.NaN)
        val lon = inputData.getDouble(KEY_LON, Double.NaN)
        val name = inputData.getString(KEY_NAME)

        if (name == null || lat.isNaN() || lon.isNaN())
            return Result.failure()

        val location = TrackedLocation(
            name,
            lat,
            lon
        )

        return try {
            val prefs = container.observeUserPreferencesUseCase().first()
            val lang = prefs.currentLanguage.code
            val snapshot = container.getWeatherUseCase(location.trackedLocationQuery, lang)
            val cleared = container.checkAlertConditionsUseCase(location, snapshot)

            Log.d(
                TAG,
                "check ${location.trackedLocationQuery}: " +
                        "fetched=${snapshot.cityName} " +
                        "(${snapshot.latitude},${snapshot.longitude}) " +
                        "active=${snapshot.alerts.size} " +
                        "cleared=${cleared.size}"
            )
            if (cleared.isEmpty()) return Result.success()

            alertsNotificationManager.showAlarmNotification(
                clearedAlerts = cleared,
                notificationId =
                    cleared
                        .map { it.id }
                        .sorted()
                        .joinToString(",")
                        .hashCode()
            )
            cleared
                .forEach {
                    container.untrackAlertUseCase(it.id)
                }

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }
}