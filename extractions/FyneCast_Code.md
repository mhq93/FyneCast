# Files under `app/src/main/java/com/mhq/fynecast`

## alerts\data\database\AlertDao.kt

```kotlin
package com.mhq.fynecast.alerts.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing persistent tracked weather alerts in the Alerts database.
 */
@Dao
interface AlertDao {
    /** Retrieves all currently tracked weather alerts as a one-shot list. */
    @Query("SELECT * FROM alerts")
    suspend fun getAllTrackedAlerts(): List<TrackedAlertEntity>

    /** Observes a live stream of all tracked alert identifier strings. */
    @Query("SELECT id FROM alerts")
    fun getTrackedAlertIds(): Flow<List<String>>

    /** Inserts or updates a tracked alert in local storage, replacing conflicts. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackedAlert(alert: TrackedAlertEntity)

    /** Deletes a specific tracked alert by its unique identifier (ID). */
    @Query("DELETE FROM alerts WHERE id = :alertId")
    suspend fun deleteTrackedAlertById(alertId: String)

    /** Clears all tracked weather alerts from the Alerts database. */
    @Query("DELETE FROM alerts")
    suspend fun deleteAllTrackedAlerts()
}
```

## alerts\data\database\AlertsDatabase.kt

```kotlin
package com.mhq.fynecast.alerts.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database abstract class serving as the main access point for the tracked alerts persistence layer.
 */
@Database(
    entities = [TrackedAlertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AlertsDatabase : RoomDatabase() {
    /** Provides access to the [AlertDao] data operations. */
    abstract fun alertDao(): AlertDao
}
```

## alerts\data\database\TrackedAlertEntity.kt

```kotlin
package com.mhq.fynecast.alerts.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity representing a weather alert that the user has chosen to track.
 */
@Entity(tableName = "alerts")
data class TrackedAlertEntity(
    @PrimaryKey
    val id: String,
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val headline: String,
    val event: String,
    val effectiveTimeMillis: Long,
    val expiresTimeMillis: Long
)
```

## alerts\data\datasource\AlertsLocalDataSource.kt

```kotlin
package com.mhq.fynecast.alerts.data.datasource

import com.mhq.fynecast.alerts.data.database.TrackedAlertEntity
import kotlinx.coroutines.flow.Flow

/** Where tracked alerts are stored on the phone. Works with database entities, never domain models. */
interface AlertsLocalDataSource {
    fun observeTrackedAlertIds(): Flow<List<String>>
    suspend fun getAll(): List<TrackedAlertEntity>
    suspend fun insert(alert: TrackedAlertEntity)
    suspend fun deleteById(alertId: String)
    suspend fun deleteAll()
}
```

## alerts\data\datasource\AlertsRoomDataSource.kt

```kotlin
package com.mhq.fynecast.alerts.data.datasource

import com.mhq.fynecast.alerts.data.database.AlertDao
import com.mhq.fynecast.alerts.data.database.TrackedAlertEntity
import kotlinx.coroutines.flow.Flow

class AlertsRoomDataSource(
    private val alertDao: AlertDao
) : AlertsLocalDataSource {

    override fun observeTrackedAlertIds(): Flow<List<String>> = alertDao.getTrackedAlertIds()

    override suspend fun getAll(): List<TrackedAlertEntity> = alertDao.getAllTrackedAlerts()

    override suspend fun insert(alert: TrackedAlertEntity) = alertDao.insertTrackedAlert(alert)

    override suspend fun deleteById(alertId: String) = alertDao.deleteTrackedAlertById(alertId)

    override suspend fun deleteAll() = alertDao.deleteAllTrackedAlerts()
}
```

## alerts\data\mapper\AlertsMapper.kt

```kotlin
package com.mhq.fynecast.alerts.data.mapper

import com.mhq.fynecast.alerts.data.database.TrackedAlertEntity
import com.mhq.fynecast.core.domain.weather.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.core.domain.location.TrackedLocation

/**
 * Converts an [AlertDomainModel] and its associated [TrackedLocation] into a Room database persistence entity.
 *
 * Throws an [IllegalArgumentException] if [AlertDomainModel.expiresMillis] is null, enforcing the business rule that only alerts with a known expiry can be tracked.
 */
fun AlertDomainModel.toEntity(location: TrackedLocation) =
    TrackedAlertEntity(
        id = id,
        cityName = location.trackedLocationName,
        latitude = location.trackedLocationLatitude,
        longitude = location.trackedLocationLongitude,
        event = event,
        headline = headline,
        effectiveTimeMillis = effectiveMillis,
        expiresTimeMillis = requireNotNull(expiresMillis) {
            "Only alerts with a known expiry can be tracked"
        }
    )

/**
 * Maps a Room database entity [TrackedAlertEntity] back to a domain model [TrackedAlertDomainModel].
 */
fun TrackedAlertEntity.toDomain() =
    TrackedAlertDomainModel(
        id = id,
        cityName = cityName,
        latitude = latitude,
        longitude = longitude,
        headline = headline,
        event = event,
        effectiveTimeMillis = effectiveTimeMillis,
        expiresTimeMillis = expiresTimeMillis
    )
```

## alerts\data\repoimpl\AlertsRepoImpl.kt

```kotlin
package com.mhq.fynecast.alerts.data.repoimpl

import com.mhq.fynecast.alerts.data.datasource.AlertsLocalDataSource
import com.mhq.fynecast.alerts.data.mapper.toDomain
import com.mhq.fynecast.alerts.data.mapper.toEntity
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.core.domain.location.TrackedLocation
import com.mhq.fynecast.core.domain.weather.AlertDomainModel

/**
 * Concrete implementation of the [AlertsRepository] on top of a local data source.
 */
class AlertsRepoImpl(private val localDataSource: AlertsLocalDataSource) : AlertsRepository {
    override fun getTrackedAlertIds() =
        localDataSource.observeTrackedAlertIds()

    override suspend fun getTrackedAlerts() =
        localDataSource.getAll().map { it.toDomain() }

    override suspend fun saveTrackedAlert(alert: AlertDomainModel, location: TrackedLocation) =
        localDataSource.insert(alert.toEntity(location))

    override suspend fun deleteTrackedAlert(alertId: String) =
        localDataSource.deleteById(alertId)

    override suspend fun deleteAllTrackedAlerts() =
        localDataSource.deleteAll()
}
```

## alerts\data\repoimpl\AlertTrackingControllerImpl.kt

```kotlin
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
```

## alerts\domain\models\TrackedAlertDomainModel.kt

```kotlin
package com.mhq.fynecast.alerts.domain.models

import com.mhq.fynecast.core.domain.location.TrackedLocation

/**
 * Domain model representing a weather alert that is currently being tracked for a specific location.
 */
data class TrackedAlertDomainModel(
    val id: String,
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val headline: String,
    val event: String,
    val effectiveTimeMillis: Long,
    val expiresTimeMillis: Long
) {
    /** Derives a [com.mhq.fynecast.core.domain.location.TrackedLocation] instance from the alert's location properties. */
    val location: TrackedLocation
        get() = TrackedLocation(
            cityName,
            latitude,
            longitude
        )
}
```

## alerts\domain\repository\AlertsRepository.kt

```kotlin
package com.mhq.fynecast.alerts.domain.repository

import com.mhq.fynecast.core.domain.weather.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.core.domain.location.TrackedLocation
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining data operations for weather alerts and tracked items.
 */
interface AlertsRepository {
    /** Observes a flow of IDs for all tracked alerts. */
    fun getTrackedAlertIds(): Flow<List<String>>

    /** Retrieves a list of all currently tracked alerts. */
    suspend fun getTrackedAlerts(): List<TrackedAlertDomainModel>

    /** Saves an alert along with its location for tracking. */
    suspend fun saveTrackedAlert(alert: AlertDomainModel, location: TrackedLocation)

    /** Removes a tracked alert by its unique identifier. */
    suspend fun deleteTrackedAlert(alertId: String)

    /** Deletes all tracked alerts from storage. */
    suspend fun deleteAllTrackedAlerts()
}
```

## alerts\domain\repository\AlertTrackingController.kt

```kotlin
package com.mhq.fynecast.alerts.domain.repository

import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.core.domain.location.TrackedLocation

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
```

## alerts\domain\usecases\CancelAllAlertTrackingUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Use case that tears down all alert tracking workers, clears saved alerts, and dismisses notifications.
 */
class CancelAllAlertTrackingUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertTrackingController: AlertTrackingController
) {
    /** Executes cleanup safely within a non-cancellable context. */
    suspend operator fun invoke() = withContext(NonCancellable) {
        val tracked = alertsRepository.getTrackedAlerts()
        tracked.forEach { alertTrackingController.cancelTrackingWorker(it.id) }
        tracked.map { it.location }.distinct().forEach { alertTrackingController.cancelLiftedCheck(it) }
        alertsRepository.deleteAllTrackedAlerts()
        alertTrackingController.showTrackingNotification(emptyList())
    }
}
```

## alerts\domain\usecases\CheckAlertConditionsUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.core.domain.location.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.core.domain.time.TimeProvider
import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import kotlin.math.abs

/**
 * Use case that evaluates weather snapshots against tracked alerts to determine which alerts have expired or been lifted.
 */
class CheckAlertConditionsUseCase(
    private val alertsRepository: AlertsRepository,
    private val timeProvider: TimeProvider
) {
    /**
     * Evaluates whether any tracked alerts for the given [trackedLocation] have expired or been cleared based on the [weatherSnapshot].
     *
     * Edge case: If [weatherSnapshot] is `null` or corresponds to a different location than [trackedLocation], it is treated as "unknown"—meaning only expiration timestamps are evaluated, preventing false-positive lifted alerts.
     *
     * @param trackedLocation The location whose tracked alerts are being verified.
     * @param weatherSnapshot The latest fetched weather model containing active alerts, or `null`.
     * @return A list of [TrackedAlertDomainModel] instances that are either past their expiry time or missing from the current active weather payload.
     */
    suspend operator fun invoke(
        trackedLocation: TrackedLocation,
        weatherSnapshot: WeatherDomainModel?
    ): List<TrackedAlertDomainModel> {
        val now = timeProvider.nowMillis()
        val activeIds = weatherSnapshot
            ?.takeIf { it.isNear(trackedLocation) }
            ?.alerts
            ?.mapTo(HashSet()) { it.id }

        return alertsRepository.getTrackedAlerts()
            .filter {
                it.latitude == trackedLocation.trackedLocationLatitude &&
                        it.longitude == trackedLocation.trackedLocationLongitude
            }
            .filter { now >= it.expiresTimeMillis || (activeIds != null && it.id !in activeIds) }
    }

    /**
     * Validates whether a weather snapshot's coordinates fall within acceptable proximity tolerance of the tracked location.
     */
    private fun WeatherDomainModel.isNear(location: TrackedLocation) =
        abs(latitude - location.trackedLocationLatitude) < TOLERANCE_DEGREES &&
                abs(longitude - location.trackedLocationLongitude) < TOLERANCE_DEGREES

    private companion object {
        const val TOLERANCE_DEGREES = 0.1
    }
}
```

## alerts\domain\usecases\GetAlertsUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.core.domain.weather.AlertDomainModel
import com.mhq.fynecast.core.domain.weather.CurrentWeatherStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case that streams current weather alerts from the weather store.
 */
class GetAlertsUseCase(private val currentWeatherStore: CurrentWeatherStore) {
    /** Returns a flow of active weather alert lists, or null if unavailable. */
    operator fun invoke(): Flow<List<AlertDomainModel>?> =
        currentWeatherStore.current.map { it?.alerts }
}
```

## alerts\domain\usecases\GetTrackedAlertIdsUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for observing IDs of all currently tracked alerts.
 */
class GetTrackedAlertIdsUseCase(private val alertsRepository: AlertsRepository) {
    /** Returns a flow emitting lists of tracked alert ID strings. */
    operator fun invoke(): Flow<List<String>> =
        alertsRepository.getTrackedAlertIds()
}
```

## alerts\domain\usecases\GetTrackedAlertStatusUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.core.domain.time.TimeProvider

/**
 * Use case that evaluates whether a specific tracked alert is active, expired, or untracked.
 */
class GetTrackedAlertStatusUseCase(
    private val alertsRepository: AlertsRepository,
    private val timeProvider: TimeProvider
) {
    /** Represents the current tracked state of an alert. */
    sealed interface Status {
        data object NotTracked : Status
        data object Active : Status
        data class Expired(val alert: TrackedAlertDomainModel) : Status
    }

    /** Checks expiry time against the current time provider. */
    suspend operator fun invoke(alertId: String): Status {
        val alert = alertsRepository.getTrackedAlerts().firstOrNull { it.id == alertId }
            ?: return Status.NotTracked

        return if (timeProvider.nowMillis() >= alert.expiresTimeMillis)
            Status.Expired(alert)
        else Status.Active
    }
}
```

## alerts\domain\usecases\RefreshTrackingNotificationUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository

class RefreshTrackingNotificationUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertTrackingController: AlertTrackingController
) {
    /** Refreshes the ongoing tracking notification with current tracked alerts. */
    suspend operator fun invoke() =
        alertTrackingController.showTrackingNotification(alertsRepository.getTrackedAlerts())
}
```

## alerts\domain\usecases\RestoreAlertTrackingUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.core.domain.time.TimeProvider

/**
 * Use case used upon device reboot or app startup to restore active alert tracking workers and prune expired items.
 */
class RestoreAlertTrackingUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertTrackingController: AlertTrackingController,
    private val untrackAlertUseCase: UntrackAlertUseCase,
    private val timeProvider: TimeProvider
) {
    /**
     * Partitions stored tracked alerts into expired versus live sets against the current time.
     * Expired alerts are immediately untracked and removed, while live alerts have their tracking workers and location refresh checks rescheduled.
     */
    suspend operator fun invoke() {
        val now = timeProvider.nowMillis()
        val (expired, live) = alertsRepository.getTrackedAlerts()
            .partition { now >= it.expiresTimeMillis }
        expired.forEach { untrackAlertUseCase(it.id) }
        live.forEach { alertTrackingController.scheduleAlertTracking(it.id) }
        live.map { it.location }.distinct()
            .forEach { alertTrackingController.scheduleLiftedCheck(it) }
        alertTrackingController.showTrackingNotification(live)
    }
}
```

## alerts\domain\usecases\ToggleAlertsUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.core.domain.weather.AlertDomainModel
import com.mhq.fynecast.core.domain.location.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.first

/**
 * Use case for switching an alert between tracked and untracked states.
 */
class ToggleAlertsUseCase(
    private val alertsRepository: AlertsRepository,
    private val trackAlertUseCase: TrackAlertUseCase,
    private val untrackAlertUseCase: UntrackAlertUseCase
) {
    /** Result states of a toggle operation. */
    sealed interface Result {
        data object Tracked : Result
        data object Untracked : Result
        data object AlreadyExpired : Result
        data object ExpiryUnknown : Result
        data object NotificationsRequired : Result
    }

    /** Toggles tracking on or off depending on whether the alert is already tracked. */
    suspend operator fun invoke(alert: AlertDomainModel, trackedLocation: TrackedLocation): Result {
        if (alert.id in alertsRepository.getTrackedAlertIds().first()) {
            untrackAlertUseCase(alert.id)
            return Result.Untracked
        }
        return when (trackAlertUseCase(alert, trackedLocation)) {
            TrackAlertUseCase.Result.Tracked -> Result.Tracked
            TrackAlertUseCase.Result.AlreadyExpired -> Result.AlreadyExpired
            TrackAlertUseCase.Result.ExpiryUnknown -> Result.ExpiryUnknown
            TrackAlertUseCase.Result.NotificationsRequired -> Result.NotificationsRequired
        }
    }
}
```

## alerts\domain\usecases\TrackAlertUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.core.domain.weather.AlertDomainModel
import com.mhq.fynecast.core.domain.location.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.core.domain.time.TimeProvider
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import kotlinx.coroutines.flow.first

/**
 * Use case responsible for validating expiration criteria, permission status, and initializing background tracking for a weather alert.
 */
class TrackAlertUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertTrackingController: AlertTrackingController,
    private val timeProvider: TimeProvider,
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase
) {
    /** Possible outcome states resulting from a tracking attempt. */
    sealed interface Result {
        data object Tracked : Result
        data object AlreadyExpired : Result
        data object ExpiryUnknown : Result
        data object NotificationsRequired : Result
    }

    /**
     * Attempts to track an [alert] at a specified [trackedLocation].
     *
     * Execution flow & edge cases:
     * 1. Returns [Result.ExpiryUnknown] if the alert lacks an expiration timestamp.
     * 2. Returns [Result.AlreadyExpired] if the expiration time has already passed.
     * 3. Returns [Result.NotificationsRequired] if notifications are disabled in user preferences or system permissions are not granted.
     * 4. Otherwise, saves the alert to the repository, shows tracking notifications, and schedules background worker tasks.
     */
    suspend operator fun invoke(alert: AlertDomainModel, trackedLocation: TrackedLocation): Result {
        val expiresMillis = alert.expiresMillis ?: return Result.ExpiryUnknown
        if (expiresMillis <= timeProvider.nowMillis()) return Result.AlreadyExpired

        val enabled = observeUserPreferencesUseCase().first().notificationsEnabled
        if (!enabled || !notificationPermissionChecker.isGranted()) return Result.NotificationsRequired

        alertsRepository.saveTrackedAlert(alert, trackedLocation)
        alertTrackingController.showTrackingNotification(alertsRepository.getTrackedAlerts())
        alertTrackingController.scheduleAlertTracking(alert.id)
        alertTrackingController.scheduleLiftedCheck(trackedLocation)
        return Result.Tracked
    }
}
```

## alerts\domain\usecases\UntrackAlertUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository

/**
 * Use case for stopping the tracking of a specific alert and cleaning up associated workers.
 */
class UntrackAlertUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertTrackingController: AlertTrackingController
) {
    suspend operator fun invoke(alertId: String) {
        val removed = alertsRepository.getTrackedAlerts().firstOrNull { it.id == alertId }
        alertsRepository.deleteTrackedAlert(alertId)

        val remainingAlerts = alertsRepository.getTrackedAlerts()

        alertTrackingController.showTrackingNotification(remainingAlerts)
        if (removed != null && remainingAlerts.none {
                it.latitude == removed.latitude && it.longitude == removed.longitude
            }) {
            alertTrackingController.cancelLiftedCheck(removed.location)
        }

        alertTrackingController.cancelTrackingWorker(alertId)
    }
}
```

## alerts\notifications\AlertTrackingReceiver.kt

```kotlin
package com.mhq.fynecast.alerts.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Broadcast receiver handling actions related to alert tracking notifications (e.g. cancel all, dismiss).
 */
class AlertTrackingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as FyneCastApplication).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_CANCEL_ALL -> container.cancelAllAlertTrackingUseCase()
                    ACTION_TRACKING_DISMISSED -> container.refreshTrackingNotificationUseCase()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle ${intent.action}", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AlertTrackingReceiver"
        const val ACTION_CANCEL_ALL = "com.mhq.fynecast.action.ALERT_CANCEL_ALL"
        const val ACTION_TRACKING_DISMISSED = "com.mhq.fynecast.action.TRACKING_DISMISSED"
    }
}
```

## alerts\notifications\BootReceiver.kt

```kotlin
package com.mhq.fynecast.alerts.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Broadcast receiver that listens for device boot completion to restore active alert tracking.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val container = (context.applicationContext as FyneCastApplication).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.restoreAlertTrackingUseCase()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore alert tracking after boot", e)
            } finally {
                pending.finish()
            }
        }
    }
    private companion object {
        const val TAG = "BootReceiver"
    }
}
```

## alerts\notifications\data\infra\AlertsNotificationManager.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.data.infra

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.mhq.fynecast.R
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.notifications.AlertTrackingReceiver
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.core.ui.MainActivity
import com.mhq.fynecast.core.ui.withAppLocale
import kotlin.jvm.java

/**
 * Manages system notifications for tracked weather alerts and lifted warning alarms.
 */
class AlertsNotificationManager(baseContext: Context) {

    private val context: Context = baseContext.withAppLocale()
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    /** Displays a high-priority alarm notification when a tracked weather alert is lifted or expired. */
    fun showAlarmNotification(
        clearedAlerts: List<TrackedAlertDomainModel>,
        notificationId: Int
    ) {
        val title = if (clearedAlerts.size == 1) {
            context.getString(
                R.string.alert_lifted_title_single,
                clearedAlerts.first().event
            )
        } else {
            context.getString(
                R.string.alert_lifted_title_multiple,
                clearedAlerts.size
            )
        }

        val body = if (clearedAlerts.size == 1) {
            context.getString(
                R.string.alert_lifted_body_single,
                clearedAlerts.first().event,
                clearedAlerts.first().cityName
            )
        } else {
            context.getString(
                R.string.alert_lifted_body_multiple,
                clearedAlerts.joinToString(", ") { it.event })
        }

        val notification = NotificationCompat
            .Builder(context, FyneCastApplication.CHANNEL_ALERT_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /** Displays or updates the persistent ongoing tracking notification listing active tracked alerts. */
    fun showTrackingNotification(alerts: List<TrackedAlertDomainModel>) {
        if (alerts.isEmpty()) {
            cancelTrackingNotification()
            return
        }
        val title = if (alerts.size == 1)
            context.getString(
                R.string.alert_tracking_title_single,
                alerts.first().event
            )
        else
            context.getString(
                R.string.alert_tracking_title_multiple,
                alerts.size
            )

        val clickPendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification =
            NotificationCompat.Builder(context, FyneCastApplication.CHANNEL_ALERT_TRACKING)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(
                    if (alerts.size == 1) alerts.first().headline
                    else context.getString(R.string.alert_tracking_text_multiple)
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(alerts.joinToString("\n") { "• ${it.headline}" })
                )
                .setContentIntent(clickPendingIntent)
                .setDeleteIntent(
                    receiverIntent(
                        AlertTrackingReceiver.ACTION_TRACKING_DISMISSED,
                        requestCode = 1
                    )
                )
                .addAction(
                    R.drawable.ic_cancel,
                    context.getString(R.string.cancel_tracking),
                    receiverIntent(AlertTrackingReceiver.ACTION_CANCEL_ALL, requestCode = 2)
                )
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()

        notificationManager.notify(TRACKING_NOTIFICATION_ID, notification)
    }

    /** Cancels the persistent tracking notification. */
    fun cancelTrackingNotification() = notificationManager.cancel(TRACKING_NOTIFICATION_ID)

    /** Helper method to build broadcast intents for notification actions. */
    private fun receiverIntent(action: String, requestCode: Int) = PendingIntent.getBroadcast(
        context, requestCode,
        Intent(context, AlertTrackingReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    companion object {
        const val TRACKING_NOTIFICATION_ID = 1001
    }
}
```

## alerts\notifications\data\infra\AndroidNotificationPermissionChecker.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.data.infra

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker

/**
 * Android-specific implementation of [NotificationPermissionChecker] using runtime permission checks.
 */
class AndroidNotificationPermissionChecker(
    private val context: Context
) : NotificationPermissionChecker {
    override fun isGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
```

## alerts\notifications\domain\infra\NotificationPermissionChecker.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.domain.infra

/**
 * Interface for checking whether notification permissions have been granted.
 */
interface NotificationPermissionChecker {
    fun isGranted(): Boolean
}
```

## alerts\notifications\domain\usecases\ConsumeNotificationPromptUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

/**
 * Use case to determine if the permission prompt dialog should be shown to the user.
 */
class ConsumeNotificationPromptUseCase(
    private val permissionChecker: NotificationPermissionChecker,
    private val preferencesRepository: UserPreferencesRepository
) {
    /** @return true only the first time the app should show the permission dialog. */
    suspend operator fun invoke(): Boolean {
        if (permissionChecker.isGranted()) return false
        return preferencesRepository.consumeNotificationPrompt()
    }
}
```

## alerts\notifications\domain\usecases\HandleNotificationPermissionResultUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.settings.domain.usecases.profile.SetNotificationsEnabledUseCase

/**
 * Use case that saves the user's response to the notification permission request.
 */
class HandleNotificationPermissionResultUseCase(
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase
) {
    suspend operator fun invoke(granted: Boolean) {
        setNotificationsEnabledUseCase(granted)
    }
}
```

## alerts\notifications\domain\usecases\SyncNotificationPermissionUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.alerts.domain.usecases.CancelAllAlertTrackingUseCase
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetNotificationsEnabledUseCase
import kotlinx.coroutines.flow.first

/**
 * Use case that synchronizes notification preferences with actual system runtime permissions.
 */
class SyncNotificationPermissionUseCase(
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val cancelAllAlertTrackingUseCase: CancelAllAlertTrackingUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val notificationPermissionChecker: NotificationPermissionChecker
) {
    suspend operator fun invoke() {
        val current = observeUserPreferencesUseCase().first()
        val systemGranted = notificationPermissionChecker.isGranted()

        if (current.notificationsEnabled && !systemGranted) {
            /** System permission was revoked: turn tracking off locally */
            cancelAllAlertTrackingUseCase()
            setNotificationsEnabledUseCase(false)
        } else if (!current.notificationsEnabled && systemGranted) {
            /** System permission was granted from Settings:
             * sync the local preference toggle to on */
            setNotificationsEnabledUseCase(true)
        }
    }
}
```

## alerts\notifications\domain\usecases\ToggleNotificationsUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.alerts.domain.usecases.CancelAllAlertTrackingUseCase
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.settings.domain.usecases.profile.SetNotificationsEnabledUseCase

/**
 * Use case for enabling or disabling notifications, handling permissions and tracker cleanup.
 */
class ToggleNotificationsUseCase(
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val cancelAllAlertTrackingUseCase: CancelAllAlertTrackingUseCase
) {
    sealed interface Result {
        data object Enabled : Result
        data object Disabled : Result
        data object PermissionRequired : Result
    }

    suspend operator fun invoke(wantsEnabled: Boolean): Result {
        if (!wantsEnabled) {
            // Tear tracking down first. If this fails, the preference stays "enabled",
            // so the UI never claims notifications are off while workers still run.
            cancelAllAlertTrackingUseCase()
            setNotificationsEnabledUseCase(false)
            return Result.Disabled
        }

        return if (notificationPermissionChecker.isGranted()) {
            setNotificationsEnabledUseCase(true)
            Result.Enabled
        } else {
            Result.PermissionRequired
        }
    }
}
```

## alerts\notifications\WeatherAlertTrackingWorker.kt

```kotlin
package com.mhq.fynecast.alerts.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertStatusUseCase
import com.mhq.fynecast.alerts.notifications.data.infra.AlertsNotificationManager
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.CancellationException

/**
 * Background worker that periodically checks the status of a specific tracked alert.
 */
class WeatherAlertTrackingWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val container = (context.applicationContext as FyneCastApplication).container
    private val alertsNotificationManager = AlertsNotificationManager(context)

    companion object { const val KEY_ALERT_ID = "alert_id" }

    override suspend fun doWork(): Result {
        val alertId = inputData.getString(KEY_ALERT_ID) ?: return Result.failure()

        return try {
            when (val status = container.getTrackedAlertStatusUseCase(alertId)) {
                GetTrackedAlertStatusUseCase.Status.Active -> Unit

                GetTrackedAlertStatusUseCase.Status.NotTracked ->
                    container.untrackAlertUseCase(alertId)

                is GetTrackedAlertStatusUseCase.Status.Expired -> {
                    alertsNotificationManager.showAlarmNotification(
                        clearedAlerts = listOf(status.alert),
                        notificationId = alertId.hashCode()
                    )
                    container.untrackAlertUseCase(alertId)
                }
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

## alerts\notifications\WeatherRefreshWorker.kt

```kotlin
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
```

## alerts\presentation\AlertsEvent.kt

```kotlin
package com.mhq.fynecast.alerts.presentation

/**
 * One-off UI events emitted by [AlertsViewModel] for handling side effects like snackbars or permissions.
 */
sealed interface AlertsEvent {
    data object AlertAlreadyExpired : AlertsEvent
    data object AlertExpiryUnknown : AlertsEvent
    data class AlertTrackingStarted(val eventName: String) : AlertsEvent
    data object RequestNotificationPermission : AlertsEvent
    data object NotificationPermissionDenied : AlertsEvent
}
```

## alerts\presentation\AlertsUiState.kt

```kotlin
package com.mhq.fynecast.alerts.presentation

import com.mhq.fynecast.alerts.presentation.model.AlertPresentationModel

/**
 * Represents the UI state of the alerts screen.
 */
sealed interface AlertsUiState {
    object Loading : AlertsUiState
    object Unavailable : AlertsUiState
    data class Success(val alerts: List<AlertPresentationModel>) : AlertsUiState
}
```

## alerts\presentation\AlertsViewModel.kt

```kotlin
package com.mhq.fynecast.alerts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.alerts.domain.usecases.GetAlertsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertIdsUseCase
import com.mhq.fynecast.alerts.domain.usecases.ToggleAlertsUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.alerts.presentation.mapper.toPresentation
import com.mhq.fynecast.core.domain.location.TrackedLocation
import com.mhq.fynecast.core.domain.weather.AlertDomainModel
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentLocationUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel managing state, user actions, permission flows, and event channels for the alerts feature.
 */
class AlertsViewModel(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val toggleAlertsUseCase: ToggleAlertsUseCase,
    private val getTrackedAlertIdsUseCase: GetTrackedAlertIdsUseCase,
    private val observeCurrentLocationUseCase: ObserveCurrentLocationUseCase,
    private val toggleNotificationsUseCase: ToggleNotificationsUseCase,
    private val handleNotificationPermissionResultUseCase: HandleNotificationPermissionResultUseCase
) : ViewModel() {

    /** The domain alerts, kept privately: the screen only ever sees presentation models. */
    private val domainAlerts: StateFlow<List<AlertDomainModel>?> = getAlertsUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Exposes reactive UI state for alerts, transitioning between loading and success states. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AlertsUiState> = domainAlerts
        .transformLatest { alerts ->
            if (alerts != null) {
                emit(AlertsUiState.Success(alerts.map { it.toPresentation() }))
            } else {
                emit(AlertsUiState.Loading)
                delay(LOAD_TIMEOUT_MILLIS.milliseconds)
                emit(AlertsUiState.Unavailable)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            AlertsUiState.Loading
        )

    /** Stream of IDs representing currently tracked alerts. */
    val trackedAlertIds: StateFlow<List<String>> = getTrackedAlertIdsUseCase()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    /** Stream tracking the user's current location context. */
    private val currentLocation: StateFlow<TrackedLocation?> =
        observeCurrentLocationUseCase()
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                null
            )

    private var pendingAlert: AlertDomainModel? = null

    private val _showNotificationsPrompt = MutableStateFlow(false)
    val showNotificationsPrompt: StateFlow<Boolean> = _showNotificationsPrompt.asStateFlow()

    private val _events = Channel<AlertsEvent>(Channel.BUFFERED)
    val events: Flow<AlertsEvent> = _events.receiveAsFlow()

    /**
     * Triggered when the user clicks the tracking toggle on an alert card.
     */
    fun onAlertTrackToggled(alertId: String) {
        val alert = domainAlerts.value?.firstOrNull { it.id == alertId } ?: return
        viewModelScope.launch { toggle(alert) }
    }

    /**
     * Called when the user confirms the notification prompt dialog.
     *
     * Attempts to enable notifications; if system permission is required, emits an event to request it.
     */
    fun onNotificationsPromptConfirmed() {
        _showNotificationsPrompt.value = false
        viewModelScope.launch {
            when (toggleNotificationsUseCase(wantsEnabled = true)) {
                ToggleNotificationsUseCase.Result.Enabled -> trackPendingAlert()
                ToggleNotificationsUseCase.Result.PermissionRequired ->
                    _events.send(AlertsEvent.RequestNotificationPermission)

                ToggleNotificationsUseCase.Result.Disabled -> Unit
            }
        }
    }

    /**
     * Dismisses the notification prompt dialog and clears any pending alert reference.
     */
    fun onNotificationsPromptDismissed() {
        _showNotificationsPrompt.value = false
        pendingAlert = null
    }

    /**
     * Processes the runtime permission result for notifications.
     *
     * If granted, resumes tracking the pending alert; if denied, emits a denial event.
     */
    fun onNotificationPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            handleNotificationPermissionResultUseCase(granted)
            if (granted) {
                trackPendingAlert()
            } else {
                pendingAlert = null
                _events.send(AlertsEvent.NotificationPermissionDenied)
            }
        }
    }

    /** Resumes tracking for an alert that was held pending notification permission. */
    private suspend fun trackPendingAlert() {
        val alert = pendingAlert ?: return
        pendingAlert = null
        toggle(alert)
    }

    /** Executes toggle logic and handles resulting states (e.g. triggering permission dialogs or emitting snackbar events). */
    private suspend fun toggle(alert: AlertDomainModel) {
        val location = currentLocation.value ?: return
        when (toggleAlertsUseCase(alert, location)) {
            ToggleAlertsUseCase.Result.Tracked -> _events.send(AlertsEvent.AlertTrackingStarted(alert.event))
            ToggleAlertsUseCase.Result.AlreadyExpired -> _events.send(AlertsEvent.AlertAlreadyExpired)
            ToggleAlertsUseCase.Result.ExpiryUnknown -> _events.send(AlertsEvent.AlertExpiryUnknown)
            ToggleAlertsUseCase.Result.NotificationsRequired -> {
                pendingAlert = alert
                _showNotificationsPrompt.value = true
            }

            ToggleAlertsUseCase.Result.Untracked -> Unit
        }
    }

    companion object {
        private const val LOAD_TIMEOUT_MILLIS = 20_000L
    }
}
```

## alerts\presentation\mapper\AlertPresentationMapper.kt

```kotlin
package com.mhq.fynecast.alerts.presentation.mapper

import com.mhq.fynecast.alerts.presentation.model.AlertPresentationModel
import com.mhq.fynecast.alerts.presentation.model.AlertSeverity
import com.mhq.fynecast.core.domain.weather.AlertDomainModel

fun AlertDomainModel.toPresentation(): AlertPresentationModel = AlertPresentationModel(
    id = id,
    event = event,
    headline = headline,
    description = description,
    instruction = instruction,
    severity = severity.toAlertSeverity(),
    severityLabel = severity
)

internal fun String.toAlertSeverity(): AlertSeverity = when (trim().lowercase()) {
    "extreme" -> AlertSeverity.Extreme
    "severe" -> AlertSeverity.Severe
    "moderate" -> AlertSeverity.Moderate
    else -> AlertSeverity.Other
}
```

## alerts\presentation\model\AlertPresentationModel.kt

```kotlin
package com.mhq.fynecast.alerts.presentation.model

/** What the Alerts screen needs to show one alert. The UI never sees the domain model. */
data class AlertPresentationModel(
    val id: String,
    val event: String,
    val headline: String,
    val description: String,
    val instruction: String,
    val severity: AlertSeverity,
    /** The severity text as reported, for the badge. */
    val severityLabel: String
)

enum class AlertSeverity {
    Extreme,
    Severe,
    Moderate,

    /** Minor, unknown or anything not recognised. */
    Other
}
```

## alerts\ui\components\AlertCard.kt

```kotlin
package com.mhq.fynecast.alerts.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.R
import com.mhq.fynecast.alerts.presentation.model.AlertPresentationModel
import com.mhq.fynecast.alerts.presentation.model.AlertSeverity
import com.mhq.fynecast.alerts.ui.mapper.toColor
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

/**
 * Premium, modern, sleek, and space-efficient weather card implementation.
 * Maintains full architectural compatibility, existing callbacks, and domain models.
 */
@Composable
fun AlertCard(
    alert: AlertPresentationModel,
    isTracked: Boolean,
    isExpanded: Boolean,
    onTrackToggle: () -> Unit,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val severityColor = remember(alert.severity) { alert.severity.toColor() }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Premium multi-layered glass-morphic background effect
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            severityColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .clip(RoundedCornerShape(22.dp))
                .clickable { onExpandToggle() }
                .padding(16.dp) // Optimized space efficiency (reduced from 20dp)
        ) {
            // Header Layer: Status Indicator, Event Title, and Severity Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color = severityColor, shape = CircleShape)
                    )
                    Text(
                        text = alert.event.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = severityColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(
                            color = severityColor.copy(alpha = 0.14f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = alert.severityLabel.uppercase(),
                        color = severityColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Headline & Expand Toggle Icon Layer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = alert.headline,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Smooth Expandable Metadata Area (Description & Instructions)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // Divider line for sleek visual separation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = alert.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    if (alert.instruction.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.instructions).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = alert.instruction,
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tracking Control Button Action Layer
            AlertTrackingButton(
                isTracked = isTracked,
                severityColor = severityColor,
                onTrackToggle = onTrackToggle
            )
        }
    }
}

@Preview
@Composable
private fun AlertCardPreview() {
    FyneCastTheme() {
        AlertCard(
            alert = AlertPresentationModel(
                id = "1",
                event = "Flood",
                headline = "Flood warning for the coast",
                description = "Water levels are rising.",
                instruction = "Move to higher ground.",
                severity = AlertSeverity.Severe,
                severityLabel = "Severe"
            ),
            isTracked = true,
            isExpanded = true,
            onTrackToggle = {},
            onExpandToggle = {}
        )
    }
}
```

## alerts\ui\components\AlertTrackingButton.kt

```kotlin
package com.mhq.fynecast.alerts.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R

/**
 * Composable button allowing users to toggle tracking status on/off for a specific alert.
 */
@Composable
fun AlertTrackingButton(
    isTracked: Boolean,
    severityColor: Color,
    onTrackToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (isTracked) severityColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        animationSpec = tween(durationMillis = 250), label = "buttonBg"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (isTracked) severityColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        animationSpec = tween(durationMillis = 250), label = "buttonContent"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isTracked) severityColor.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = tween(durationMillis = 250), label = "buttonBorder"
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(animatedBgColor)
            .border(width = 1.dp, color = animatedBorderColor, shape = RoundedCornerShape(16.dp))
            .clickable { onTrackToggle() }
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Icon(
            imageVector = if (isTracked) Icons.Rounded.NotificationsOff else Icons.Rounded.NotificationsActive,
            contentDescription = null,
            tint = animatedContentColor,
            modifier = Modifier.clip(RoundedCornerShape(40))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isTracked) {
                stringResource(R.string.cancel_tracking)
            } else {
                stringResource(R.string.track_this_alert)
            },
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
            color = animatedContentColor
        )
    }
}
```

## alerts\ui\components\NotificationsDialog.kt

```kotlin
package com.mhq.fynecast.alerts.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mhq.fynecast.R

/**
 * Reusable alert dialog prompting the user for notification permissions or settings adjustments.
 */
@Composable
fun NotificationsDialog(
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notifications_dialog_title)) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.notifications_dialog_dismiss)) } }
    )
}
```

## alerts\ui\mapper\AlertSeverityColors.kt

```kotlin
package com.mhq.fynecast.alerts.ui.mapper

import androidx.compose.ui.graphics.Color
import com.mhq.fynecast.alerts.presentation.model.AlertSeverity
import com.mhq.fynecast.core.ui.theme.AppleRed
import com.mhq.fynecast.core.ui.theme.EditorialRoyalBlue
import com.mhq.fynecast.core.ui.theme.Tangerine
import com.mhq.fynecast.core.ui.theme.VividAmber

fun AlertSeverity.toColor(): Color = when (this) {
    AlertSeverity.Extreme -> AppleRed
    AlertSeverity.Severe -> Tangerine
    AlertSeverity.Moderate -> VividAmber
    AlertSeverity.Other -> EditorialRoyalBlue
}
```

## alerts\ui\screens\AlertsBlank.kt

```kotlin
package com.mhq.fynecast.alerts.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.BlankPagePlaceholder
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

/**
 * Empty state screen shown when there are no active weather alerts in the area.
 */
@Composable
fun AlertsBlank(
    contentPadding: PaddingValues = PaddingValues(8.dp),
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize()
    ) {
        BlankPagePlaceholder(
            blankSectionIcon = Icons.Default.Block,
            blankSectionIconDescription = null,
            blankSectionTitle = stringResource(R.string.no_active_alerts_in_this_area),
            blankSectionSubtitle = stringResource(R.string.a_great_chance_to_enjoy_the_weather_outdoors)
        )
    }
}
```

## alerts\ui\screens\AlertsContainer.kt

```kotlin
package com.mhq.fynecast.alerts.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.R
import com.mhq.fynecast.alerts.presentation.AlertsEvent
import com.mhq.fynecast.alerts.presentation.AlertsUiState
import com.mhq.fynecast.alerts.presentation.AlertsViewModel
import com.mhq.fynecast.alerts.ui.components.NotificationsDialog
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.ui.openNotificationSettings
import kotlinx.coroutines.launch

/**
 * Root container composable for the alerts feature, managing snackbars, dialogs, and permission flows.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AlertsContainer(
    alertsViewModel: AlertsViewModel = viewModel(factory = AppViewModelFactory),    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by alertsViewModel.uiState.collectAsStateWithLifecycle()
    val trackedAlertIds by alertsViewModel.trackedAlertIds.collectAsStateWithLifecycle()
    val showNotificationsPrompt by alertsViewModel.showNotificationsPrompt.collectAsStateWithLifecycle()
    var showOpenSettingsDialog by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> alertsViewModel.onNotificationPermissionResult(granted) }

    fun showMessage(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    LaunchedEffect(alertsViewModel) {
        alertsViewModel.events.collect { event ->
            when (event) {
                AlertsEvent.AlertAlreadyExpired ->
                    showMessage(resources.getString(R.string.alert_already_expired))

                AlertsEvent.AlertExpiryUnknown ->
                    showMessage(resources.getString(R.string.alert_expiry_unknown))

                is AlertsEvent.AlertTrackingStarted ->
                    showMessage(resources.getString(R.string.alert_tracking_started, event.eventName))


                AlertsEvent.RequestNotificationPermission ->
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

                AlertsEvent.NotificationPermissionDenied -> {
                    val activity = context.findActivity()
                    val blockedForGood = activity != null &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(
                                activity, Manifest.permission.POST_NOTIFICATIONS
                            )
                    if (blockedForGood) {
                        showMessage(
                            message = resources.getString(R.string.notifications_settings_message),
                            actionLabel = resources.getString(R.string.notifications_settings_confirm)
                        ) { context.openNotificationSettings() }
                    } else {
                        showMessage(resources.getString(R.string.notifications_denied_message))
                    }
                }
            }
        }
    }

    if (showNotificationsPrompt) {
        NotificationsDialog(
            message = stringResource(R.string.notifications_dialog_message),
            confirmText = stringResource(R.string.notifications_dialog_confirm),
            onConfirm = alertsViewModel::onNotificationsPromptConfirmed,
            onDismiss = alertsViewModel::onNotificationsPromptDismissed
        )
    }

    if (showOpenSettingsDialog) {
        NotificationsDialog(
            message = stringResource(R.string.notifications_settings_message),
            confirmText = stringResource(R.string.notifications_settings_confirm),
            onConfirm = {
                showOpenSettingsDialog = false
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            },
            onDismiss = { showOpenSettingsDialog = false }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is AlertsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is AlertsUiState.Unavailable -> {
                AlertsUnavailable(
                    contentPadding = contentPadding,
                )
            }

            is AlertsUiState.Success -> {
                if (state.alerts.isEmpty()) {
                    AlertsBlank(contentPadding = contentPadding)
                } else {
                    AlertsContent(
                        alerts = state.alerts,
                        trackedAlertIds = trackedAlertIds,
                        onTrackToggle = alertsViewModel::onAlertTrackToggled,
                        contentPadding = contentPadding
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding())
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
```

## alerts\ui\screens\AlertsContent.kt

```kotlin
package com.mhq.fynecast.alerts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.alerts.presentation.model.AlertPresentationModel
import com.mhq.fynecast.alerts.ui.components.AlertCard

/**
 * Composable list content displaying the lazy column of available weather alert cards.
 */
@Composable
fun AlertsContent(
    alerts: List<AlertPresentationModel>,
    trackedAlertIds: List<String>,
    onTrackToggle: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val expandedAlertIds = rememberSaveable(saver = ExpandedAlertIdsSaver) { mutableStateListOf<String>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 48.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = alerts,
                key = { it.id }
            ) { alert ->
                val isExpanded = expandedAlertIds.contains(alert.id)

                AlertCard(
                    alert = alert,
                    isTracked = trackedAlertIds.contains(alert.id),
                    isExpanded = isExpanded,
                    onExpandToggle = {
                        if (isExpanded) {
                            expandedAlertIds.remove(alert.id)
                        } else {
                            expandedAlertIds.add(alert.id)
                        }
                    },
                    onTrackToggle = { onTrackToggle(alert.id) }
                )
            }
        }
    }
}

private val ExpandedAlertIdsSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() }
)
```

## alerts\ui\screens\AlertsUnavailable.kt

```kotlin
package com.mhq.fynecast.alerts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.BlankPagePlaceholder
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AlertsUnavailable(
    contentPadding: PaddingValues = PaddingValues(8.dp),
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize()
    ) {
        BlankPagePlaceholder(
            blankSectionIcon = Icons.Default.WifiOff,
            blankSectionIconDescription = null,
            blankSectionTitle = stringResource(R.string.alerts_unavailable),
            blankSectionSubtitle = stringResource(R.string.alerts_connect_to_the_internet)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AlertsUnavailablePreview() {
    FyneCastTheme() {
        AlertsUnavailable()
    }
}
```

## auth\data\repoimpl\auth\AuthRepositoryImpl.kt

```kotlin
package com.mhq.fynecast.auth.data.repoimpl.auth

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.mhq.fynecast.auth.data.repository.auth.SocialAuthCredentialProvider
import com.mhq.fynecast.auth.data.session.SessionKeys
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.core.domain.util.suspendRunCatching
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val sessionDataStore: DataStore<Preferences>,
    private val socialAuthCredentialProvider: SocialAuthCredentialProvider
) : AuthRepository {

    override suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): Result<UserDomainModel> = suspendRunCatching {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("Auth registration failed.")

        /** Account already exists at this point; displayName is cosmetic,
         * so a failure here shouldn't fail the whole registration.
         * Retry once so a transient failure doesn't leave the name unpersisted server-side,
         * and surface it if it still doesn't stick so it isn't silently lost. */
        val profileSynced = suspendRunCatching {
            user.updateProfile(userProfileChangeRequest { displayName = name }).await()
        }.isSuccess
        if (!profileSynced) {
            val retrySynced = suspendRunCatching {
                user.updateProfile(userProfileChangeRequest { displayName = name }).await()
            }.isSuccess
            if (!retrySynced) {
                Log.w(
                    "AuthRepositoryImpl",
                    "Display name for ${user.uid} was not persisted to Firebase after registration."
                )
            }
        }

        UserDomainModel(user.uid, name, email)
    }

    override suspend fun loginWithEmail(email: String, password: String): Result<UserDomainModel> =
        suspendRunCatching {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Auth login failed.")

            /** The local cache lookup is a best-effort fallback only;
             * a DataStore read failure here must not fail an otherwise-successful login. */
            val snapshot = suspendRunCatching { sessionDataStore.data.first() }.getOrNull()
            val cachedUsername = snapshot?.get(SessionKeys.USERNAME)
                ?.takeIf { snapshot[SessionKeys.USER_ID] == user.uid }
            val resolvedName = user.displayName ?: cachedUsername ?: "Username"
            UserDomainModel(id = user.uid, name = resolvedName, email = user.email ?: email)
        }

    override suspend fun loginWithGoogle(idToken: String): Result<UserDomainModel> =
        suspendRunCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Google token confirmation failed.")
            UserDomainModel(user.uid, user.displayName ?: "", user.email ?: "")
        }

    override suspend fun loginWithFacebook(accessToken: String): Result<UserDomainModel> =
        suspendRunCatching {
            val credential = FacebookAuthProvider.getCredential(accessToken)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Facebook session parsing failed.")
            UserDomainModel(user.uid, user.displayName ?: "", user.email ?: "")
        }

    override suspend fun addEmailPasswordLogin(email: String, password: String): Result<Unit> =
        suspendRunCatching {
            val user = firebaseAuth.currentUser
                ?: throw Exception("No authenticated user active session found")
            user.linkWithCredential(EmailAuthProvider.getCredential(email, password)).await()
            user.reload().await()
            val current = firebaseAuth.currentUser ?: user
            if (current.email.equals(email, ignoreCase = true)) {
                // Linking already made this the account's email; it only needs verifying.
                current.sendEmailVerification().await()
            } else {
                current.verifyBeforeUpdateEmail(email).await()
            }
        }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        suspendRunCatching {
            firebaseAuth.sendPasswordResetEmail(email).await()
        }

    override fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null

    override suspend fun saveLocalSession(user: UserDomainModel) {
        sessionDataStore.edit { prefs ->
            prefs[SessionKeys.USER_ID] = user.id
            prefs[SessionKeys.USERNAME] = user.name
            prefs[SessionKeys.EMAIL] = user.email

            /** Bring back this account's own picture (if it saved one); never another account's.*/
            val savedImage = prefs[SessionKeys.profileImageFor(user.id)]
            if (savedImage != null) {
                prefs[SessionKeys.PROFILE_IMAGE_URI] = savedImage
            } else {
                prefs.remove(SessionKeys.PROFILE_IMAGE_URI)
            }
        }
    }

    override suspend fun detachOutdatedSocialProviders() {
        try {
            val user = firebaseAuth.currentUser ?: return
            val accountEmail = user.email ?: return
            if (!user.isEmailVerified) return
            val passwordWorksForAccountEmail = user.providerData.any {
                it.providerId == EmailAuthProvider.PROVIDER_ID &&
                        it.email.equals(accountEmail, ignoreCase = true)
            }
            if (!passwordWorksForAccountEmail) return
            val outdated = user.providerData.filter {
                (it.providerId == GoogleAuthProvider.PROVIDER_ID ||
                        it.providerId == FacebookAuthProvider.PROVIDER_ID) &&
                        it.email != null &&
                        !it.email.equals(accountEmail, ignoreCase = true)
            }
            outdated.forEach { user.unlink(it.providerId).await() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("AuthRepository", "Could not detach outdated social providers", e)
        }
    }

    override suspend fun clearSession() {
        firebaseAuth.signOut()
        sessionDataStore.edit { prefs ->
            /** Park the picture under the account's id before wiping the active session.*/
            val uid = prefs[SessionKeys.USER_ID]
            val image = prefs[SessionKeys.PROFILE_IMAGE_URI]
            if (!uid.isNullOrBlank() && image != null) {
                prefs[SessionKeys.profileImageFor(uid)] = image
            }
            prefs.remove(SessionKeys.USER_ID)
            prefs.remove(SessionKeys.USERNAME)
            prefs.remove(SessionKeys.EMAIL)
            prefs.remove(SessionKeys.PROFILE_IMAGE_URI)
        }
        socialAuthCredentialProvider.clearCredentials()
    }
}
```

## auth\data\repoimpl\auth\SocialAuthCredentialProviderImpl.kt

```kotlin
package com.mhq.fynecast.auth.data.repoimpl.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.facebook.login.LoginManager
import com.mhq.fynecast.auth.data.repository.auth.SocialAuthCredentialProvider
import kotlinx.coroutines.CancellationException

class SocialAuthCredentialProviderImpl(
    private val appContext: Context
) : SocialAuthCredentialProvider {

    override suspend fun clearCredentials() {
        LoginManager.getInstance().logOut()
        try {
            CredentialManager
                .create(appContext)
                .clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort: signing out must not fail because the credential provider did.
        }
    }
}
```

## auth\data\repoimpl\profile\ImageStorageRepositoryImpl.kt

```kotlin
package com.mhq.fynecast.auth.data.repoimpl.profile

import android.content.Context
import androidx.core.net.toUri
import com.mhq.fynecast.auth.domain.repository.profile.ImageStorageRepository
import com.mhq.fynecast.auth.data.storage.ImageStorageManager
import java.io.File

class ImageStorageRepositoryImpl(
    private val context: Context,
    private val imageStorageManager: ImageStorageManager
) : ImageStorageRepository {

    override fun isAlreadySandboxed(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        val uri = uriString.toUri()
        if (uri.scheme != "file") return false
        val path = uri.path ?: return false

        val canonicalFilesDir = File(context.filesDir.path).canonicalPath + File.separator
        val canonicalTarget = runCatching { File(path).canonicalPath }.getOrNull() ?: return false

        return canonicalTarget.startsWith(canonicalFilesDir)
    }

    override suspend fun sandboxAndCompressImage(uriString: String): String? {
        return imageStorageManager.processAndSaveProfileImage(uriString)
    }

    override fun deleteProfileImage(uriString: String) {
        imageStorageManager.deleteProfileImage(uriString)
    }
}
```

## auth\data\repoimpl\profile\UserProfileRepositoryImpl.kt

```kotlin
package com.mhq.fynecast.auth.data.repoimpl.profile

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.userProfileChangeRequest
import com.mhq.fynecast.auth.data.session.SessionKeys
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
import com.mhq.fynecast.core.domain.util.suspendRunCatching
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserProfileRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val sessionDataStore: DataStore<Preferences>,
    externalScope: CoroutineScope
) : UserProfileRepository {

    private val _profileState =
        MutableStateFlow(
            UserDomainModel(
                "",
                "Username",
                "username@gmail.com",
                null
            )
        )

    override val profileState: StateFlow<UserDomainModel> = _profileState.asStateFlow()

    init {
        externalScope.launch(Dispatchers.IO) {
            try {
                refreshActiveUserSession()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("UserProfileRepository", "Initial profile refresh failed", e)
            }
        }
    }

    override suspend fun getProfile(): UserDomainModel = withContext(Dispatchers.IO) {
        val user = firebaseAuth.currentUser
        val snapshot = sessionDataStore.data.first()
        val name = snapshot[SessionKeys.USERNAME] ?: user?.displayName ?: "Username"
        val email = user?.email?.takeIf { it.isNotBlank() }
            ?: snapshot[SessionKeys.EMAIL]
            ?: "username@gmail.com"
        val img = snapshot[SessionKeys.PROFILE_IMAGE_URI]
        UserDomainModel(
            id = user?.uid ?: snapshot[SessionKeys.USER_ID] ?: "",
            name = name,
            email = email,
            profileImageUri = img,
            hasPasswordLogin = user?.providerData
                ?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } ?: true
        )
    }

    override suspend fun saveProfile(profile: UserDomainModel) {
        withContext(Dispatchers.IO) {
            sessionDataStore.edit { prefs ->
                prefs[SessionKeys.USERNAME] = profile.name
                prefs[SessionKeys.EMAIL] = profile.email
                if (profile.profileImageUri != null) {
                    prefs[SessionKeys.PROFILE_IMAGE_URI] = profile.profileImageUri
                }
            }
        }
    }

    override suspend fun updateRemoteProfile(name: String, email: String): Result<Boolean> =
        suspendRunCatching {
            withContext(Dispatchers.IO) {
                val user = firebaseAuth.currentUser
                    ?: throw Exception("No authenticated user active session found")
                var verificationSent = false
                if (user.displayName != name) {
                    user.updateProfile(userProfileChangeRequest { displayName = name }).await()
                }
                if (user.email != email) {
                    user.verifyBeforeUpdateEmail(email).await()
                    verificationSent = true
                }
                verificationSent
            }
        }

    override suspend fun refreshActiveUserSession(): Boolean {
        var isSessionValid = true
        try {
            firebaseAuth.currentUser?.reload()?.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidUserException) {
            // Email/password changed elsewhere, or the account was disabled or deleted.
            isSessionValid = false
        } catch (e: Exception) {
            Log.w("UserProfileRepository", "Session refresh failed", e)
        }
        _profileState.value = getProfile()
        return isSessionValid
    }

}
```

## auth\data\repository\auth\SocialAuthCredentialProvider.kt

```kotlin
package com.mhq.fynecast.auth.data.repository.auth

interface SocialAuthCredentialProvider {
    /** Forgets the Google and Facebook sessions so the next sign-in starts clean. */
    suspend fun clearCredentials()
}
```

## auth\data\session\SessionKeys.kt

```kotlin
package com.mhq.fynecast.auth.data.session

import androidx.datastore.preferences.core.stringPreferencesKey

internal object SessionKeys {
    val USER_ID = stringPreferencesKey("user_id")
    val USERNAME = stringPreferencesKey("username")
    val EMAIL = stringPreferencesKey("email")
    val PROFILE_IMAGE_URI = stringPreferencesKey("profile_image_uri")
    /** Per-account copy of the profile picture; survives logout so it returns on the next login. */
    fun profileImageFor(userId: String) = stringPreferencesKey("profile_image_uri_$userId")
}
```

## auth\data\storage\ImageStorageManager.kt

```kotlin
package com.mhq.fynecast.auth.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class ImageStorageManager(private val context: Context) {

    suspend fun processAndSaveProfileImage(uriString: String?): String? =
        withContext(Dispatchers.IO) {
            if (uriString.isNullOrBlank()) return@withContext null
            val sourceUri = uriString.toUri()

            if (sourceUri.scheme == "file" && isWithinFilesDir(sourceUri.path)) {
                return@withContext uriString
            }

            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(sourceUri).use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                val maxDimension = 1080
                var inSampleSize = 1
                while (
                    options.outHeight / inSampleSize > maxDimension ||
                    options.outWidth / inSampleSize > maxDimension
                ) {
                    inSampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val sampledBitmap =
                    context.contentResolver.openInputStream(sourceUri).use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    } ?: return@withContext null

                val correctedBitmap = correctImageOrientation(sourceUri, sampledBitmap)

                val profilePicturesDir = File(context.filesDir, "profile_pics").apply { mkdirs() }
                val uniqueFileName = "profile_${UUID.randomUUID()}.webp"
                val destinationFile = File(profilePicturesDir, uniqueFileName)

                val compressed = FileOutputStream(destinationFile).use { outStream ->
                    correctedBitmap.compress(Bitmap.CompressFormat.WEBP, 85, outStream)
                }

                if (correctedBitmap != sampledBitmap) {
                    correctedBitmap.recycle()
                }
                sampledBitmap.recycle()

                if (!compressed) {
                    destinationFile.delete()
                    return@withContext null
                }

                return@withContext Uri.fromFile(destinationFile).toString()

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }

    /**
     * Deletes a single sandboxed profile image by its file:// URI. Used to
     * roll back a newly-created image when the profile update that was
     * supposed to reference it fails, so it isn't left orphaned.
     */
    fun deleteProfileImage(uriString: String) {
        runCatching {
            uriString.toUri().path?.let { File(it).delete() }
        }
    }

    private fun isWithinFilesDir(path: String?): Boolean {
        if (path == null) return false
        val canonicalFilesDir = File(context.filesDir.path).canonicalPath + File.separator
        val canonicalTarget = runCatching { File(path).canonicalPath }.getOrNull() ?: return false
        return canonicalTarget.startsWith(canonicalFilesDir)
    }

    private fun correctImageOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val exifInterface = inputStream?.use { ExifInterface(it) } ?: return bitmap
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
}
```

## auth\domain\models\UserDomainModel.kt

```kotlin
package com.mhq.fynecast.auth.domain.models

data class UserDomainModel(
    val id: String,
    val name: String,
    val email: String,
    val profileImageUri: String? = null,
    val hasPasswordLogin: Boolean = true
)
```

## auth\domain\models\ValidationError.kt

```kotlin
package com.mhq.fynecast.auth.domain.models

enum class ValidationError {
    EmailEmpty,
    EmailInvalid,
    PasswordEmpty,
    PasswordWeak,
    ConfirmPasswordEmpty,
    PasswordsMismatch,
    NameEmpty,
    NameInvalidForSignup,
    NameInvalidForProfile
}
```

## auth\domain\models\ValidationException.kt

```kotlin
package com.mhq.fynecast.auth.domain.models

/** Thrown by use cases that validate input themselves;
 * the UI maps [error] to localized text. */
class ValidationException(val error: ValidationError) : Exception()
```

## auth\domain\models\ValidationResult.kt

```kotlin
package com.mhq.fynecast.auth.domain.models

sealed interface ValidationResult {
    object Success : ValidationResult
    data class Failure(val error: ValidationError) : ValidationResult
}
```

## auth\domain\repository\auth\AuthRepository.kt

```kotlin
package com.mhq.fynecast.auth.domain.repository.auth

import com.mhq.fynecast.auth.domain.models.UserDomainModel

interface AuthRepository {

    // Standard Email Operations
    suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): Result<UserDomainModel>

    suspend fun loginWithEmail(
        email: String,
        password: String
    ): Result<UserDomainModel>


    // Social Media Token Operations
    suspend fun loginWithGoogle(
        idToken: String
    ): Result<UserDomainModel>

    suspend fun loginWithFacebook(
        accessToken: String
    ): Result<UserDomainModel>

    /** Adds email/password sign-in for [email] to a social account and sends that email a verification link. */
    suspend fun addEmailPasswordLogin(email: String, password: String): Result<Unit>

    // Password Reset
    suspend fun sendPasswordResetEmail(
        email: String
    ): Result<Unit>


    // Session Management persistence
    fun isUserLoggedIn(): Boolean

    suspend fun saveLocalSession(
        user: UserDomainModel
    )

    /**
     * After an email switch: detaches Google/Facebook links whose email is no longer the account's email.
     * Only runs when email/password login already works for the account's current email.
     */
    suspend fun detachOutdatedSocialProviders()

    suspend fun clearSession()
}
```

## auth\domain\repository\profile\ImageStorageRepository.kt

```kotlin
package com.mhq.fynecast.auth.domain.repository.profile

interface ImageStorageRepository {
    fun isAlreadySandboxed(uriString: String?): Boolean
    suspend fun sandboxAndCompressImage(uriString: String): String?
    fun deleteProfileImage(uriString: String)
}
```

## auth\domain\repository\profile\UserProfileRepository.kt

```kotlin
package com.mhq.fynecast.auth.domain.repository.profile

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import kotlinx.coroutines.flow.StateFlow

interface UserProfileRepository {
    val profileState: StateFlow<UserDomainModel>
    suspend fun getProfile(): UserDomainModel
    suspend fun saveProfile(profile: UserDomainModel)
    suspend fun updateRemoteProfile(name: String, email: String): Result<Boolean>
    /** @return false when Firebase says the account changed and the user must sign in again. */
    suspend fun refreshActiveUserSession(): Boolean
}
```

## auth\domain\usecases\auth\AuthenticateWithSocialTokenUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository

class AuthenticateWithSocialTokenUseCase(
    private val authRepository: AuthRepository,
    private val completeAuthSessionUseCase: CompleteAuthSessionUseCase
) {
    suspend operator fun invoke(token: String, isGoogle: Boolean): Result<UserDomainModel> {
        val result =
            if (isGoogle)
                authRepository.loginWithGoogle(token)
            else
                authRepository.loginWithFacebook(token)
        return completeAuthSessionUseCase(result)
    }
}
```

## auth\domain\usecases\auth\CompleteAuthSessionUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository

class CompleteAuthSessionUseCase(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(result: Result<UserDomainModel>): Result<UserDomainModel> =
        result.onSuccess { user ->
            authRepository.saveLocalSession(user)
            userProfileRepository.refreshActiveUserSession()
        }
}
```

## auth\domain\usecases\auth\LoginWithEmailUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository

class LoginWithEmailUseCase(
    private val authRepository: AuthRepository,
    private val completeAuthSessionUseCase: CompleteAuthSessionUseCase
) {
    suspend operator fun invoke(email: String, pass: String): Result<UserDomainModel> {
        val result = authRepository.loginWithEmail(email, pass)
        if (result.isSuccess) authRepository.detachOutdatedSocialProviders()
        return completeAuthSessionUseCase(result)
    }
}
```

## auth\domain\usecases\auth\RegisterWithEmailUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository

class RegisterWithEmailUseCase(
    private val authRepository: AuthRepository,
    private val completeAuthSessionUseCase: CompleteAuthSessionUseCase
) {
    suspend operator fun invoke(
        username: String,
        email: String,
        password: String
    ): Result<UserDomainModel> {
        val result = authRepository.registerWithEmail(username, email, password)
        return completeAuthSessionUseCase(result)
    }
}
```

## auth\domain\usecases\auth\SendPasswordResetUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationException
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.validation.EmailFormatValidator

class SendPasswordResetUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(ValidationException(ValidationError.EmailEmpty))
        }
        if (!EmailFormatValidator.isValid(email)) {
            return Result.failure(ValidationException(ValidationError.EmailInvalid))
        }
        return authRepository.sendPasswordResetEmail(email)
    }
}
```

## auth\domain\usecases\auth\ValidateLoginFormUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.validation.EmailFormatValidator

class ValidateLoginFormUseCase {
    fun validateEmailFormat(email: String): Boolean {
        return EmailFormatValidator.isValidOrEmpty(email)
    }

    operator fun invoke(email: String, password: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Failure(ValidationError.EmailEmpty)
            !EmailFormatValidator.isValid(email) -> ValidationResult.Failure(ValidationError.EmailInvalid)
            password.isBlank() -> ValidationResult.Failure(ValidationError.PasswordEmpty)
            else -> ValidationResult.Success
        }
    }
}
```

## auth\domain\usecases\auth\ValidateSignupFormUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.validation.EmailFormatValidator
import com.mhq.fynecast.auth.domain.validation.PasswordValidator

class ValidateSignupFormUseCase {
    private val usernameRegex =
        Regex("^[a-zA-Z0-9_]{3,15}$")

    fun validateUsername(username: String): Boolean =
        username.isEmpty() || username.matches(usernameRegex)

    fun validateEmail(email: String): Boolean =
        EmailFormatValidator.isValidOrEmpty(email)

    fun validatePassword(password: String): Boolean =
        PasswordValidator.isValidOrEmpty(password)

    fun validatePasswordMatch(password: String, confirmPassword: String): Boolean =
        confirmPassword.isEmpty() || password == confirmPassword

    operator fun invoke(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Failure(ValidationError.NameEmpty)
            !username.matches(usernameRegex) -> ValidationResult.Failure(ValidationError.NameInvalidForSignup)

            email.isBlank() -> ValidationResult.Failure(ValidationError.EmailEmpty)
            !EmailFormatValidator.isValid(email) -> ValidationResult.Failure(ValidationError.EmailInvalid)

            password.isBlank() -> ValidationResult.Failure(ValidationError.PasswordEmpty)
            !PasswordValidator.isValid(password) -> ValidationResult.Failure(ValidationError.PasswordWeak)

            confirmPassword.isBlank() -> ValidationResult.Failure(ValidationError.ConfirmPasswordEmpty)
            password != confirmPassword -> ValidationResult.Failure(ValidationError.PasswordsMismatch)

            else -> ValidationResult.Success
        }
    }
}
```

## auth\domain\usecases\profile\ObserveUserProfileUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveUserProfileUseCase(
    private val profileRepository: UserProfileRepository
) {
    operator fun invoke(): StateFlow<UserDomainModel> {
        return profileRepository.profileState
    }
}
```

## auth\domain\usecases\profile\RefreshUserSessionUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
import com.mhq.fynecast.auth.domain.usecases.session.LogoutUserUseCase

class RefreshUserSessionUseCase(
    private val profileRepository: UserProfileRepository,
    private val authRepository: AuthRepository,
    private val logoutUserUseCase: LogoutUserUseCase
) {
    /** @return false when the session had expired and the user was signed out. */
    suspend operator fun invoke(): Boolean {
        if (!profileRepository.refreshActiveUserSession()) {
            logoutUserUseCase()
            return false
        }
        authRepository.detachOutdatedSocialProviders()
        return true
    }
}
```

## auth\domain\usecases\profile\UpdateUserProfileUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.profile.ImageStorageRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository

class UpdateUserProfileUseCase(
    private val profileRepository: UserProfileRepository,
    private val imageRepository: ImageStorageRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        newName: String,
        newEmail: String,
        newPassword: String? = null,
        localImageUriString: String?
    ): Result<Boolean> {
        val currentProfile = profileRepository.getProfile()
        val switchesToPasswordLogin = !currentProfile.hasPasswordLogin &&
                !newEmail.equals(currentProfile.email, ignoreCase = true)
        var finalImageUri = currentProfile.profileImageUri
        val previousImageUri = currentProfile.profileImageUri
        var createdImageUri: String? = null

        if (!localImageUriString.isNullOrBlank()) {
            if (!imageRepository.isAlreadySandboxed(localImageUriString)) {
                val sandboxedUri = imageRepository.sandboxAndCompressImage(localImageUriString)
                if (sandboxedUri != null) {
                    finalImageUri = sandboxedUri
                    createdImageUri = sandboxedUri
                }
            } else {
                finalImageUri = localImageUriString
            }
        }

        var profileSaved = false

        return runCatching {
            var remoteUpdateVerification = profileRepository.updateRemoteProfile(
                newName,
                if (switchesToPasswordLogin) currentProfile.email else newEmail
            ).getOrThrow()
            if (switchesToPasswordLogin) {
                authRepository.addEmailPasswordLogin(
                    newEmail,
                    requireNotNull(newPassword)
                ).getOrThrow()
                remoteUpdateVerification = true
            }

            profileRepository.saveProfile(
                currentProfile.copy(
                    name = newName,
                    /** A changed email only applies after the user clicks the verification link,
                    so keep showing the current one until then.*/
                    email = if (remoteUpdateVerification) currentProfile.email else newEmail,
                    profileImageUri = finalImageUri
                )
            )
            profileSaved = true
            profileRepository.refreshActiveUserSession()

            /** The profile now durably references the new image, so only this account's
             * replaced picture is deleted. The whole folder is never purged, because other
             * accounts' pictures live there too. Doing this any earlier risked leaving
             * the profile pointing at a deleted file if the save above failed. */
            createdImageUri?.let { newUri ->
                if (previousImageUri != null &&
                    previousImageUri != newUri &&
                    imageRepository.isAlreadySandboxed(previousImageUri)
                ) {
                    imageRepository.deleteProfileImage(previousImageUri)
                }
            }


            remoteUpdateVerification
        }.onFailure {
            /**Only roll back if the profile never ended up pointing at this file.
             * If saveProfile already succeeded
             * and a later step (refreshActiveUserSession)
             * is what threw, deleting the file now would leave
             * the saved profile pointing at a file that no longer exists
             * — the exact thing this rollback exists to prevent.*/
            if (!profileSaved) {
                createdImageUri?.let { imageRepository.deleteProfileImage(it) }
            }
        }
    }
}
```

## auth\domain\usecases\profile\ValidateEditProfileUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.validation.EmailFormatValidator
import com.mhq.fynecast.auth.domain.validation.PasswordValidator

class ValidateEditProfileUseCase {
    private val usernameRegex = Regex("^[a-zA-Z0-9_ -]{3,30}$")

    fun validateUsernameFormat(name: String): Boolean {
        return name.isEmpty() || name.matches(usernameRegex)
    }

    fun validateEmailFormat(email: String): Boolean {
        return EmailFormatValidator.isValidOrEmpty(email)
    }

    fun validatePassword(password: String): Boolean = PasswordValidator.isValidOrEmpty(password)

    fun validatePasswordMatch(password: String, confirmPassword: String): Boolean =
        confirmPassword.isEmpty() || password == confirmPassword

    /** [newPassword] is non-null only when the email switch needs a password. */
    operator fun invoke(
        username: String,
        email: String,
        newPassword: String? = null,
        confirmPassword: String? = null
    ): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Failure(ValidationError.NameEmpty)
            !username.matches(usernameRegex) -> ValidationResult.Failure(ValidationError.NameInvalidForProfile)
            email.isBlank() -> ValidationResult.Failure(ValidationError.EmailEmpty)
            !EmailFormatValidator.isValid(email) -> ValidationResult.Failure(ValidationError.EmailInvalid)
            newPassword != null && newPassword.isBlank() ->
                ValidationResult.Failure(ValidationError.PasswordEmpty)
            newPassword != null && !PasswordValidator.isValid(newPassword) ->
                ValidationResult.Failure(ValidationError.PasswordWeak)
            newPassword != null && confirmPassword.isNullOrBlank() ->
                ValidationResult.Failure(ValidationError.ConfirmPasswordEmpty)
            newPassword != null && newPassword != confirmPassword ->
                ValidationResult.Failure(ValidationError.PasswordsMismatch)
            else -> ValidationResult.Success
        }
    }
}
```

## auth\domain\usecases\session\CheckUserLoggedInUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.session

import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository

class CheckUserLoggedInUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Boolean = authRepository.isUserLoggedIn()
}
```

## auth\domain\usecases\session\LogoutUserUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.session

import com.mhq.fynecast.alerts.domain.usecases.CancelAllAlertTrackingUseCase
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
import com.mhq.fynecast.settings.domain.usecases.profile.ClearLastSearchedCityUseCase

class LogoutUserUseCase(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val cancelAllAlertTrackingUseCase: CancelAllAlertTrackingUseCase,
    private val clearLastSearchedCityUseCase: ClearLastSearchedCityUseCase
) {
    suspend operator fun invoke(): Result<Unit> = runCatching {
        authRepository.clearSession()
        userProfileRepository.refreshActiveUserSession()

        runCatching { cancelAllAlertTrackingUseCase() }
        runCatching { clearLastSearchedCityUseCase() }
    }
}
```

## auth\domain\validation\EmailFormatValidator.kt

```kotlin
package com.mhq.fynecast.auth.domain.validation

object EmailFormatValidator {
    private val emailRegex = Regex(
        "^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$"
    )

    /** Strict: an empty string is NOT a valid email. */
    fun isValid(email: String): Boolean = email.matches(emailRegex)

    /** For live typing feedback: nothing is flagged until the user has typed something. */
    fun isValidOrEmpty(email: String): Boolean = email.isEmpty() || isValid(email)
}
```

## auth\domain\validation\PasswordValidator.kt

```kotlin
package com.mhq.fynecast.auth.domain.validation

object PasswordValidator {
    private val regex =
        Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$")

    fun isValid(password: String): Boolean = password.matches(regex)
    fun isValidOrEmpty(password: String): Boolean = password.isEmpty() || isValid(password)
}
```

## auth\presentation\editprofile\EditProfileUiState.kt

```kotlin
package com.mhq.fynecast.auth.presentation.editprofile

import com.mhq.fynecast.core.presentation.text.UiText

sealed interface EditProfileUiState {
    object Idle : EditProfileUiState
    object Loading : EditProfileUiState
    object Success : EditProfileUiState
    data class Error(val message: UiText) : EditProfileUiState
}
```

## auth\presentation\editprofile\EditProfileViewModel.kt

```kotlin
package com.mhq.fynecast.auth.presentation.editprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.usecases.profile.ObserveUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.UpdateUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ValidateEditProfileUseCase
import com.mhq.fynecast.auth.presentation.mapper.toEditProfilePresentation
import com.mhq.fynecast.auth.presentation.mapper.toUiText
import com.mhq.fynecast.auth.presentation.model.EditProfilePresentationModel
import com.mhq.fynecast.core.presentation.text.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class EditProfileViewModel(
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val validateEditProfileUseCase: ValidateEditProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) : ViewModel() {

    private val _editProfileUiState =
        MutableStateFlow<EditProfileUiState>(EditProfileUiState.Idle)

    private val _uiEventChannel = Channel<UiText>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()

    private val _navigationEventChannel = Channel<Unit>(Channel.BUFFERED)
    val navigationEvent = _navigationEventChannel.receiveAsFlow()

    /**
     * The stored profile, emitted once it is loaded (null until then).
     * The UI copies it into its own fields one time; later refreshes of the profile
     * never overwrite what the user is typing.
     */
    val initialProfile: StateFlow<EditProfilePresentationModel?> = flow {
        emit(observeUserProfileUseCase().first { it.id.isNotEmpty() }.toEditProfilePresentation())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Pure checks used by the UI to show field errors while typing. */
    fun isUsernameValid(username: String): Boolean =
        validateEditProfileUseCase.validateUsernameFormat(username)

    fun isEmailValid(email: String): Boolean =
        validateEditProfileUseCase.validateEmailFormat(email)

    fun isPasswordValid(password: String): Boolean =
        validateEditProfileUseCase.validatePassword(password)

    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean =
        validateEditProfileUseCase.validatePasswordMatch(password, confirmPassword)


    fun onSaveProfileAction(
        username: String,
        email: String,
        profileImageUri: String?,
        newPassword: String?,
        confirmPassword: String?
    ) {
        when (val validation =
            validateEditProfileUseCase(username, email, newPassword, confirmPassword)) {
            is ValidationResult.Success -> {
                if (_editProfileUiState.value is EditProfileUiState.Loading) return
                _editProfileUiState.value = EditProfileUiState.Loading

                viewModelScope.launch {
                    _uiEventChannel.send(UiText.res(R.string.msg_profile_processing))

                    updateUserProfileUseCase(
                        newName = username,
                        newEmail = email,
                        localImageUriString = profileImageUri,
                        newPassword = newPassword
                    )
                        .onSuccess { verificationSent ->
                            _editProfileUiState.value = EditProfileUiState.Success
                            _uiEventChannel.send(
                                UiText.res(
                                    if (verificationSent) R.string.msg_profile_saved_verify_email
                                    else R.string.msg_profile_saved
                                )
                            )
                            delay(SUCCESS_MESSAGE_DISPLAY_DELAY_MS.milliseconds)
                            _navigationEventChannel.send(Unit)
                        }
                        .onFailure { e ->
                            val message = e.toUiText(R.string.error_profile_update_failed)
                            _editProfileUiState.value = EditProfileUiState.Error(message)
                            _uiEventChannel.send(message)
                        }
                }
            }

            is ValidationResult.Failure -> {
                viewModelScope.launch { _uiEventChannel.send(validation.error.toUiText()) }
            }
        }
    }

    companion object {
        private const val SUCCESS_MESSAGE_DISPLAY_DELAY_MS = 1500L
    }
}
```

## auth\presentation\forgotpassword\ForgotPasswordUiState.kt

```kotlin
package com.mhq.fynecast.auth.presentation.forgotpassword

import com.mhq.fynecast.core.presentation.text.UiText

sealed interface ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState
    object Loading : ForgotPasswordUiState
    object Success : ForgotPasswordUiState
    data class Error(val message: UiText) : ForgotPasswordUiState
}
```

## auth\presentation\forgotpassword\ForgotPasswordViewModel.kt

```kotlin
package com.mhq.fynecast.auth.presentation.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.domain.usecases.auth.SendPasswordResetUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateLoginFormUseCase
import com.mhq.fynecast.auth.presentation.mapper.toUiText
import com.mhq.fynecast.core.presentation.text.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ForgotPasswordViewModel(
    private val validateLoginFormUseCase: ValidateLoginFormUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase
) : ViewModel() {

    private val _forgotPasswordUiState =
        MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    private val _uiEventChannel = Channel<UiText>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()
    private val _navigationEvent = Channel<Unit>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    /** Pure check used by the UI to show the field error while typing. */
    fun isEmailValid(email: String): Boolean =
        validateLoginFormUseCase.validateEmailFormat(email)

    fun onPasswordResetAction(email: String) {
        val state = _forgotPasswordUiState.value
        if (state is ForgotPasswordUiState.Loading || state is ForgotPasswordUiState.Success) return
        viewModelScope.launch {
            _forgotPasswordUiState.value = ForgotPasswordUiState.Loading
            sendPasswordResetUseCase(email)
                .onSuccess {
                    _forgotPasswordUiState.value = ForgotPasswordUiState.Success
                    _uiEventChannel.send(UiText.res(R.string.msg_reset_link_sent, email))
                    delay(SUCCESS_MESSAGE_DISPLAY_DELAY_MS.milliseconds)
                    _navigationEvent.send(Unit)
                }
                .onFailure { error ->
                    val msg = error.toUiText(R.string.error_reset_link_failed)
                    _forgotPasswordUiState.value = ForgotPasswordUiState.Error(msg)
                    _uiEventChannel.send(msg)
                }
        }
    }

    companion object {
        private const val SUCCESS_MESSAGE_DISPLAY_DELAY_MS = 1500L
    }
}
```

## auth\presentation\login\LoginUiState.kt

```kotlin
package com.mhq.fynecast.auth.presentation.login

import com.mhq.fynecast.core.presentation.text.UiText

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    object Success : LoginUiState
    data class Error(val message: UiText) : LoginUiState
}
```

## auth\presentation\login\LoginViewModel.kt

```kotlin
package com.mhq.fynecast.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.usecases.auth.AuthenticateWithSocialTokenUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.LoginWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateLoginFormUseCase
import com.mhq.fynecast.auth.presentation.mapper.toUiText
import com.mhq.fynecast.core.presentation.text.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginWithEmailUseCase: LoginWithEmailUseCase,
    private val validateLoginFormUseCase: ValidateLoginFormUseCase,
    private val authenticateWithSocialTokenUseCase: AuthenticateWithSocialTokenUseCase
) : ViewModel() {

    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    private val _uiEventChannel = Channel<UiText>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()
    private val _navigationEvent = Channel<Unit>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    /** Pure check used by the UI to show the field error while typing. */
    fun isEmailValid(email: String): Boolean =
        validateLoginFormUseCase.validateEmailFormat(email)

    fun onLoginAction(email: String, password: String) {
        if (_loginUiState.value is LoginUiState.Loading) return
        when (val validation = validateLoginFormUseCase(email, password)) {
            is ValidationResult.Success -> {
                viewModelScope.launch {
                    _loginUiState.value = LoginUiState.Loading
                    loginWithEmailUseCase(email, password)
                        .onSuccess {
                            _loginUiState.value = LoginUiState.Success
                            _navigationEvent.send(Unit)
                        }
                        .onFailure { handleLoginFailure(it) }
                }
            }
            is ValidationResult.Failure -> {
                viewModelScope.launch { _uiEventChannel.send(validation.error.toUiText()) }
            }
        }
    }

    suspend fun onGoogleAuthenticate(fetchToken: suspend () -> String) =
        runSocialSignIn(
            UiText.res(R.string.error_google_sign_in_failed),
            isGoogle = true,
            fetchToken
        )

    suspend fun onFacebookAuthenticate(fetchToken: suspend () -> String) =
        runSocialSignIn(
            UiText.res(R.string.error_facebook_sign_in_failed),
            isGoogle = false,
            fetchToken
        )

    private suspend fun runSocialSignIn(
        failureMessage: UiText,
        isGoogle: Boolean,
        fetchToken: suspend () -> String
    ) {
        if (_loginUiState.value is LoginUiState.Loading) return
        _loginUiState.value = LoginUiState.Loading
        try {
            val result = try {
                authenticateWithSocialTokenUseCase(fetchToken(), isGoogle = isGoogle)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            result
                .onSuccess {
                    _loginUiState.value = LoginUiState.Success
                    _navigationEvent.send(Unit)
                }
                .onFailure {
                    _loginUiState.value = LoginUiState.Idle
                    _uiEventChannel.send(failureMessage)
                }
        } finally {
            // Canceled sign-in (user backed out): never stay stuck in Loading.
            if (_loginUiState.value is LoginUiState.Loading) _loginUiState.value = LoginUiState.Idle
        }
    }

    private suspend fun handleLoginFailure(error: Throwable) {
        val message = error.toUiText(R.string.error_login_failed)
        _loginUiState.value = LoginUiState.Error(message)
        _uiEventChannel.send(message)
    }
}
```

## auth\presentation\mapper\AuthUiText.kt

```kotlin
package com.mhq.fynecast.auth.presentation.mapper

import androidx.annotation.StringRes
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationException
import com.mhq.fynecast.core.presentation.text.UiText

fun ValidationError.toUiText(): UiText = UiText.res(
    when (this) {
        ValidationError.EmailEmpty -> R.string.error_email_empty
        ValidationError.EmailInvalid -> R.string.error_email_invalid
        ValidationError.PasswordEmpty -> R.string.error_password_empty
        ValidationError.PasswordWeak -> R.string.error_password_weak
        ValidationError.ConfirmPasswordEmpty -> R.string.error_confirm_password_empty
        ValidationError.PasswordsMismatch -> R.string.error_passwords_mismatch
        ValidationError.NameEmpty -> R.string.error_name_empty
        ValidationError.NameInvalidForSignup -> R.string.error_name_invalid_signup
        ValidationError.NameInvalidForProfile -> R.string.error_name_invalid_profile
    }
)

/**
 * Our own typed errors become localized text. Provider errors (Firebase) keep the
 * provider's message, and anything else falls back to [fallbackRes].
 */
fun Throwable.toUiText(@StringRes fallbackRes: Int): UiText =
    (this as? ValidationException)?.error?.toUiText()
        ?: localizedMessage?.takeIf { it.isNotBlank() }?.let { UiText.Dynamic(it) }
        ?: UiText.res(fallbackRes)
```

## auth\presentation\mapper\EditProfilePresentationMapper.kt

```kotlin
package com.mhq.fynecast.auth.presentation.mapper

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.presentation.model.EditProfilePresentationModel

fun UserDomainModel.toEditProfilePresentation(): EditProfilePresentationModel =
    EditProfilePresentationModel(
        name = name,
        email = email,
        profileImageUri = profileImageUri,
        hasPasswordLogin = hasPasswordLogin
    )
```

## auth\presentation\model\EditProfilePresentationModel.kt

```kotlin
package com.mhq.fynecast.auth.presentation.model

/** What the Edit Profile screen needs to seed its fields. The UI never sees the domain model. */
data class EditProfilePresentationModel(
    val name: String,
    val email: String,
    val profileImageUri: String?,
    /** False for Google/Facebook accounts that have no password yet. */
    val hasPasswordLogin: Boolean
)
```

## auth\presentation\signup\SignupUiState.kt

```kotlin
package com.mhq.fynecast.auth.presentation.signup

import com.mhq.fynecast.core.presentation.text.UiText

sealed interface SignupUiState {
    object Idle : SignupUiState
    object Loading : SignupUiState
    object Success : SignupUiState
    data class Error(val message: UiText) : SignupUiState
}
```

## auth\presentation\signup\SignupViewModel.kt

```kotlin
package com.mhq.fynecast.auth.presentation.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.usecases.auth.AuthenticateWithSocialTokenUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.RegisterWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateSignupFormUseCase
import com.mhq.fynecast.auth.presentation.mapper.toUiText
import com.mhq.fynecast.core.presentation.text.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

class SignupViewModel(
    private val validateSignupFormUseCase: ValidateSignupFormUseCase,
    private val registerWithEmailUseCase: RegisterWithEmailUseCase,
    private val authenticateWithSocialTokenUseCase: AuthenticateWithSocialTokenUseCase
) : ViewModel() {

    private val _signupUiState = MutableStateFlow<SignupUiState>(SignupUiState.Idle)
    private val _uiEventChannel = Channel<UiText>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()
    private val _navigationEvent = Channel<Unit>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    /** Pure checks used by the UI to show field errors while typing. */
    fun isUsernameValid(username: String): Boolean =
        validateSignupFormUseCase.validateUsername(username)

    fun isEmailValid(email: String): Boolean =
        validateSignupFormUseCase.validateEmail(email)

    fun isPasswordValid(password: String): Boolean =
        validateSignupFormUseCase.validatePassword(password)

    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean =
        validateSignupFormUseCase.validatePasswordMatch(password, confirmPassword)

    fun onSignupAction(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        val state = _signupUiState.value
        if (state is SignupUiState.Loading || state is SignupUiState.Success) return
        val validation = validateSignupFormUseCase(
            username = username,
            email = email,
            password = password,
            confirmPassword = confirmPassword
        )
        if (validation is ValidationResult.Failure) {
            viewModelScope.launch { _uiEventChannel.send(validation.error.toUiText()) }
            return
        }
        viewModelScope.launch {
            _signupUiState.value = SignupUiState.Loading
            registerWithEmailUseCase(
                username = username,
                email = email,
                password = password
            )
                .onSuccess {
                    _signupUiState.value = SignupUiState.Success
                    _uiEventChannel.send(UiText.res(R.string.msg_account_created))
                    delay(SUCCESS_MESSAGE_DISPLAY_DELAY_MS.milliseconds)
                    _navigationEvent.send(Unit)
                }
                .onFailure { error ->
                    val msg = error.toUiText(R.string.error_signup_failed)
                    _signupUiState.value = SignupUiState.Error(msg)
                    _uiEventChannel.send(msg)
                }
        }
    }

    suspend fun onGoogleAuthenticate(fetchToken: suspend () -> String) =
        runSocialSignIn(
            UiText.res(R.string.error_google_sign_in_failed),
            isGoogle = true,
            fetchToken
        )

    suspend fun onFacebookAuthenticate(fetchToken: suspend () -> String) =
        runSocialSignIn(
            UiText.res(R.string.error_facebook_sign_in_failed),
            isGoogle = false,
            fetchToken
        )

    private suspend fun runSocialSignIn(
        failureMessage: UiText,
        isGoogle: Boolean,
        fetchToken: suspend () -> String
    ) {
        val state = _signupUiState.value
        if (state is SignupUiState.Loading || state is SignupUiState.Success) return
        _signupUiState.value = SignupUiState.Loading
        try {
            val result = try {
                authenticateWithSocialTokenUseCase(fetchToken(), isGoogle = isGoogle)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            result
                .onSuccess {
                    _signupUiState.value = SignupUiState.Success
                    _navigationEvent.send(Unit)
                }
                .onFailure {
                    _signupUiState.value = SignupUiState.Idle
                    _uiEventChannel.send(failureMessage)
                }
        } finally {
            if (_signupUiState.value is SignupUiState.Loading) _signupUiState.value =
                SignupUiState.Idle
        }
    }

    companion object {
        private const val SUCCESS_MESSAGE_DISPLAY_DELAY_MS = 1500L
    }
}
```

## auth\ui\editprofile\ui\components\EditProfileForm.kt

```kotlin
package com.mhq.fynecast.auth.ui.editprofile.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.AuthInputField
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun EditProfileForm(
    username: String,
    isUsernameValid: Boolean,
    onUsernameChanged: (String) -> Unit,
    userEmail: String,
    isEmailValid: Boolean,
    onUserEmailChanged: (String) -> Unit,
    showPasswordFields: Boolean = false,
    newPassword: String = "",
    isNewPasswordValid: Boolean = true,
    onNewPasswordChanged: (String) -> Unit = {},
    confirmPassword: String = "",
    doPasswordsMatch: Boolean = true,
    onConfirmPasswordChanged: (String) -> Unit = {},
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: () -> Unit = {},
    usernameFocusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier
) {

    val focusManager = LocalFocusManager.current

    GlassyCard(
        modifier = modifier.padding(horizontal = 36.dp)
    ) {
        AuthInputField(
            value = username,
            label = stringResource(R.string.name),
            placeholder = stringResource(R.string.enter_name),
            leadingIcon = Icons.Default.Person,
            trailingIcon = null,
            isError = !isUsernameValid,
            onValueChange = onUsernameChanged,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = {
                focusManager.moveFocus(
                    FocusDirection.Down
                )
            }),
            modifier = Modifier.focusRequester(usernameFocusRequester)
        )
        Spacer(
            modifier = Modifier.height(8.dp)
        )
        AuthInputField(
            value = userEmail,
            label = stringResource(R.string.email),
            placeholder = stringResource(R.string.enter_email),
            leadingIcon = Icons.Default.Email,
            trailingIcon = null,
            isError = !isEmailValid,
            onValueChange = onUserEmailChanged,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = {
                focusManager.moveFocus(
                    FocusDirection.Down
                )
            })
        )
        if (showPasswordFields) {
            Text(
                text = stringResource(R.string.email_switch_password_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            val visibilityToggle: @Composable () -> Unit = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = stringResource(
                            if (isPasswordVisible) R.string.hide_password else R.string.show_password
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val passwordTransformation =
                if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()

            AuthInputField(
                value = newPassword,
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.enter_password),
                leadingIcon = Icons.Default.Lock,
                trailingIcon = visibilityToggle,
                isError = !isNewPasswordValid,
                onValueChange = onNewPasswordChanged,
                visualTransformation = passwordTransformation,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                })
            )
            Spacer(modifier = Modifier.height(8.dp))
            AuthInputField(
                value = confirmPassword,
                label = stringResource(R.string.confirm_password),
                placeholder = stringResource(R.string.confirm_password),
                leadingIcon = Icons.Default.Lock,
                trailingIcon = visibilityToggle,
                isError = !doPasswordsMatch,
                onValueChange = onConfirmPasswordChanged,
                visualTransformation = passwordTransformation,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
        }
    }
}

@Preview
@Composable
private fun EditProfileFormPreview() {
    FyneCastTheme() {
        EditProfileForm(
            username = "",
            isUsernameValid = true,
            onUsernameChanged = {},
            userEmail = "",
            isEmailValid = true,
            onUserEmailChanged = {}
        )
    }
}
```

## auth\ui\editprofile\ui\components\EditProfileHeader.kt

```kotlin
package com.mhq.fynecast.auth.ui.editprofile.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.AbyssalVoid
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun EditProfileHeader(
    profileImageUri: String?,
    onProfileImagePicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    val imageModel = profileImageUri ?: R.drawable.user

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onProfileImagePicked(uri.toString())
        }
    }

    Box(
        modifier = modifier
            .padding(top = 24.dp, bottom = 36.dp)
            .size(100.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        AsyncImage(
            model = imageModel,
            placeholder = painterResource(R.drawable.user),
            contentDescription = stringResource(R.string.profile_photo),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AbyssalVoid)
                    .border(1.dp, ElectricLimeGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.change_photo),
                    tint = ElectricLimeGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun EditProfileHeaderPreview() {
    FyneCastTheme() {
        EditProfileHeader(
            profileImageUri = null,
            onProfileImagePicked = {},
        )
    }
}
```

## auth\ui\editprofile\ui\screens\EditProfileContainer.kt

```kotlin
package com.mhq.fynecast.auth.ui.editprofile.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.auth.presentation.editprofile.EditProfileViewModel
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import kotlinx.coroutines.launch

@Composable
fun EditProfileContainer(
    editProfileViewModel: EditProfileViewModel = viewModel(factory = AppViewModelFactory),
    onBackClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val initialProfile by editProfileViewModel.initialProfile.collectAsStateWithLifecycle()
    var profileImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var username by rememberSaveable { mutableStateOf("") }
    var userEmail by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isSeeded by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(initialProfile) {
        val profile = initialProfile ?: return@LaunchedEffect
        if (!isSeeded) {
            username = profile.name
            userEmail = profile.email
            profileImageUri = profile.profileImageUri
            isSeeded = true
        }
    }

    val isUsernameValid = editProfileViewModel.isUsernameValid(username)
    val isEmailValid = editProfileViewModel.isEmailValid(userEmail)
    // Social accounts have no password yet: switching the email needs one for the new email.
    val showPasswordFields = initialProfile?.let {
        !it.hasPasswordLogin && !userEmail.equals(it.email, ignoreCase = true)
    } ?: false
    val isNewPasswordValid = editProfileViewModel.isPasswordValid(newPassword)
    val doPasswordsMatch = editProfileViewModel.doPasswordsMatch(newPassword, confirmPassword)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        editProfileViewModel.navigationEvent.collect {
            onBackClick()
        }
    }

    LaunchedEffect(Unit) {
        editProfileViewModel.uiEvent.collect { errorMessage ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = errorMessage.asString(context),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        EditProfileContent(
            username = username,
            userEmail = userEmail,
            isUsernameValid = isUsernameValid,
            isEmailValid = isEmailValid,
            onUsernameChanged = { username = it },
            onUserEmailChanged = { userEmail = it.trim() },
            showPasswordFields = showPasswordFields,
            newPassword = newPassword,
            isNewPasswordValid = isNewPasswordValid,
            onNewPasswordChanged = { newPassword = it },
            confirmPassword = confirmPassword,
            doPasswordsMatch = doPasswordsMatch,
            onConfirmPasswordChanged = { confirmPassword = it },
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
            onBackClick = onBackClick,
            onSaveClick = {
                editProfileViewModel.onSaveProfileAction(
                    username,
                    userEmail,
                    profileImageUri,
                    newPassword.takeIf { showPasswordFields },
                    confirmPassword.takeIf { showPasswordFields }
                )
            },
            contentPadding = contentPadding,
            modifier = modifier,
            profileImageUri = profileImageUri,
            onProfileImagePicked = {
                profileImageUri = it
            }
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .padding(horizontal = 24.dp)
        ) { data ->
            AuthWarningSnackbar(data)
        }
    }
}
```

## auth\ui\editprofile\ui\screens\EditProfileContent.kt

```kotlin
package com.mhq.fynecast.auth.ui.editprofile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.ui.editprofile.ui.components.EditProfileForm
import com.mhq.fynecast.auth.ui.editprofile.ui.components.EditProfileHeader
import com.mhq.fynecast.core.ui.globalcomponents.AuthActionButton

@Composable
fun EditProfileContent(
    username: String,
    isUsernameValid: Boolean,
    onUsernameChanged: (String) -> Unit,
    userEmail: String,
    isEmailValid: Boolean,
    onUserEmailChanged: (String) -> Unit,
    profileImageUri: String?,
    showPasswordFields: Boolean = false,
    newPassword: String = "",
    isNewPasswordValid: Boolean = true,
    onNewPasswordChanged: (String) -> Unit = {},
    confirmPassword: String = "",
    doPasswordsMatch: Boolean = true,
    onConfirmPasswordChanged: (String) -> Unit = {},
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: () -> Unit = {},
    onProfileImagePicked: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {

    val usernameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        usernameFocusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp),
        ) {
            // 1. Customized Top Action Navigation Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(R.string.edit_profile),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
            EditProfileHeader(
                profileImageUri = profileImageUri,
                onProfileImagePicked = onProfileImagePicked,
                modifier = Modifier
            )
            EditProfileForm(
                username = username,
                userEmail = userEmail,
                isUsernameValid = isUsernameValid,
                isEmailValid = isEmailValid,
                onUsernameChanged = onUsernameChanged,
                onUserEmailChanged = onUserEmailChanged,
                showPasswordFields = showPasswordFields,
                newPassword = newPassword,
                isNewPasswordValid = isNewPasswordValid,
                onNewPasswordChanged = onNewPasswordChanged,
                confirmPassword = confirmPassword,
                doPasswordsMatch = doPasswordsMatch,
                onConfirmPasswordChanged = onConfirmPasswordChanged,
                isPasswordVisible = isPasswordVisible,
                onTogglePasswordVisibility = onTogglePasswordVisibility,
                usernameFocusRequester = usernameFocusRequester,
            )
            AuthActionButton(
                text = stringResource(R.string.save_changes),
                onDoAction = { onSaveClick() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    EditProfileContent(
        username = "JohnDoe",
        isUsernameValid = true,
        onUsernameChanged = {},
        userEmail = "johndoe@gmail.com",
        isEmailValid = true,
        onUserEmailChanged = {},
        profileImageUri = null,
        onProfileImagePicked = {},
        onSaveClick = {},
        onBackClick = {},
        contentPadding = PaddingValues(0.dp)
    )
}
```

## auth\ui\forgotpassword\ui\components\AuthForgotPasswordForm.kt

```kotlin
package com.mhq.fynecast.auth.ui.forgotpassword.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.AuthInputField
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AuthForgotPasswordForm(
    email: String,
    isEmailValid: Boolean,
    onEmailChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {

    val focusManager = LocalFocusManager.current

    GlassyCard(
        modifier = modifier.padding(horizontal = 36.dp)
    ) {
        Text(
            text = stringResource(R.string.recover_password),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(
            modifier = Modifier.height(8.dp)
        )
        Text(
            text = stringResource(R.string.enter_the_email_associated_with_your_account),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.we_will_send_a_link_to_reset_your_password),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        AuthInputField(
            value = email,
            label = stringResource(R.string.email),
            placeholder = stringResource(R.string.enter_email),
            leadingIcon = Icons.Default.Email,
            trailingIcon = null,
            isError = !isEmailValid,
            onValueChange = onEmailChanged,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview
@Composable
private fun AuthForgotPasswordFormPreview() {
    FyneCastTheme() {
        AuthForgotPasswordForm(
            email = "",
            isEmailValid = true,
            onEmailChanged = {}
        )
    }
}
```

## auth\ui\forgotpassword\ui\screens\ForgotPasswordContainer.kt

```kotlin
package com.mhq.fynecast.auth.ui.forgotpassword.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.auth.presentation.forgotpassword.ForgotPasswordViewModel
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ForgotPasswordContainer(
    forgotPasswordViewModel: ForgotPasswordViewModel = viewModel(factory = AppViewModelFactory),
    onSendLinkSuccessNavigate: () -> Unit,
    onBackToLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    val isEmailValid = forgotPasswordViewModel.isEmailValid(email)

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        forgotPasswordViewModel.navigationEvent.collect { onSendLinkSuccessNavigate() }
    }

    LaunchedEffect(Unit) {
        forgotPasswordViewModel.uiEvent.collectLatest { errorMessage ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = errorMessage.asString(context),
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ForgotPasswordContent(
            email = email,
            isEmailValid = isEmailValid,
            onEmailChanged = { email = it.trim() },
            onBackToLoginClick = onBackToLoginClick,
            onSendLink = { forgotPasswordViewModel.onPasswordResetAction(email) },            modifier = modifier
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .padding(horizontal = 24.dp)
        ) { snackbarData ->
            AuthWarningSnackbar(snackbarData)
        }
    }
}
```

## auth\ui\forgotpassword\ui\screens\ForgotPasswordContent.kt

```kotlin
package com.mhq.fynecast.auth.ui.forgotpassword.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.ui.forgotpassword.ui.components.AuthForgotPasswordForm
import com.mhq.fynecast.core.ui.globalcomponents.AppName
import com.mhq.fynecast.core.ui.globalcomponents.AuthActionButton
import com.mhq.fynecast.core.ui.globalcomponents.AuthFooterHyperlink

@Composable
fun ForgotPasswordContent(
    email: String,
    isEmailValid: Boolean,
    onEmailChanged: (String) -> Unit,
    onBackToLoginClick: () -> Unit,
    onSendLink: () -> Unit,
    modifier: Modifier = Modifier
) {

    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 24.dp)
    ) {
        AppName(
            text = stringResource(R.string.app_name)
        )
        AuthForgotPasswordForm(
            email = email,
            isEmailValid = isEmailValid,
            onEmailChanged = onEmailChanged,
            focusRequester = focusRequester,
        )
        AuthActionButton(
            text = stringResource(R.string.send_link),
            onDoAction = { onSendLink() },
        )
        AuthFooterHyperlink(
            question = stringResource(R.string.remembered_credentials),
            answer = stringResource(R.string.login),
            onDoAction = { onBackToLoginClick() }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordContentPreview() {
    ForgotPasswordContent(
        email = "developer@fynecast.com",
        isEmailValid = true,
        onEmailChanged = {},
        onBackToLoginClick = {},
        onSendLink = {}
    )
}
```

## auth\ui\login\ui\components\AuthFormLogin.kt

```kotlin
package com.mhq.fynecast.auth.ui.login.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.AuthInputField
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.core.ui.theme.AbyssalVoid

@Composable
fun AuthFormLogin(
    email: String,
    isEmailValid: Boolean,
    onEmailChanged: (String) -> Unit,
    password: String,
    onPasswordChanged: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    GlassyCard(
        modifier = modifier.padding(horizontal = 36.dp)
    ) {
        AuthInputField(
            value = email,
            label = stringResource(R.string.email),
            placeholder = stringResource(R.string.enter_email),
            leadingIcon = Icons.Default.Email,
            trailingIcon = null,
            isError = !isEmailValid,
            onValueChange = onEmailChanged,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.focusRequester(focusRequester)
        )
        AuthInputField(
            value = password,
            label = stringResource(R.string.password),
            placeholder = stringResource(R.string.enter_password),
            leadingIcon = Icons.Default.Lock,
            trailingIcon = {
                val image =
                    if (isPasswordVisible)
                        Icons.Default.Visibility
                    else
                        Icons.Default.VisibilityOff
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = image,
                        contentDescription = stringResource(
                            if (isPasswordVisible) R.string.hide_password else R.string.show_password
                        ),
                        tint = AbyssalVoid
                    )
                }
            },
            isError = false,
            onValueChange = onPasswordChanged,
            visualTransformation =
                if (isPasswordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.forgot_password),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.clickable { onForgotPasswordClick() }
            )
        }
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview
@Composable
private fun AuthFormLoginPreview() {
    FyneCastTheme() {
        AuthFormLogin(
            email = "username@gmail.com",
            password = "12345",
            isPasswordVisible = true,
            isEmailValid = true,
            onEmailChanged = {},
            onPasswordChanged = {},
            onTogglePasswordVisibility = {},
            onForgotPasswordClick = {}
        )
    }
}
```

## auth\ui\login\ui\screens\LoginContainer.kt

```kotlin
package com.mhq.fynecast.auth.ui.login.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.auth.presentation.login.LoginViewModel
import com.mhq.fynecast.auth.ui.social.SocialTokenFetcher
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun LoginContainer(
    loginViewModel: LoginViewModel = viewModel(factory = AppViewModelFactory),
    onLoginClick: () -> Unit,
    onSignUpNavigate: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    val isEmailValid = loginViewModel.isEmailValid(email)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        loginViewModel.navigationEvent.collect { onLoginClick() }
    }

    LaunchedEffect(Unit) {
        loginViewModel.uiEvent.collectLatest { errorMessage ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = errorMessage.asString(context),
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LoginContent(
            email = email,
            isEmailValid = isEmailValid,
            onEmailChanged = { email = it.trim() },
            password = password,
            onPasswordChanged = { password = it },
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
            onSignUpClick = onSignUpNavigate,
            onLoginClick = { loginViewModel.onLoginAction(email, password) },            onForgotPasswordClick = onForgotPasswordClick,
            onGoogleAuthenticate = {
                scope.launch {
                    loginViewModel.onGoogleAuthenticate {
                        SocialTokenFetcher.getGoogleIdToken(context)
                    }
                }
            },
            onFacebookAuthenticate = {
                (context as? ComponentActivity)?.let { activity ->
                    scope.launch {
                        loginViewModel.onFacebookAuthenticate {
                            SocialTokenFetcher.getFacebookAccessToken(activity)
                        }
                    }
                }
            },
            modifier = modifier
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .padding(horizontal = 24.dp)
        ) { snackbarData -> AuthWarningSnackbar(snackbarData) }
    }
}
```

## auth\ui\login\ui\screens\LoginContent.kt

```kotlin
package com.mhq.fynecast.auth.ui.login.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.ui.login.ui.components.AuthFormLogin
import com.mhq.fynecast.core.ui.globalcomponents.AppName
import com.mhq.fynecast.core.ui.globalcomponents.AuthActionButton
import com.mhq.fynecast.core.ui.globalcomponents.AuthFooterHyperlink
import com.mhq.fynecast.core.ui.globalcomponents.AuthSocialOptions
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun LoginContent(
    email: String,
    isEmailValid: Boolean,
    onEmailChanged: (String) -> Unit,
    password: String,
    onPasswordChanged: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onGoogleAuthenticate: () -> Unit,
    onFacebookAuthenticate: () -> Unit,
    modifier: Modifier = Modifier
) {

    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
            .padding(vertical = 24.dp)
    ) {
        AppName(
            text = stringResource(R.string.app_name)
        )
        AuthFormLogin(
            email = email,
            isEmailValid = isEmailValid,
            onEmailChanged = onEmailChanged,
            password = password,
            onPasswordChanged = onPasswordChanged,
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            onForgotPasswordClick = onForgotPasswordClick,
        )
        AuthActionButton(
            text = stringResource(R.string.login),
            onDoAction = { onLoginClick() }
        )
        AuthSocialOptions(
            onGoogleAuthenticate = { onGoogleAuthenticate() },
            onFacebookAuthenticate = { onFacebookAuthenticate() }
        )
        AuthFooterHyperlink(
            question = stringResource(R.string.don_t_have_an_account),
            answer = stringResource(R.string.sign_up),
            onDoAction = { onSignUpClick() }
        )
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    FyneCastTheme() {
        LoginContent(
            email = "",
            isEmailValid = true,
            onEmailChanged = {},
            password = "",
            onPasswordChanged = {},
            isPasswordVisible = true,
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onSignUpClick = {},
            onForgotPasswordClick = {},
            onGoogleAuthenticate = {},
            onFacebookAuthenticate = {}
        )
    }
}
```

## auth\ui\signup\ui\components\AuthFormSignup.kt

```kotlin
package com.mhq.fynecast.auth.ui.signup.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.AuthInputField
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AuthFormSignup(
    username: String,
    isUsernameValid: Boolean,
    onUsernameChanged: (String) -> Unit,
    userEmail: String,
    isEmailValid: Boolean,
    onUserEmailChanged: (String) -> Unit,
    userPassword: String,
    isPasswordValid: Boolean,
    onUserPasswordChanged: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChanged: (String) -> Unit,
    doPasswordsMatch: Boolean,
    arePasswordsVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {

    val focusManager = LocalFocusManager.current
    val emailFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    GlassyCard(
        modifier = modifier.padding(horizontal = 36.dp)
    ) {
        AuthInputField(
            value = username,
            label = stringResource(R.string.name),
            placeholder = stringResource(R.string.enter_name),
            leadingIcon = Icons.Default.Person,
            trailingIcon = null,
            isError = !isUsernameValid,
            onValueChange = onUsernameChanged,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { usernameFocusRequester.requestFocus() }
            )
        )
        AuthInputField(
            value = userEmail,
            label = stringResource(R.string.email),
            placeholder = stringResource(R.string.enter_email),
            leadingIcon = Icons.Default.Email,
            trailingIcon = null,
            isError = !isEmailValid,
            onValueChange = onUserEmailChanged,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { emailFocusRequester.requestFocus() }
            ),
            modifier = Modifier.focusRequester(usernameFocusRequester)
        )
        AuthInputField(
            value = userPassword,
            label = stringResource(R.string.password),
            placeholder = stringResource(R.string.enter_password),
            leadingIcon = Icons.Default.Lock,
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector =
                            if (arePasswordsVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                        contentDescription = stringResource(
                            if (arePasswordsVisible) R.string.hide_password else R.string.show_password
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            isError = !isPasswordValid,
            onValueChange = onUserPasswordChanged,
            visualTransformation =
                if (arePasswordsVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions =
                KeyboardActions(
                    onNext = { passwordFocusRequester.requestFocus() }
                ),
            modifier = Modifier.focusRequester(emailFocusRequester)
        )
        AuthInputField(
            value = confirmPassword,
            label = stringResource(R.string.confirm_password),
            placeholder = stringResource(R.string.confirm_password),
            leadingIcon = Icons.Default.Lock,
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector =
                            if (arePasswordsVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                        contentDescription = stringResource(
                            if (arePasswordsVisible) R.string.hide_password else R.string.show_password
                        ),
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            },
            isError = !doPasswordsMatch,
            onValueChange = onConfirmPasswordChanged,
            visualTransformation =
                if (arePasswordsVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier.focusRequester(passwordFocusRequester)
        )
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview(showBackground = true)
@Composable
private fun AuthFormSignupPreview() {
    FyneCastTheme() {
        AuthFormSignup(
            username = "Username",
            userEmail = "username@gmail.com",
            userPassword = "123abc",
            confirmPassword = "123abc",
            isUsernameValid = true,
            isEmailValid = true,
            isPasswordValid = true,
            doPasswordsMatch = true,
            arePasswordsVisible = true,
            onUsernameChanged = {},
            onUserEmailChanged = {},
            onUserPasswordChanged = {},
            onConfirmPasswordChanged = {},
            onTogglePasswordVisibility = {}
        )
    }
}
```

## auth\ui\signup\ui\screens\SignupContainer.kt

```kotlin
package com.mhq.fynecast.auth.ui.signup.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.auth.presentation.signup.SignupViewModel
import com.mhq.fynecast.auth.ui.social.SocialTokenFetcher
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SignupContainer(
    signupViewModel: SignupViewModel = viewModel(factory = AppViewModelFactory),
    onSignupClick: () -> Unit,
    onLoginNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userName by rememberSaveable { mutableStateOf("") }
    var userEmail by rememberSaveable { mutableStateOf("") }
    var userPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var arePasswordsVisible by rememberSaveable { mutableStateOf(false) }

    val isUsernameValid = signupViewModel.isUsernameValid(userName)
    val isEmailValid = signupViewModel.isEmailValid(userEmail)
    val isPasswordValid = signupViewModel.isPasswordValid(userPassword)
    val doPasswordsMatch = signupViewModel.doPasswordsMatch(userPassword, confirmPassword)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        signupViewModel.navigationEvent.collect { onSignupClick() }
    }

    LaunchedEffect(Unit) {
        signupViewModel.uiEvent.collectLatest { errorMessage ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = errorMessage.asString(context),
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        SignupContent(
            username = userName,
            isUsernameValid = isUsernameValid,
            onUsernameChanged = { userName = it },
            email = userEmail,
            isEmailValid = isEmailValid,
            onEmailChanged = { userEmail = it.trim() },
            password = userPassword,
            isPasswordValid = isPasswordValid,
            onPasswordChanged = { userPassword = it },
            confirmPassword = confirmPassword,
            onConfirmPasswordChanged = { confirmPassword = it },
            doPasswordsMatch = doPasswordsMatch,
            arePasswordsVisible = arePasswordsVisible,
            onTogglePasswordVisibility = { arePasswordsVisible = !arePasswordsVisible },            onGoogleAuthenticate = {
                scope.launch {
                    signupViewModel.onGoogleAuthenticate {
                        SocialTokenFetcher.getGoogleIdToken(context)
                    }
                }
            },
            onFacebookAuthenticate = {
                (context as? ComponentActivity)?.let { activity ->
                    scope.launch {
                        signupViewModel.onFacebookAuthenticate {
                            SocialTokenFetcher.getFacebookAccessToken(activity)
                        }
                    }
                }
            },
            onLoginClick = onLoginNavigate,
            onSignupClick = {
                signupViewModel.onSignupAction(userName, userEmail, userPassword, confirmPassword)
            },
            modifier = modifier
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .padding(horizontal = 24.dp)
        ) { data -> AuthWarningSnackbar(data) }
    }
}
```

## auth\ui\signup\ui\screens\SignupContent.kt

```kotlin
package com.mhq.fynecast.auth.ui.signup.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.ui.signup.ui.components.AuthFormSignup
import com.mhq.fynecast.core.ui.globalcomponents.AppName
import com.mhq.fynecast.core.ui.globalcomponents.AuthActionButton
import com.mhq.fynecast.core.ui.globalcomponents.AuthFooterHyperlink
import com.mhq.fynecast.core.ui.globalcomponents.AuthSocialOptions
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun SignupContent(
    username: String,
    isUsernameValid: Boolean,
    onUsernameChanged: (String) -> Unit,
    email: String,
    isEmailValid: Boolean,
    onEmailChanged: (String) -> Unit,
    password: String,
    onPasswordChanged: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChanged: (String) -> Unit,
    isPasswordValid: Boolean,
    doPasswordsMatch: Boolean,
    arePasswordsVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onSignupClick: () -> Unit,
    onLoginClick: () -> Unit,
    onGoogleAuthenticate: () -> Unit,
    onFacebookAuthenticate: () -> Unit,
    modifier: Modifier = Modifier
) {

    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
    ) {
        AppName(
            text = stringResource(R.string.app_name)
        )
        AuthFormSignup(
            username = username,
            isUsernameValid = isUsernameValid,
            onUsernameChanged = onUsernameChanged,
            userEmail = email,
            isEmailValid = isEmailValid,
            onUserEmailChanged = onEmailChanged,
            userPassword = password,
            isPasswordValid = isPasswordValid,
            onUserPasswordChanged = onPasswordChanged,
            confirmPassword = confirmPassword,
            onConfirmPasswordChanged = onConfirmPasswordChanged,
            doPasswordsMatch = doPasswordsMatch,
            arePasswordsVisible = arePasswordsVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility
        )
        AuthActionButton(
            text = stringResource(R.string.sign_up),
            onDoAction = onSignupClick
        )
        AuthSocialOptions(
            onGoogleAuthenticate = onGoogleAuthenticate,
            onFacebookAuthenticate = onFacebookAuthenticate
        )
        AuthFooterHyperlink(
            question = stringResource(R.string.already_have_an_account),
            answer = stringResource(R.string.login),
            onDoAction = onLoginClick
        )
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview(showBackground = true)
@Composable
fun SignupScreenScreenPreview() {
    FyneCastTheme() {
        SignupContent(
            username = "",
            isUsernameValid = true,
            onUsernameChanged = {},
            email = "",
            onEmailChanged = {},
            isEmailValid = true,
            password = "",
            isPasswordValid = true,
            onPasswordChanged = {},
            confirmPassword = "",
            onConfirmPasswordChanged = {},
            doPasswordsMatch = true,
            arePasswordsVisible = true,
            onTogglePasswordVisibility = {},
            onSignupClick = {},
            onLoginClick = {},
            onGoogleAuthenticate = {},
            onFacebookAuthenticate = {}
        )
    }
}
```

## auth\ui\social\SocialTokenFetcher.kt

```kotlin
package com.mhq.fynecast.auth.ui.social

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mhq.fynecast.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Gets provider tokens. Needs an Activity/Context,
 * so it lives in the UI layer. */
object SocialTokenFetcher {

    suspend fun getGoogleIdToken(context: Context): String {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = credentialManager.getCredential(context, request)
        return GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    }

    suspend fun getFacebookAccessToken(activity: ComponentActivity): String =
        suspendCancellableCoroutine { continuation ->
            val loginManager = LoginManager.getInstance()
            val callbackManager = CallbackManager.Factory.create()

            loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    loginManager.unregisterCallback(callbackManager)
                    continuation.resume(result.accessToken.token)
                }

                override fun onCancel() {
                    loginManager.unregisterCallback(callbackManager)
                    continuation.cancel()
                }

                override fun onError(error: FacebookException) {
                    loginManager.unregisterCallback(callbackManager)
                    continuation.resumeWithException(error)
                }
            })

            continuation.invokeOnCancellation {
                loginManager.unregisterCallback(callbackManager)
            }

            loginManager.logInWithReadPermissions(
                activity,
                callbackManager,
                listOf("public_profile", "email")
            )
        }
}
```

## core\data\infra\AppInitializerImpl.kt

```kotlin
package com.mhq.fynecast.core.data.infra

import android.app.Application
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Base64
import android.util.Log
import com.facebook.FacebookSdk
import com.mhq.fynecast.core.domain.infra.AppInitializer
import java.security.MessageDigest

class AppInitializerImpl(
    private val application: Application
) : AppInitializer {

    override fun initializeDebugFeatures() {
        FacebookSdk.setIsDebugEnabled(true)
        printFacebookKeyHash()
    }

    private fun printFacebookKeyHash() {
        try {
            for (signature in getSigningSignatures()) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                Log.d("AppInitializer", "Facebook key hash: $hashKey")
            }
        } catch (e: Exception) {
            Log.w("AppInitializer", "Could not read the signing certificate", e)
        }
    }

    /**
     * Reads the app's signing certificates using the non-deprecated API for
     * the running OS version, falling back to the old signature API only on
     * versions where no replacement exists.
     */
    private fun getSigningSignatures(): Array<Signature> {
        val packageName = application.packageName
        val packageManager = application.packageManager

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val flags = PackageManager.PackageInfoFlags.of(
                    PackageManager.GET_SIGNING_CERTIFICATES.toLong()
                )
                val signingInfo = packageManager.getPackageInfo(packageName, flags).signingInfo
                signingInfo?.let {
                    if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory
                } ?: emptyArray()
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                @Suppress("DEPRECATION")
                val signingInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo
                signingInfo?.let {
                    if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory
                } ?: emptyArray()
            }

            else -> {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                ).signatures ?: emptyArray()
            }
        }
    }
}
```

## core\data\infra\FirebaseCurrentUserProvider.kt

```kotlin
package com.mhq.fynecast.core.data.infra

import com.google.firebase.auth.FirebaseAuth
import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class FirebaseCurrentUserProvider(
    private val firebaseAuth: FirebaseAuth
) : CurrentUserProvider {

    override fun observeUserId(): Flow<String?> = callbackFlow {
        val firebaseAuthAuthStateListener =
            FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }

        firebaseAuth.addAuthStateListener(firebaseAuthAuthStateListener)
        awaitClose { firebaseAuth.removeAuthStateListener(firebaseAuthAuthStateListener) }
    }.distinctUntilChanged()

    override fun currentUserId(): String? = firebaseAuth.currentUser?.uid
}
```

## core\data\location\SelectedCityRepositoryImpl.kt

```kotlin
package com.mhq.fynecast.core.data.location

import com.mhq.fynecast.core.domain.location.SelectedCity
import com.mhq.fynecast.core.domain.location.SelectedCityRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class SelectedCityRepositoryImpl : SelectedCityRepository {
    private val channel = Channel<SelectedCity>(Channel.BUFFERED)
    override val selections: Flow<SelectedCity> = channel.receiveAsFlow()
    override suspend fun select(city: SelectedCity) = channel.send(city)
}
```

## core\data\time\SystemTimeProvider.kt

```kotlin
package com.mhq.fynecast.core.data.time

import com.mhq.fynecast.core.domain.time.TimeProvider

class SystemTimeProvider : TimeProvider {
    override fun nowMillis() = System.currentTimeMillis()
}
```

## core\di\AppContainer.kt

```kotlin
package com.mhq.fynecast.core.di

import com.mhq.fynecast.alerts.domain.usecases.CancelAllAlertTrackingUseCase
import com.mhq.fynecast.alerts.domain.usecases.CheckAlertConditionsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetAlertsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertIdsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertStatusUseCase
import com.mhq.fynecast.alerts.domain.usecases.RefreshTrackingNotificationUseCase
import com.mhq.fynecast.alerts.domain.usecases.RestoreAlertTrackingUseCase
import com.mhq.fynecast.alerts.domain.usecases.ToggleAlertsUseCase
import com.mhq.fynecast.alerts.domain.usecases.TrackAlertUseCase
import com.mhq.fynecast.alerts.domain.usecases.UntrackAlertUseCase
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.alerts.notifications.domain.usecases.ConsumeNotificationPromptUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.SyncNotificationPermissionUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.AuthenticateWithSocialTokenUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.CompleteAuthSessionUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.LoginWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.RegisterWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.SendPasswordResetUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateLoginFormUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateSignupFormUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ObserveUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.RefreshUserSessionUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.UpdateUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ValidateEditProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.session.CheckUserLoggedInUseCase
import com.mhq.fynecast.auth.domain.usecases.session.LogoutUserUseCase
import com.mhq.fynecast.core.domain.infra.AppInitializer
import com.mhq.fynecast.core.domain.location.ObserveSelectedCityUseCase
import com.mhq.fynecast.core.domain.location.SelectCityUseCase
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.KeepFavoriteNamesLocalizedUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshAllFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshFavoriteSnapshotUseCase
import com.mhq.fynecast.favorites.domain.usecases.RestoreFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.ToggleFavoriteUseCase
import com.mhq.fynecast.home.domain.usecases.GetCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.GetLastSearchedCityUseCase
import com.mhq.fynecast.home.domain.usecases.GetLocationSuggestionsUseCase
import com.mhq.fynecast.home.domain.usecases.GetWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.LoadCurrentWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.LocalizePlaceNameUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveFavoriteStatusUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveSignedInUserIdUseCase
import com.mhq.fynecast.home.domain.usecases.ResolveStartupLocationUseCase
import com.mhq.fynecast.map.domain.usecases.ReverseGeocodeLocationUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SaveLastSearchedCityUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetDarkModeUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetLanguageUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetUnitSystemUseCase

interface AppContainer {

    val appInitializer: AppInitializer

    val getWeatherUseCase: GetWeatherUseCase
    val loadCurrentWeatherUseCase: LoadCurrentWeatherUseCase
    val observeCurrentLocationUseCase: ObserveCurrentLocationUseCase

    // --- Authentication & Profile Use Cases ---
    val validateSignupFormUseCase: ValidateSignupFormUseCase
    val validateLoginFormUseCase: ValidateLoginFormUseCase
    val registerWithEmailUseCase: RegisterWithEmailUseCase
    val loginWithEmailUseCase: LoginWithEmailUseCase
    val authenticateWithSocialTokenUseCase: AuthenticateWithSocialTokenUseCase
    val completeAuthSessionUseCase: CompleteAuthSessionUseCase
    val sendPasswordResetUseCase: SendPasswordResetUseCase
    val updateUserProfileUseCase: UpdateUserProfileUseCase
    val validateEditProfileUseCase: ValidateEditProfileUseCase
    val observeUserProfileUseCase: ObserveUserProfileUseCase
    val refreshUserSessionUseCase: RefreshUserSessionUseCase
    val observeSignedInUserIdUseCase: ObserveSignedInUserIdUseCase
    val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase
    val logoutUserUseCase: LogoutUserUseCase
    val checkUserLoggedInUseCase: CheckUserLoggedInUseCase
    val setUnitSystemUseCase: SetUnitSystemUseCase
    val setLanguageUseCase: SetLanguageUseCase
    val setDarkModeUseCase: SetDarkModeUseCase
    val saveLastSearchedCityUseCase: SaveLastSearchedCityUseCase

    // Location Use Cases
    val getCurrentLocationUseCase: GetCurrentLocationUseCase
    val reverseGeocodeLocationUseCase: ReverseGeocodeLocationUseCase
    val getLocationSuggestionsUseCase: GetLocationSuggestionsUseCase
    val getLastSearchedCityUseCase: GetLastSearchedCityUseCase
    val resolveStartupLocationUseCase: ResolveStartupLocationUseCase
    val selectCityUseCase: SelectCityUseCase
    val observeSelectedCityUseCase: ObserveSelectedCityUseCase
    val localizePlaceNameUseCase: LocalizePlaceNameUseCase
    val refreshAllFavoritesUseCase: RefreshAllFavoritesUseCase
    val keepFavoriteNamesLocalizedUseCase: KeepFavoriteNamesLocalizedUseCase

    // Favorites Use Cases
    val getFavoritesUseCase: GetFavoritesUseCase
    val deleteFavoriteUseCase: DeleteFavoriteUseCase
    val toggleFavoriteUseCase: ToggleFavoriteUseCase
    val observeFavoriteStatusUseCase: ObserveFavoriteStatusUseCase
    val refreshFavoriteSnapshotUseCase: RefreshFavoriteSnapshotUseCase
    val restoreFavoriteUseCase: RestoreFavoriteUseCase

    // Alerts Use Cases
    val getAlertsUseCase: GetAlertsUseCase
    val toggleAlertsUseCase: ToggleAlertsUseCase
    val trackAlertUseCase: TrackAlertUseCase
    val untrackAlertUseCase: UntrackAlertUseCase
    val cancelAllAlertTrackingUseCase: CancelAllAlertTrackingUseCase
    val checkAlertConditionsUseCase: CheckAlertConditionsUseCase
    val getTrackedAlertIdsUseCase: GetTrackedAlertIdsUseCase
    val getTrackedAlertStatusUseCase: GetTrackedAlertStatusUseCase
    val restoreAlertTrackingUseCase: RestoreAlertTrackingUseCase
    val refreshTrackingNotificationUseCase: RefreshTrackingNotificationUseCase

    // Notifications Use Cases
    val notificationPermissionChecker: NotificationPermissionChecker
    val toggleNotificationsUseCase: ToggleNotificationsUseCase
    val handleNotificationPermissionResultUseCase: HandleNotificationPermissionResultUseCase
    val syncNotificationPermissionUseCase: SyncNotificationPermissionUseCase
    val consumeNotificationPromptUseCase: ConsumeNotificationPromptUseCase
}
```

## core\di\AppViewModelFactory.kt

```kotlin
package com.mhq.fynecast.core.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.alerts.presentation.AlertsViewModel
import com.mhq.fynecast.auth.presentation.editprofile.EditProfileViewModel
import com.mhq.fynecast.auth.presentation.forgotpassword.ForgotPasswordViewModel
import com.mhq.fynecast.auth.presentation.login.LoginViewModel
import com.mhq.fynecast.auth.presentation.signup.SignupViewModel
import com.mhq.fynecast.core.presentation.SessionViewModel
import com.mhq.fynecast.favorites.presentation.FavoritesViewModel
import com.mhq.fynecast.home.presentation.HomeViewModel
import com.mhq.fynecast.map.presentation.MapPickerViewModel
import com.mhq.fynecast.settings.presentation.SettingsViewModel

private fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as FyneCastApplication).container

/** The single place where ViewModels are wired to the DI container. */
val AppViewModelFactory: ViewModelProvider.Factory = viewModelFactory {

    initializer {
        val container = container()
        AlertsViewModel(
            getAlertsUseCase = container.getAlertsUseCase,
            getTrackedAlertIdsUseCase = container.getTrackedAlertIdsUseCase,
            observeCurrentLocationUseCase = container.observeCurrentLocationUseCase,
            toggleAlertsUseCase = container.toggleAlertsUseCase,
            toggleNotificationsUseCase = container.toggleNotificationsUseCase,
            handleNotificationPermissionResultUseCase = container.handleNotificationPermissionResultUseCase,
        )
    }

    initializer {
        val container = container()
        EditProfileViewModel(
            observeUserProfileUseCase = container.observeUserProfileUseCase,
            validateEditProfileUseCase = container.validateEditProfileUseCase,
            updateUserProfileUseCase = container.updateUserProfileUseCase
        )
    }

    initializer {
        val container = container()
        ForgotPasswordViewModel(
            validateLoginFormUseCase = container.validateLoginFormUseCase,
            sendPasswordResetUseCase = container.sendPasswordResetUseCase
        )
    }

    initializer {
        val container = container()
        LoginViewModel(
            loginWithEmailUseCase = container.loginWithEmailUseCase,
            validateLoginFormUseCase = container.validateLoginFormUseCase,
            authenticateWithSocialTokenUseCase = container.authenticateWithSocialTokenUseCase
        )
    }

    initializer {
        val container = container()
        SignupViewModel(
            validateSignupFormUseCase = container.validateSignupFormUseCase,
            registerWithEmailUseCase = container.registerWithEmailUseCase,
            authenticateWithSocialTokenUseCase = container.authenticateWithSocialTokenUseCase
        )
    }

    initializer {
        SessionViewModel(
            checkUserLoggedIn = container().checkUserLoggedInUseCase,
            refreshUserSession = container().refreshUserSessionUseCase
        )
    }

    initializer {
        val container = container()
        FavoritesViewModel(
            getFavoritesUseCase = container.getFavoritesUseCase,
            deleteFavoriteUseCase = container.deleteFavoriteUseCase,
            restoreFavoriteUseCase = container.restoreFavoriteUseCase,
            refreshAllFavoritesUseCase = container.refreshAllFavoritesUseCase,
            selectCityUseCase = container.selectCityUseCase
        )
    }

    initializer {
        val container = container()
        HomeViewModel(
            loadCurrentWeatherUseCase = container.loadCurrentWeatherUseCase,
            getLocationSuggestionsUseCase = container.getLocationSuggestionsUseCase,
            observeFavoriteStatusUseCase = container.observeFavoriteStatusUseCase,
            observeSignedInUserIdUseCase = container.observeSignedInUserIdUseCase,
            observeSelectedCityUseCase = container.observeSelectedCityUseCase,
            resolveStartupLocationUseCase = container.resolveStartupLocationUseCase,
            refreshFavoriteSnapshotUseCase = container.refreshFavoriteSnapshotUseCase,
            toggleFavoriteUseCase = container.toggleFavoriteUseCase,
            deleteFavoriteUseCase = container.deleteFavoriteUseCase,
            saveLastSearchedCityUseCase = container.saveLastSearchedCityUseCase,
            reverseGeocodeLocationUseCase = container.reverseGeocodeLocationUseCase,
            localizePlaceNameUseCase = container.localizePlaceNameUseCase
        )
    }

    initializer {
        val container = container()
        MapPickerViewModel(
            reverseGeocodeLocationUseCase = container().reverseGeocodeLocationUseCase,
            selectCityUseCase = container.selectCityUseCase
        )
    }

    initializer {
        val container = container()
        SettingsViewModel(
            observeUserProfileUseCase = container.observeUserProfileUseCase,
            observeUserPreferencesUseCase = container.observeUserPreferencesUseCase,
            setUnitSystem = container.setUnitSystemUseCase,
            setLanguage = container.setLanguageUseCase,
            setDarkMode = container.setDarkModeUseCase,
            toggleNotifications = container.toggleNotificationsUseCase,
            handleNotificationPermissionResult = container.handleNotificationPermissionResultUseCase,
            syncNotificationPermission = container.syncNotificationPermissionUseCase,
            consumeNotificationPrompt = container.consumeNotificationPromptUseCase,
            logoutUserUseCase = container.logoutUserUseCase
        )
    }
}
```

## core\di\DefaultAppContainer.kt

```kotlin
package com.mhq.fynecast.core.di

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mhq.fynecast.BuildConfig
import com.mhq.fynecast.alerts.data.database.AlertsDatabase
import com.mhq.fynecast.alerts.data.datasource.AlertsLocalDataSource
import com.mhq.fynecast.alerts.data.datasource.AlertsRoomDataSource
import com.mhq.fynecast.alerts.data.repoimpl.AlertTrackingControllerImpl
import com.mhq.fynecast.alerts.data.repoimpl.AlertsRepoImpl
import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.alerts.domain.usecases.CancelAllAlertTrackingUseCase
import com.mhq.fynecast.alerts.domain.usecases.CheckAlertConditionsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetAlertsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertIdsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertStatusUseCase
import com.mhq.fynecast.alerts.domain.usecases.RefreshTrackingNotificationUseCase
import com.mhq.fynecast.alerts.domain.usecases.RestoreAlertTrackingUseCase
import com.mhq.fynecast.alerts.domain.usecases.ToggleAlertsUseCase
import com.mhq.fynecast.alerts.domain.usecases.TrackAlertUseCase
import com.mhq.fynecast.alerts.domain.usecases.UntrackAlertUseCase
import com.mhq.fynecast.alerts.notifications.data.infra.AndroidNotificationPermissionChecker
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.alerts.notifications.domain.usecases.ConsumeNotificationPromptUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.SyncNotificationPermissionUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.auth.data.repoimpl.auth.AuthRepositoryImpl
import com.mhq.fynecast.auth.data.repoimpl.auth.SocialAuthCredentialProviderImpl
import com.mhq.fynecast.auth.data.repoimpl.profile.ImageStorageRepositoryImpl
import com.mhq.fynecast.auth.data.repoimpl.profile.UserProfileRepositoryImpl
import com.mhq.fynecast.auth.data.repository.auth.SocialAuthCredentialProvider
import com.mhq.fynecast.auth.data.storage.ImageStorageManager
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.profile.ImageStorageRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
import com.mhq.fynecast.auth.domain.usecases.auth.AuthenticateWithSocialTokenUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.CompleteAuthSessionUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.LoginWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.RegisterWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.SendPasswordResetUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateLoginFormUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateSignupFormUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ObserveUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.RefreshUserSessionUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.UpdateUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ValidateEditProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.session.CheckUserLoggedInUseCase
import com.mhq.fynecast.auth.domain.usecases.session.LogoutUserUseCase
import com.mhq.fynecast.core.data.infra.AppInitializerImpl
import com.mhq.fynecast.core.data.infra.FirebaseCurrentUserProvider
import com.mhq.fynecast.core.data.location.SelectedCityRepositoryImpl
import com.mhq.fynecast.core.data.time.SystemTimeProvider
import com.mhq.fynecast.core.domain.infra.AppInitializer
import com.mhq.fynecast.core.domain.infra.AppLanguageProvider
import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import com.mhq.fynecast.core.domain.location.LocationRepository
import com.mhq.fynecast.core.domain.location.ObserveSelectedCityUseCase
import com.mhq.fynecast.core.domain.location.SelectCityUseCase
import com.mhq.fynecast.core.domain.location.SelectedCityRepository
import com.mhq.fynecast.core.domain.time.TimeProvider
import com.mhq.fynecast.core.domain.weather.CurrentWeatherStore
import com.mhq.fynecast.core.domain.weather.WeatherRepository
import com.mhq.fynecast.favorites.data.database.FavoritesDatabase
import com.mhq.fynecast.favorites.data.datasource.FavoritesLocalDataSource
import com.mhq.fynecast.favorites.data.datasource.FavoritesRoomDataSource
import com.mhq.fynecast.favorites.data.repoimpl.FavoritesRepoImpl
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.KeepFavoriteNamesLocalizedUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshAllFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshFavoriteSnapshotUseCase
import com.mhq.fynecast.favorites.domain.usecases.RelocalizeFavoriteNamesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RestoreFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.ToggleFavoriteUseCase
import com.mhq.fynecast.home.data.api.location.LocationApiService
import com.mhq.fynecast.home.data.api.weather.WeatherApiService
import com.mhq.fynecast.home.data.datasource.location.AndroidPlaceNameSource
import com.mhq.fynecast.home.data.datasource.location.DeviceLocationDataSource
import com.mhq.fynecast.home.data.datasource.location.FusedDeviceLocationDataSource
import com.mhq.fynecast.home.data.datasource.location.LocationRemoteDataSource
import com.mhq.fynecast.home.data.datasource.location.PhotonLocationRemoteDataSource
import com.mhq.fynecast.home.data.datasource.weather.WeatherRemoteDataSource
import com.mhq.fynecast.home.data.datasource.weather.WeatherRemoteDataSourceImpl
import com.mhq.fynecast.home.data.infra.AndroidLocationStatusChecker
import com.mhq.fynecast.home.data.repoimpl.location.DefaultLocationRepoImpl
import com.mhq.fynecast.home.data.repoimpl.location.PhotonLocationRepoImpl
import com.mhq.fynecast.home.data.repoimpl.weather.InMemoryCurrentWeatherStore
import com.mhq.fynecast.home.data.repoimpl.weather.WeatherRepoImpl
import com.mhq.fynecast.home.domain.repository.DeviceLocationRepository
import com.mhq.fynecast.home.domain.repository.LocationProviderStatusChecker
import com.mhq.fynecast.home.domain.usecases.GetCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.GetLastSearchedCityUseCase
import com.mhq.fynecast.home.domain.usecases.GetLocationSuggestionsUseCase
import com.mhq.fynecast.home.domain.usecases.GetWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.LoadCurrentWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.LocalizePlaceNameUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveFavoriteStatusUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveSignedInUserIdUseCase
import com.mhq.fynecast.home.domain.usecases.ResolveStartupLocationUseCase
import com.mhq.fynecast.map.domain.usecases.ReverseGeocodeLocationUseCase
import com.mhq.fynecast.settings.data.infra.PreferencesAppLanguageProvider
import com.mhq.fynecast.settings.data.repoimpl.UserPreferencesRepositoryImpl
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import com.mhq.fynecast.settings.domain.usecases.profile.ClearLastSearchedCityUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SaveLastSearchedCityUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetDarkModeUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetLanguageUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetNotificationsEnabledUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetUnitSystemUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class DefaultAppContainer(
    private val context: Context,
    private val prefsDataStore: DataStore<Preferences>,
    private val sessionDataStore: DataStore<Preferences>
) : AppContainer {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val appInitializer: AppInitializer by lazy {
        AppInitializerImpl(context.applicationContext as Application)
    }

    // Infrastructure Components
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val imageStorageManager: ImageStorageManager by lazy { ImageStorageManager(context) }

    private val networkClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Networking — Weather
    private val weatherApiBaseUrl = "https://api.weatherapi.com/"
    private val weatherApiJson =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
    private val weatherApiRetrofit = Retrofit.Builder()
        .client(networkClient)
        .addConverterFactory(
            weatherApiJson.asConverterFactory("application/json".toMediaType())
        )
        .baseUrl(weatherApiBaseUrl)
        .build()
    private val weatherApiRetrofitService: WeatherApiService by lazy {
        weatherApiRetrofit.create(WeatherApiService::class.java)
    }
    private val weatherRemoteDataSource: WeatherRemoteDataSource by lazy {
        WeatherRemoteDataSourceImpl(
            weatherApiService = weatherApiRetrofitService,
            apiKey = BuildConfig.WEATHER_API_KEY
        )
    }
    private val weatherRepository: WeatherRepository by lazy {
        WeatherRepoImpl(remoteDataSource = weatherRemoteDataSource)
    }
    private val currentWeatherStore: CurrentWeatherStore by lazy {
        InMemoryCurrentWeatherStore()
    }
    override val getWeatherUseCase: GetWeatherUseCase by lazy {
        GetWeatherUseCase(weatherRepository)
    }
    override val loadCurrentWeatherUseCase: LoadCurrentWeatherUseCase by lazy {
        LoadCurrentWeatherUseCase(weatherRepository, currentWeatherStore)
    }
    override val observeCurrentLocationUseCase by lazy {
        ObserveCurrentLocationUseCase(
            currentWeatherStore
        )
    }

    // Networking — Photon
    private val photonBaseUrl = "https://photon.komoot.io/"
    private val photonJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val photonRetrofit = Retrofit.Builder()
        .client(networkClient)
        .addConverterFactory(photonJson.asConverterFactory("application/json".toMediaType()))
        .baseUrl(photonBaseUrl)
        .build()
    private val photonApiRetrofitService: LocationApiService by lazy {
        photonRetrofit.create(LocationApiService::class.java)
    }

    // Repositories
    private val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(firebaseAuth, sessionDataStore, socialAuthCredentialProvider)
    }
    private val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepositoryImpl(firebaseAuth, sessionDataStore, applicationScope)
    }
    private val imageStorageRepository: ImageStorageRepository by lazy {
        ImageStorageRepositoryImpl(context, imageStorageManager)
    }

    // Location Tracker
    private val locationProviderStatusChecker: LocationProviderStatusChecker by lazy {
        AndroidLocationStatusChecker(appContext = context)
    }
    private val deviceLocationDataSource: DeviceLocationDataSource by lazy {
        FusedDeviceLocationDataSource(
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        )
    }
    private val deviceLocationRepository: DeviceLocationRepository by lazy {
        DefaultLocationRepoImpl(
            deviceLocationDataSource = deviceLocationDataSource,
            statusChecker = locationProviderStatusChecker
        )
    }
    private val locationRemoteDataSource: LocationRemoteDataSource by lazy {
        PhotonLocationRemoteDataSource(photonApiRetrofitService)
    }
    private val locationRepository: LocationRepository by lazy {
        PhotonLocationRepoImpl(
            remoteDataSource = locationRemoteDataSource,
            placeNameSource = AndroidPlaceNameSource(context)
        )
    }
    override val localizePlaceNameUseCase: LocalizePlaceNameUseCase by lazy {
        LocalizePlaceNameUseCase(locationRepository)
    }

    override val getCurrentLocationUseCase: GetCurrentLocationUseCase by lazy {
        GetCurrentLocationUseCase(deviceLocationRepository)
    }
    override val getLocationSuggestionsUseCase: GetLocationSuggestionsUseCase by lazy {
        GetLocationSuggestionsUseCase(locationRepository)
    }
    override val reverseGeocodeLocationUseCase: ReverseGeocodeLocationUseCase by lazy {
        ReverseGeocodeLocationUseCase(locationRepository)
    }
    override val getLastSearchedCityUseCase: GetLastSearchedCityUseCase by lazy {
        GetLastSearchedCityUseCase(userPreferencesRepository)
    }
    private val selectedCityRepository: SelectedCityRepository by lazy {
        SelectedCityRepositoryImpl()
    }
    override val selectCityUseCase: SelectCityUseCase by lazy {
        SelectCityUseCase(selectedCityRepository)
    }
    override val observeSelectedCityUseCase: ObserveSelectedCityUseCase by lazy {
        ObserveSelectedCityUseCase(selectedCityRepository)
    }
    override val resolveStartupLocationUseCase: ResolveStartupLocationUseCase by lazy {
        ResolveStartupLocationUseCase(getCurrentLocationUseCase, getLastSearchedCityUseCase)
    }


    // Signup & Login Use Cases
    override val validateSignupFormUseCase by lazy {
        ValidateSignupFormUseCase()
    }
    override val validateLoginFormUseCase by lazy {
        ValidateLoginFormUseCase()
    }
    override val registerWithEmailUseCase: RegisterWithEmailUseCase by lazy {
        RegisterWithEmailUseCase(
            authRepository = authRepository,
            completeAuthSessionUseCase = completeAuthSessionUseCase
        )
    }
    override val loginWithEmailUseCase: LoginWithEmailUseCase by lazy {
        LoginWithEmailUseCase(
            authRepository = authRepository,
            completeAuthSessionUseCase = completeAuthSessionUseCase
        )
    }
    private val socialAuthCredentialProvider: SocialAuthCredentialProvider by lazy {
        SocialAuthCredentialProviderImpl(appContext = context)
    }
    override val completeAuthSessionUseCase: CompleteAuthSessionUseCase by lazy {
        CompleteAuthSessionUseCase(
            authRepository = authRepository,
            userProfileRepository = userProfileRepository
        )
    }
    override val authenticateWithSocialTokenUseCase: AuthenticateWithSocialTokenUseCase by lazy {
        AuthenticateWithSocialTokenUseCase(
            authRepository = authRepository,
            completeAuthSessionUseCase = completeAuthSessionUseCase
        )
    }
    override val sendPasswordResetUseCase by lazy {
        SendPasswordResetUseCase(authRepository)
    }


    // Profile & Logout Use Cases
    private val currentUserProvider: CurrentUserProvider by lazy {
        FirebaseCurrentUserProvider(firebaseAuth)
    }
    override val validateEditProfileUseCase by lazy {
        ValidateEditProfileUseCase()
    }
    override val observeUserProfileUseCase by lazy {
        ObserveUserProfileUseCase(userProfileRepository)
    }
    override val refreshUserSessionUseCase: RefreshUserSessionUseCase by lazy {
        RefreshUserSessionUseCase(userProfileRepository, authRepository, logoutUserUseCase)
    }
    override val observeSignedInUserIdUseCase by lazy {
        ObserveSignedInUserIdUseCase(
            currentUserProvider
        )
    }
    override val updateUserProfileUseCase by lazy {
        UpdateUserProfileUseCase(userProfileRepository, imageStorageRepository, authRepository)
    }
    override val checkUserLoggedInUseCase: CheckUserLoggedInUseCase by lazy {
        CheckUserLoggedInUseCase(authRepository)
    }
    override val logoutUserUseCase by lazy {
        LogoutUserUseCase(
            authRepository,
            userProfileRepository,
            cancelAllAlertTrackingUseCase,
            clearLastSearchedCityUseCase
        )
    }

    // Preferences Use Cases
    private val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepositoryImpl(prefsDataStore)
    }
    override val observeUserPreferencesUseCase by lazy {
        ObserveUserPreferencesUseCase(userPreferencesRepository)
    }
    override val setUnitSystemUseCase by lazy { SetUnitSystemUseCase(userPreferencesRepository) }
    override val setLanguageUseCase by lazy { SetLanguageUseCase(userPreferencesRepository) }
    override val setDarkModeUseCase by lazy { SetDarkModeUseCase(userPreferencesRepository) }
    override val saveLastSearchedCityUseCase by lazy {
        SaveLastSearchedCityUseCase(userPreferencesRepository)
    }
    private val setNotificationsEnabledUseCase by lazy {
        SetNotificationsEnabledUseCase(userPreferencesRepository)
    }
    private val clearLastSearchedCityUseCase by lazy {
        ClearLastSearchedCityUseCase(userPreferencesRepository)
    }


    // Favorites Use Cases
    private val favoritesLocalDataSource: FavoritesLocalDataSource by lazy {
        FavoritesRoomDataSource(favoritesDatabase.favoriteDao())
    }
    private val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepoImpl(favoritesLocalDataSource, currentUserProvider)
    }
    private val favoritesDatabase: FavoritesDatabase by lazy {
        Room.databaseBuilder(
            context,
            FavoritesDatabase::class.java,
            "favorites_db"
        ).build()
    }
    override val getFavoritesUseCase: GetFavoritesUseCase by lazy {
        GetFavoritesUseCase(favoritesRepository)
    }
    override val deleteFavoriteUseCase: DeleteFavoriteUseCase by lazy {
        DeleteFavoriteUseCase(favoritesRepository)
    }
    override val toggleFavoriteUseCase: ToggleFavoriteUseCase by lazy {
        ToggleFavoriteUseCase(favoritesRepository)
    }
    override val observeFavoriteStatusUseCase: ObserveFavoriteStatusUseCase by lazy {
        ObserveFavoriteStatusUseCase(favoritesRepository)
    }
    override val refreshFavoriteSnapshotUseCase: RefreshFavoriteSnapshotUseCase by lazy {
        RefreshFavoriteSnapshotUseCase(favoritesRepository)
    }
    override val refreshAllFavoritesUseCase: RefreshAllFavoritesUseCase by lazy {
        RefreshAllFavoritesUseCase(favoritesRepository, weatherRepository)
    }
    override val restoreFavoriteUseCase: RestoreFavoriteUseCase by lazy {
        RestoreFavoriteUseCase(favoritesRepository)
    }
    private val appLanguageProvider: AppLanguageProvider by lazy {
        PreferencesAppLanguageProvider(userPreferencesRepository)
    }
    private val relocalizeFavoriteNamesUseCase: RelocalizeFavoriteNamesUseCase by lazy {
        RelocalizeFavoriteNamesUseCase(favoritesRepository, locationRepository)
    }
    override val keepFavoriteNamesLocalizedUseCase: KeepFavoriteNamesLocalizedUseCase by lazy {
        KeepFavoriteNamesLocalizedUseCase(
            appLanguageProvider,
            currentUserProvider,
            relocalizeFavoriteNamesUseCase
        )
    }

    // Time
    private val timeProvider: TimeProvider by lazy { SystemTimeProvider() }

    // Alerts
    private val alertTrackingController: AlertTrackingController by lazy {
        AlertTrackingControllerImpl(context)
    }
    private val alertsDatabase: AlertsDatabase by lazy {
        Room.databaseBuilder(
            context,
            AlertsDatabase::class.java,
            "alerts_db"
        ).build()
    }
    private val alertsLocalDataSource: AlertsLocalDataSource by lazy {
        AlertsRoomDataSource(alertsDatabase.alertDao())
    }
    private val alertsRepository: AlertsRepository by lazy {
        AlertsRepoImpl(localDataSource = alertsLocalDataSource)
    }
    override val getAlertsUseCase: GetAlertsUseCase by lazy {
        GetAlertsUseCase(currentWeatherStore)
    }
    override val trackAlertUseCase: TrackAlertUseCase by lazy {
        TrackAlertUseCase(
            alertsRepository,
            alertTrackingController,
            timeProvider,
            notificationPermissionChecker,
            observeUserPreferencesUseCase
        )
    }
    override val untrackAlertUseCase: UntrackAlertUseCase by lazy {
        UntrackAlertUseCase(alertsRepository, alertTrackingController)
    }
    override val cancelAllAlertTrackingUseCase: CancelAllAlertTrackingUseCase by lazy {
        CancelAllAlertTrackingUseCase(alertsRepository, alertTrackingController)
    }
    override val checkAlertConditionsUseCase: CheckAlertConditionsUseCase by lazy {
        CheckAlertConditionsUseCase(alertsRepository, timeProvider)
    }
    override val toggleAlertsUseCase: ToggleAlertsUseCase by lazy {
        ToggleAlertsUseCase(alertsRepository, trackAlertUseCase, untrackAlertUseCase)
    }
    override val getTrackedAlertIdsUseCase: GetTrackedAlertIdsUseCase by lazy {
        GetTrackedAlertIdsUseCase(alertsRepository)
    }
    override val getTrackedAlertStatusUseCase: GetTrackedAlertStatusUseCase by lazy {
        GetTrackedAlertStatusUseCase(alertsRepository, timeProvider)
    }
    override val restoreAlertTrackingUseCase: RestoreAlertTrackingUseCase by lazy {
        RestoreAlertTrackingUseCase(
            alertsRepository,
            alertTrackingController,
            untrackAlertUseCase,
            timeProvider
        )
    }

    override val refreshTrackingNotificationUseCase by lazy {
        RefreshTrackingNotificationUseCase(alertsRepository, alertTrackingController)
    }

    override val notificationPermissionChecker: NotificationPermissionChecker by lazy {
        AndroidNotificationPermissionChecker(context = context)
    }
    override val toggleNotificationsUseCase by lazy {
        ToggleNotificationsUseCase(
            setNotificationsEnabledUseCase = setNotificationsEnabledUseCase,
            notificationPermissionChecker = notificationPermissionChecker,
            cancelAllAlertTrackingUseCase = cancelAllAlertTrackingUseCase
        )
    }
    override val handleNotificationPermissionResultUseCase by lazy {
        HandleNotificationPermissionResultUseCase(setNotificationsEnabledUseCase)
    }
    override val syncNotificationPermissionUseCase by lazy {
        SyncNotificationPermissionUseCase(
            observeUserPreferencesUseCase = observeUserPreferencesUseCase,
            setNotificationsEnabledUseCase = setNotificationsEnabledUseCase,
            notificationPermissionChecker = notificationPermissionChecker,
            cancelAllAlertTrackingUseCase = cancelAllAlertTrackingUseCase
        )
    }
    override val consumeNotificationPromptUseCase by lazy {
        ConsumeNotificationPromptUseCase(
            notificationPermissionChecker,
            userPreferencesRepository
        )
    }
}
```

## core\di\FyneCastApplication.kt

```kotlin
package com.mhq.fynecast.core.di

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.datastore.preferences.preferencesDataStore
import com.mhq.fynecast.BuildConfig
import com.mhq.fynecast.R

private val Context.internalPrefsStore by preferencesDataStore(name = "user_preferences")
private val Context.internalSessionStore by preferencesDataStore(name = "user_session")

class FyneCastApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Pass both pure DataStore file handles straight down into the constructor graph
        container = DefaultAppContainer(
            context = this,
            prefsDataStore = internalPrefsStore,
            sessionDataStore = internalSessionStore
        )
        if (BuildConfig.DEBUG) container.appInitializer.initializeDebugFeatures()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        manager.createNotificationChannels(
            listOf(
                // 1. Silent, persistent background tracking channel
                NotificationChannel(
                    CHANNEL_ALERT_TRACKING,
                    getString(R.string.channel_alert_tracking_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.channel_alert_tracking_description)
                    setSound(null, null)
                    enableVibration(false)
                },

                // 2. High-priority critical emergency alarm channel
                NotificationChannel(
                    CHANNEL_ALERT_ALARM,
                    getString(R.string.channel_alert_alarm_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = getString(R.string.channel_alert_alarm_description)
                    setBypassDnd(true)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                }
            )
        )
    }

    companion object {
        lateinit var instance: FyneCastApplication
            private set

        const val CHANNEL_ALERT_TRACKING = "channel_alert_tracking"
        const val CHANNEL_ALERT_ALARM = "channel_alert_alarm"
    }
}
```

## core\domain\infra\AppInitializer.kt

```kotlin
package com.mhq.fynecast.core.domain.infra

interface AppInitializer {
    fun initializeDebugFeatures()
}
```

## core\domain\infra\AppLanguageProvider.kt

```kotlin
package com.mhq.fynecast.core.domain.infra

import kotlinx.coroutines.flow.Flow

interface AppLanguageProvider {
    /** The app language code ("en", "ar"): the current one first, then again on every change. */
    fun observeLanguageCode(): Flow<String>
}
```

## core\domain\infra\CurrentUserProvider.kt

```kotlin
package com.mhq.fynecast.core.domain.infra

import kotlinx.coroutines.flow.Flow

interface CurrentUserProvider {
    /** The signed-in user's ID, or null when signed out.
     * Emits again on every sign-in or sign-out. */
    fun observeUserId(): Flow<String?>
    fun currentUserId(): String?
}
```

## core\domain\location\CitySuggestionModel.kt

```kotlin
package com.mhq.fynecast.core.domain.location

data class CitySuggestionModel(
    val fullName: String,
    val latitude: Double,
    val longitude: Double
)
```

## core\domain\location\GeoCoordinates.kt

```kotlin
package com.mhq.fynecast.core.domain.location

data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double
)
```

## core\domain\location\LocationRepository.kt

```kotlin
package com.mhq.fynecast.core.domain.location

interface LocationRepository {
    suspend fun getCitySuggestions(query: String, lang: String): List<CitySuggestionModel>
    suspend fun getCityNameOffMap(lat: Double, lon: Double, lang: String): CitySuggestionModel?
    /** [name] rewritten in [lang] for the place near these coordinates, or null if not found. */
    suspend fun getLocalizedPlaceName(name: String, lat: Double, lon: Double, lang: String): String?
}
```

## core\domain\location\ObserveSelectedCityUseCase.kt

```kotlin
package com.mhq.fynecast.core.domain.location

import kotlinx.coroutines.flow.Flow

class ObserveSelectedCityUseCase(private val repository: SelectedCityRepository) {
    operator fun invoke(): Flow<SelectedCity> = repository.selections
}
```

## core\domain\location\SelectCityUseCase.kt

```kotlin
package com.mhq.fynecast.core.domain.location

class SelectCityUseCase(private val repository: SelectedCityRepository) {
    suspend operator fun invoke(city: SelectedCity) = repository.select(city)
}
```

## core\domain\location\SelectedCity.kt

```kotlin
package com.mhq.fynecast.core.domain.location

data class SelectedCity(
    val name: String,
    val latitude: Double,
    val longitude: Double
)
```

## core\domain\location\SelectedCityRepository.kt

```kotlin
package com.mhq.fynecast.core.domain.location

import kotlinx.coroutines.flow.Flow

/** Carries "the user picked this city" from any screen to Home. Each pick is delivered once. */
interface SelectedCityRepository {
    val selections: Flow<SelectedCity>
    suspend fun select(city: SelectedCity)
}
```

## core\domain\location\TrackedLocation.kt

```kotlin
package com.mhq.fynecast.core.domain.location

/**
 * Represents a geographical location being monitored for weather alerts.
 */
data class TrackedLocation(
    val trackedLocationName: String,
    val trackedLocationLatitude: Double,
    val trackedLocationLongitude: Double
) {
    /** The same string every time resolves to the same API location every time. */
    val trackedLocationQuery: String
        get() = "$trackedLocationLatitude,$trackedLocationLongitude"
}
```

## core\domain\time\TimeProvider.kt

```kotlin
package com.mhq.fynecast.core.domain.time

interface TimeProvider { fun nowMillis(): Long }
```

## core\domain\util\SuspendRunCatching.kt

```kotlin
package com.mhq.fynecast.core.domain.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * Same as runCatching, but rethrows cancellation. Plain runCatching would turn a cancelled
 * coroutine into a failed Result, so the screen would show an error for something the
 * user simply left.
 */
inline fun <T> suspendRunCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
```

## core\domain\weather\AlertDomainModel.kt

```kotlin
package com.mhq.fynecast.core.domain.weather

/**
 * Pure domain representation of a weather alert.
 */
data class AlertDomainModel(
    val id: String,
    val headline: String,
    val severity: String,
    val event: String,
    val effectiveMillis: Long,
    val expiresMillis: Long?,
    val description: String,
    val instruction: String
)
```

## core\domain\weather\CurrentWeatherStore.kt

```kotlin
package com.mhq.fynecast.core.domain.weather

import kotlinx.coroutines.flow.StateFlow

interface CurrentWeatherStore {
    val current: StateFlow<WeatherDomainModel?>
    fun publish(weather: WeatherDomainModel)
}
```

## core\domain\weather\DailyForecastDomainModel.kt

```kotlin
package com.mhq.fynecast.core.domain.weather

import java.time.LocalDate

data class DailyForecastDomainModel(
    val date: LocalDate,
    val iconUrl: String,
    val description: String,
    val maxTempC: Double,
    val maxTempF: Double,
    val minTempC: Double,
    val minTempF: Double
)
```

## core\domain\weather\HourlyForecastDomainModel.kt

```kotlin
package com.mhq.fynecast.core.domain.weather

import java.time.LocalDateTime

data class HourlyForecastDomainModel(
    val time: LocalDateTime,
    val iconUrl: String,
    val tempC: Double,
    val tempF: Double
)
```

## core\domain\weather\WeatherDomainModel.kt

```kotlin
package com.mhq.fynecast.core.domain.weather

import java.time.LocalDateTime

data class WeatherDomainModel(
    val cityName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val currentTempC: Double,
    val currentTempF: Double,
    val conditionText: String,
    val conditionIcon: String,
    val lastUpdated: LocalDateTime?,
    val timeZoneId: String?,
    val alerts: List<AlertDomainModel>,
    val stats: WeatherStatsDomainModel,
    val dailyForecasts: List<DailyForecastDomainModel>,
    val hourlyForecasts: List<HourlyForecastDomainModel>
)
```

## core\domain\weather\WeatherRepository.kt

```kotlin
package com.mhq.fynecast.core.domain.weather

interface WeatherRepository {
    suspend fun fetchWeather(query: String, lang: String): WeatherDomainModel
}
```

## core\domain\weather\WeatherStatsDomainModel.kt

```kotlin
package com.mhq.fynecast.core.domain.weather

data class WeatherStatsDomainModel(
    val humidity: Int,
    val windKph: Double,
    val windMph: Double,
    val pressureMb: Double,
    val pressureIn: Double,
    val uvIndex: Double
)
```

## core\navigation\AppDestination.kt

```kotlin
package com.mhq.fynecast.core.navigation

/** Every screen route in the app. */
enum class AppDestination(val route: String) {
    Signup("signup"),
    Login("login"),
    ForgotPassword("forgot_password"),
    EditProfile("edit_profile"),
    MapPicker("map_picker"),
    Home("home"),
    Favorites("favorites"),
    Alerts("alerts"),
    Settings("settings")
}
```

## core\navigation\BottomNavigationBar.kt

```kotlin
package com.mhq.fynecast.core.navigation

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onItemSelected: (BottomNavTab) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .padding(
                start = 40.dp,
                end = 40.dp,
                bottom = 0.dp
            )
            .navigationBarsPadding()
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            BottomNavTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = currentRoute == tab.destination.route,
                    label = {
                        Text(
                            text = stringResource(id = tab.titleRes),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    onClick = { onItemSelected(tab) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun BottomNavigationBarPreview() {
    FyneCastTheme() {
        BottomNavigationBar(
            currentRoute = "",
            onItemSelected = {}
        )
    }
}
```

## core\navigation\BottomNavTab.kt

```kotlin
package com.mhq.fynecast.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.mhq.fynecast.R

/** Only the four tabs shown in the bottom bar. */
enum class BottomNavTab(
    val destination: AppDestination,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    Home(AppDestination.Home, R.string.home, Icons.Default.Home),
    Favorites(AppDestination.Favorites, R.string.favorites, Icons.Default.Favorite),
    Alerts(AppDestination.Alerts, R.string.alerts, Icons.Default.Notifications),
    Settings(AppDestination.Settings, R.string.settings, Icons.Default.Settings)
}
```

## core\presentation\SessionViewModel.kt

```kotlin
package com.mhq.fynecast.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.auth.domain.usecases.profile.RefreshUserSessionUseCase
import com.mhq.fynecast.auth.domain.usecases.session.CheckUserLoggedInUseCase
import com.mhq.fynecast.core.navigation.AppDestination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SessionViewModel(
    checkUserLoggedIn: CheckUserLoggedInUseCase,
    private val refreshUserSession: RefreshUserSessionUseCase
) : ViewModel() {

    val startDestination: String =
        if (checkUserLoggedIn())
            AppDestination.Home.route
        else
            AppDestination.Signup.route

    private val _sessionExpired = Channel<Unit>(Channel.BUFFERED)
    val sessionExpired = _sessionExpired.receiveAsFlow()

    /** Checks with Firebase that the account is still valid, e.g. after an email change. */
    fun onAppResumed() {
        viewModelScope.launch {
            if (!refreshUserSession()) _sessionExpired.send(Unit)
        }
    }
}
```

## core\presentation\text\UiText.kt

```kotlin
package com.mhq.fynecast.core.presentation.text

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Text a ViewModel can publish without touching Context; the UI turns it into a String. */
sealed class UiText {
    data class Dynamic(val value: String) : UiText()
    data class Resource(@StringRes val resId: Int, val args: List<Any> = emptyList()) : UiText()

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Resource -> stringResource(resId, *args.toTypedArray())
    }

    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(resId, *args.toTypedArray())
    }

    companion object {
        fun res(@StringRes resId: Int, vararg args: Any): UiText = Resource(resId, args.toList())
    }
}
```

## core\ui\format\DateTimeFormats.kt

```kotlin
package com.mhq.fynecast.core.ui.format

import android.text.format.DateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeFormats {
    private fun formatter(
        locale: Locale,
        skeleton: String
    ) = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, skeleton), locale
    )

    fun hour(time: LocalDateTime, locale: Locale): String =
        time.format(formatter(locale, "j"))

    fun lastUpdated(time: LocalDateTime, locale: Locale): String =
        time.format(formatter(locale, "jm"))

    fun dayName(date: LocalDate, locale: Locale): String =
        date.format(formatter(locale, "EEE"))

    fun monthDay(date: LocalDate, locale: Locale): String =
        date.format(formatter(locale, "MMMd"))
}
```

## core\ui\format\TemperatureFormatter.kt

```kotlin
package com.mhq.fynecast.core.ui.format

import kotlin.math.roundToInt

object TemperatureFormatter {
    fun format(value: Double): String = "${value.roundToInt()}°"
}
```

## core\ui\globalcomponents\AppName.kt

```kotlin
package com.mhq.fynecast.core.ui.globalcomponents

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AppName(
    text: String,
    fontSize: TextUnit = 48.sp,
    fontFamily: FontFamily = FontFamily.Cursive,
    fontWeight: FontWeight = FontWeight.Bold,
    strokeWidth: Float = 5f,
    modifier: Modifier = Modifier,
) {

    Box(
        modifier = modifier
            .padding(bottom = 48.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = MaterialTheme.colorScheme.outline,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                drawStyle = Stroke(width = strokeWidth)
            ),
            modifier = Modifier.clearAndSetSemantics {}
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = fontFamily
            )
        )
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview(showBackground = true)
@Composable
fun AppNamePreview() {
    FyneCastTheme() {
        AppName(
            text = "FyneCast"
        )
    }
}
```

## core\ui\globalcomponents\ArabicText.kt

```kotlin
package com.mhq.fynecast.core.ui.globalcomponents

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Arabic letters join, and any letter spacing breaks the joins, so Arabic gets none. */
@Composable
fun TextUnit.unlessArabic(): TextUnit =
    if (currentLocale().language == "ar") 0.sp else this
```

## core\ui\globalcomponents\AuthActionButton.kt

```kotlin
package com.mhq.fynecast.core.ui.globalcomponents

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AuthActionButton(
    text: String,
    onDoAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onDoAction,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
            .padding(start = 36.dp, top = 16.dp, end = 36.dp)
            .height(56.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthActionButtonPreview() {
    FyneCastTheme() {
        AuthActionButton(
            text = "Sign Up",
            onDoAction = {}
        )
    }
}
```

## core\ui\globalcomponents\AuthFooterHyperlink.kt

```kotlin
package com.mhq.fynecast.core.ui.globalcomponents

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AuthFooterHyperlink(
    question: String,
    answer: String,
    onDoAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(
            start = 36.dp,
            top = 24.dp,
            end = 36.dp
        )
    ) {
        Text(
            text = question,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = answer,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 4.dp)
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .wrapContentHeight(Alignment.CenterVertically)
                .clickable { onDoAction() }
        )
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview
@Composable
private fun AuthFooterHyperlinkPreview() {
    FyneCastTheme() {
        AuthFooterHyperlink(
            question = "Already have an account?",
            answer = "Login",
            onDoAction = {}
        )
    }
}
```

## core\ui\globalcomponents\AuthInputField.kt

```kotlin
package com.mhq.fynecast.core.ui.globalcomponents

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AuthInputField(
    value: String,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier
) {

    OutlinedTextField(
        value = value,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        onValueChange = onValueChange,
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        },
        trailingIcon = trailingIcon,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorLabelColor = MaterialTheme.colorScheme.error,
            errorContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
    )
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview
@Composable
private fun AuthInputFieldPreview() {
    FyneCastTheme {
        AuthInputField(
            value = "value",
            label = "label",
            placeholder = "placeholder",
            leadingIcon = Icons.Default.AccessAlarm,
            trailingIcon = null,
            isError = false,
            onValueChange = {}
        )
    }
}
```

## core\ui\globalcomponents\AuthSocialMediaButton.kt

```kotlin
package com.mhq.fynecast.core.ui.globalcomponents

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AuthSocialMediaButton(
    onClick: () -> Unit,
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = modifier
            .size(48.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuthSocialMediaButtonPreview(){
    FyneCastTheme() {
        AuthSocialMediaButton(
            iconRes = R.drawable.ic_google_primary_light,
            contentDescription = null,
            onClick = {}
        )
    }
}
```

## core\ui\globalcomponents\AuthSocialOptions.kt

```kotlin
package com.mhq.fynecast.core.ui.globalcomponents

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AuthSocialOptions(
    onGoogleAuthenticate: () -> Unit,
    onFacebookAuthenticate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(top = 24.dp)
    ){
        Text(
            text = stringResource(R.string.or_with),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row {
            AuthSocialMediaButton(
                iconRes = R.drawable.ic_google_primary_light,
                contentDescription = stringResource(R.string.google_login),
                onClick = { onGoogleAuthenticate() },
                modifier = Modifier.padding(end = 8.dp)
            )
            AuthSocialMediaButton(
                iconRes = R.drawable.ic_facebook_primary_light,
                contentDescription = stringResource(R.string.facebook_login),
                onClick = { onFacebookAuthenticate() },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview
@Composable
private fun AuthSocialOptionsPreview() {
    FyneCastTheme() {
        AuthSocialOptions(
            onGoogleAuthenticate = {},
            onFacebookAuthenticate = {}
        )
    }
}
```

## core\ui\globalcomponents\AuthWarningSnackbar.kt

```kotlin
package com.mhq.fynecast.core.ui.globalcomponents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AuthWarningSnackbar(
    snackbarData: SnackbarData?
) {
    val message = snackbarData?.visuals?.message ?: return

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f)
        ),
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 120.dp
        ).fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 16.dp
            ),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.alert_info),
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthWarningSnackbarPreview() {
    FyneCastTheme() {
        AuthWarningSnackbar(snackbarData = null)
    }
}
```

## core\ui\globalcomponents\BlankPagePlaceholder.kt

```kotlin
package com.mhq.fynecast.core.ui.globalcomponents

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun BlankPagePlaceholder(
    blankSectionIcon: ImageVector,
    blankSectionIconDescription: String?,
    blankSectionTitle: String,
    blankSectionSubtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.wrapContentSize()
    ) {
        Icon(
            imageVector = blankSectionIcon,
            contentDescription = blankSectionIconDescription,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            modifier = Modifier.size(120.dp)
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        Text(
            text = blankSectionTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = blankSectionSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(
            modifier = Modifier.height(24.dp)
        )
        Text(
            text = stringResource(R.string.tab_below_to_go_back_to_home_page),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview
@Composable
private fun BlankPagePlaceholderPreview() {
    FyneCastTheme() {
        BlankPagePlaceholder(
            blankSectionIcon = Icons.Default.AccountCircle,
            blankSectionIconDescription = null,
            blankSectionTitle = "blankSectionTitle",
            blankSectionSubtitle = "blankSectionSubtitle"
        )
    }
}
```

## core\ui\globalcomponents\currentLocale.kt

```kotlin
package com.mhq.fynecast.core.ui.globalcomponents

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
```

## core\ui\globalcomponents\dynamicBackgroundGradient.kt

```kotlin
package com.mhq.fynecast.core.ui.globalcomponents

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun dynamicBackgroundGradient(): List<Color> {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return if (isLight) listOf(
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.background
    ) else listOf(
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant
    )
}
```

## core\ui\globalcomponents\FyneCastApp.kt

```kotlin
package com.mhq.fynecast.core.ui.globalcomponents

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mhq.fynecast.R
import com.mhq.fynecast.alerts.presentation.AlertsViewModel
import com.mhq.fynecast.alerts.ui.screens.AlertsContainer
import com.mhq.fynecast.auth.presentation.editprofile.EditProfileViewModel
import com.mhq.fynecast.auth.ui.editprofile.ui.screens.EditProfileContainer
import com.mhq.fynecast.auth.ui.forgotpassword.ui.screens.ForgotPasswordContainer
import com.mhq.fynecast.auth.ui.login.ui.screens.LoginContainer
import com.mhq.fynecast.auth.presentation.signup.SignupViewModel
import com.mhq.fynecast.auth.ui.signup.ui.screens.SignupContainer
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.navigation.AppDestination
import com.mhq.fynecast.core.navigation.BottomNavTab
import com.mhq.fynecast.core.navigation.BottomNavigationBar
import com.mhq.fynecast.core.presentation.SessionViewModel
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.favorites.presentation.FavoritesViewModel
import com.mhq.fynecast.favorites.ui.screens.FavoritesContainer
import com.mhq.fynecast.home.presentation.HomeUiState
import com.mhq.fynecast.home.presentation.HomeViewModel
import com.mhq.fynecast.home.ui.screens.HomeContainer
import com.mhq.fynecast.map.presentation.MapPickerViewModel
import com.mhq.fynecast.map.ui.screens.MapPickerScreen
import com.mhq.fynecast.settings.presentation.SettingsViewModel
import com.mhq.fynecast.settings.ui.screens.SettingsContainer
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val AUTO_REFRESH_INTERVAL_MILLIS = 15 * 60 * 1000L

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun FyneCastApp(
    homeViewModel: HomeViewModel,
    alertsViewModel: AlertsViewModel,
    settingsViewModel: SettingsViewModel,
    sessionViewModel: SessionViewModel,
    onRequestPermission: (String) -> Unit
) {
    val isMetric by settingsViewModel.isMetric.collectAsStateWithLifecycle()
    val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()
    val appLangCode = currentLanguage.code
    val storedDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDarkMode = storedDarkMode ?: isSystemInDarkTheme()

    LaunchedEffect(appLangCode) {
        homeViewModel.onLanguageChanged(appLangCode)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, appLangCode) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(AUTO_REFRESH_INTERVAL_MILLIS.milliseconds)
                homeViewModel.onRefreshRequested(appLangCode)
            }
        }
    }

    FyneCastTheme(isDarkMode = isDarkMode) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val context = LocalContext.current
        LaunchedEffect(sessionViewModel, navController) {
            sessionViewModel.sessionExpired.collect {
                Toast.makeText(
                    context,
                    R.string.session_expired_sign_in_again,
                    Toast.LENGTH_LONG
                ).show()
                navController.navigate(AppDestination.Login.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        val homeUiState = homeViewModel.state.collectAsStateWithLifecycle().value.uiState

        val rootRoutes = BottomNavTab.entries.map { it.destination.route }

        val shouldShowBar = if (currentRoute == AppDestination.Home.route) {
            homeUiState !is HomeUiState.Loading
        } else {
            currentRoute in rootRoutes
        }

        val bottomContentPadding = if (shouldShowBar) {
            PaddingValues(bottom = 120.dp)
        } else {
            PaddingValues(0.dp)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(dynamicBackgroundGradient()))
            ) {
                NavHost(
                    navController = navController,
                    startDestination = sessionViewModel.startDestination,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(AppDestination.Signup.route) {
                        val signupViewModel: SignupViewModel =
                            viewModel(factory = AppViewModelFactory)
                        SignupContainer(
                            onLoginNavigate = { navController.goToAuthScreen(AppDestination.Login.route) },
                            onSignupClick = { navController.enterApp() },
                            modifier = Modifier,
                            signupViewModel = signupViewModel
                        )
                    }

                    composable(AppDestination.Login.route) {
                        LoginContainer(
                            onSignUpNavigate = { navController.goToAuthScreen(AppDestination.Signup.route) },
                            onLoginClick = { navController.enterApp() },
                            onForgotPasswordClick = {
                                navController.goToAuthScreen(
                                    AppDestination.ForgotPassword.route
                                )
                            },
                            modifier = Modifier
                        )
                    }

                    composable(AppDestination.ForgotPassword.route) {
                        ForgotPasswordContainer(
                            onBackToLoginClick = { navController.goToAuthScreen(AppDestination.Login.route) },
                            onSendLinkSuccessNavigate = {
                                navController.goToAuthScreen(
                                    AppDestination.Login.route
                                )
                            },
                            modifier = Modifier
                        )
                    }

                    composable(AppDestination.EditProfile.route) {
                        val editProfileViewModel: EditProfileViewModel =
                            viewModel(factory = AppViewModelFactory)
                        EditProfileContainer(
                            onBackClick = { navController.popBackStack() },
                            contentPadding = bottomContentPadding,
                            editProfileViewModel = editProfileViewModel
                        )
                    }

                    composable(AppDestination.Home.route) {
                        val isMetric by settingsViewModel.isMetric.collectAsStateWithLifecycle()
                        HomeContainer(
                            homeViewModel = homeViewModel,
                            onNavigateToMap = { navController.navigate(AppDestination.MapPicker.route) },
                            onAlertsBannerClick = { navController.switchToTab(BottomNavTab.Alerts) },
                            onRequestPermission = onRequestPermission,
                            contentPadding = bottomContentPadding,
                            isMetric = isMetric,
                            langCode = appLangCode
                        )
                    }

                    composable(AppDestination.Favorites.route) {
                        val favoritesViewModel: FavoritesViewModel =
                            viewModel(factory = AppViewModelFactory)

                        FavoritesContainer(
                            favoritesViewModel = favoritesViewModel,
                            onFavoriteSelected = { favoriteId ->
                                favoritesViewModel.onFavoriteSelected(favoriteId)
                                navController.switchToTab(BottomNavTab.Home)
                            },
                            contentPadding = bottomContentPadding,
                            isMetric = isMetric
                        )
                    }

                    composable(AppDestination.Alerts.route) {
                        AlertsContainer(
                            contentPadding = bottomContentPadding,
                            alertsViewModel = alertsViewModel
                        )
                    }

                    composable(AppDestination.Settings.route) {
                        SettingsContainer(
                            settingsViewModel = settingsViewModel,
                            contentPadding = bottomContentPadding,
                            onEditProfileClick = {
                                navController.navigate(AppDestination.EditProfile.route)
                            },
                            onLogoutClick = {
                                navController.navigate(AppDestination.Signup.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(AppDestination.MapPicker.route) {
                        val mapLangCode = currentLanguage.code
                        val mapPickerViewModel: MapPickerViewModel =
                            viewModel(factory = AppViewModelFactory)

                        val currentWeather = (homeUiState as? HomeUiState.Success)?.weather
                        MapPickerScreen(
                            mapPickerViewModel = mapPickerViewModel,
                            langCode = mapLangCode,
                            initialLatitude = currentWeather?.latitude,
                            initialLongitude = currentWeather?.longitude,
                            onLocationConfirmed = {
                                mapPickerViewModel.onLocationConfirmed()
                                if (navController.currentBackStackEntry?.destination?.route == AppDestination.MapPicker.route) {
                                    navController.popBackStack()
                                }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = shouldShowBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onItemSelected = { tab -> navController.switchToTab(tab) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FyneCastAppPreview() {
    FyneCastTheme {}
}

private fun NavController.goToAuthScreen(route: String) {
    /** Return to it if it is already below us;
     * otherwise push it. Never stack duplicates. */
    if (!popBackStack(route, inclusive = false)) {
        navigate(route) { launchSingleTop = true }
    }
}

/** Switches to a bottom-bar tab. Home is the base of the back stack, so "Home" just pops back to it. */
private fun NavController.switchToTab(tab: BottomNavTab) {
    val route = tab.destination.route
    if (currentDestination?.route == route) return
    if (tab == BottomNavTab.Home) {
        if (!popBackStack(route, inclusive = false)) {
            navigate(route) { launchSingleTop = true }
        }
    } else {
        navigate(route) {
            popUpTo(AppDestination.Home.route)
            launchSingleTop = true
        }
    }
}

private fun NavController.enterApp() {
    navigate(AppDestination.Home.route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}
```

## core\ui\globalcomponents\GlassyCard.kt

```kotlin
package com.mhq.fynecast.core.ui.globalcomponents

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassyCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    padding: Dp = 16.dp,
    containerAlpha: Float = 0.15f,
    borderAlpha: Float = 0.25f,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = containerAlpha)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = borderAlpha),
                    Color.White.copy(alpha = borderAlpha / 2)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(padding),
            content = content
        )
    }
}
```

## core\ui\LocaleContext.kt

```kotlin
package com.mhq.fynecast.core.ui

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

fun Context.withAppLocale(): Context {
    val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: return this
    val config = Configuration(resources.configuration).apply { setLocale(locale) }
    return createConfigurationContext(config)
}
```

## core\ui\MainActivity.kt

```kotlin
package com.mhq.fynecast.core.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mhq.fynecast.alerts.presentation.AlertsViewModel
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.presentation.SessionViewModel
import com.mhq.fynecast.core.ui.globalcomponents.FyneCastApp
import com.mhq.fynecast.home.presentation.HomeViewModel
import com.mhq.fynecast.settings.presentation.SettingsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val homeViewModel: HomeViewModel by lazy {
        ViewModelProvider(this, AppViewModelFactory)[HomeViewModel::class.java]
    }

    private val alertsViewModel: AlertsViewModel by lazy {
        ViewModelProvider(this, AppViewModelFactory)[AlertsViewModel::class.java]
    }

    private val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProvider(this, AppViewModelFactory)[SettingsViewModel::class.java]
    }

    private val sessionViewModel: SessionViewModel by lazy {
        ViewModelProvider(this, AppViewModelFactory)[SessionViewModel::class.java]
    }

    private var onLocationPermissionResult: ((Boolean) -> Unit)? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onLocationPermissionResult?.invoke(isGranted)
        settingsViewModel.requestNotificationPermissionOnFirstLaunch()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val blockedForGood = !granted && !ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )
        settingsViewModel.onNotificationPermissionResult(granted, blockedForGood)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !settingsViewModel.arePreferencesLoaded.value }

        lifecycleScope.launch {
            // Wait for the saved preferences, otherwise the default language is applied first.
            settingsViewModel.arePreferencesLoaded.first { it }
            settingsViewModel.currentLanguage
                .map { it.code }
                .distinctUntilChanged()
                .collect { langCode -> applyAppLanguage(langCode) }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsViewModel.requestNotificationPermissionEvent.collect {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        setContent {
            FyneCastApp(
                homeViewModel = homeViewModel,
                alertsViewModel = alertsViewModel,
                settingsViewModel = settingsViewModel,
                sessionViewModel = sessionViewModel,
                onRequestPermission = { activeLangCode ->
                    onLocationPermissionResult = { isGranted ->
                        homeViewModel.onLocationPermissionResult(
                            isGranted = isGranted,
                            langCode = activeLangCode
                        )
                    }
                    val alreadyGranted =
                        ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(
                                    this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                    if (alreadyGranted) {
                        // Nothing to ask: skip the system permission screen, it flashes the app.
                        onLocationPermissionResult?.invoke(true)
                        settingsViewModel.requestNotificationPermissionOnFirstLaunch()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            )
        }
    }

    /**
     * Applies the chosen language, but skips it when the app already shows it:
     * applying recreates the activity and shows up as a black flash.
     */
    private fun applyAppLanguage(langCode: String) {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val effectiveLanguage = (appLocales[0] ?: resources.configuration.locales[0]).language
        if (effectiveLanguage != Locale.forLanguageTag(langCode).language) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))
        }
    }

    override fun onResume() {
        super.onResume()
        val currentLangTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "en" }
        homeViewModel.onRefreshRequested(langCode = currentLangTag)
        settingsViewModel.onAppResumed()
        sessionViewModel.onAppResumed()
    }
}
```

## core\ui\NotificationSettings.kt

```kotlin
package com.mhq.fynecast.core.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings

fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    )
}
```

## core\ui\theme\Color.kt

```kotlin
package com.mhq.fynecast.core.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Core Brand Neutrals (Dark → Light spectrum) ───────────────────────────
val AbyssalVoid = Color(0xFF0F172A)         // Deepest dark — main dark background, was MidnightBlue
val BiscayBlue = Color(0xFF1E293B)          // Dark surface layer, was DuskBlue
val SlateGray = Color(0xFF334155)           // Mid-dark — cards, elevated surfaces, was SlateBlue
val CoolGray = Color(0xFF64748B)            // Secondary text, inactive states, was MutedSlate
val VividViolet = Color(0xFF8B5CF6)         // Severe weather, lightning, storms
val AliceBlue = Color(0xFFF1F5F9)           // Frosted light gray, was LightSlateSurface
val BabyBlue = Color(0xFFE0F2FE)            // Soft ice blue — light background, was LightIceBlue
val PalerSkyBlue = Color(0xFFBAE6FD)        // Gentle sky — light surface variant, was LightBabyBlue
val SkyBlue = Color(0xFF7DD3FC)             // Clear sky — light outlines/accents, was LightMiddayBlue
val LightBlue = Color(0xFF80D8FF)
val EditorialRoyalBlue = Color(0xFF007AFF)
val ElectricCyan = Color(0xFF02FEF7)
val SuccessDark = Color(0xFF81C784)

// ─── Brand Accent Greens ────────────────────────────────────────────────────
val ElectricLimeGreen = Color(0xFF39FF14)   // Primary accent — dark theme
val JewelForestGreen = Color(0xFF15803D)    // Primary accent — light theme

// ─── Other Colors ────────────────────────────────────────────────
val PersianRed = Color(0xFFD32F2F)
val AppleRed = Color(0xFFFF3B30)
val VividRed = Color(0xFFEF4444)
val StrawberryRed = Color(0xFFEF5350)
val SoftCoral = Color(0xFFFF8A80)
val Tangerine = Color(0xFFF57C00)
val Honey = Color(0xFFF59E0B)
val VividAmber = Color(0xFFFBC02D)
val LightSuccess = Color(0xFF2E7D32)
```

## core\ui\theme\Dimens.kt

```kotlin
package com.mhq.fynecast.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppDimensions(
    val paddingSmall: Dp = 8.dp,
    val paddingMedium: Dp = 16.dp,
    val paddingLarge: Dp = 24.dp,
    val spacingElement: Dp = 12.dp,
    val cardCornerRadius: Dp = 16.dp,
    val iconSizeSmall: Dp = 16.dp
)

// Global reference object
val LocalDimensions = staticCompositionLocalOf { AppDimensions() }
```

## core\ui\theme\Theme.kt

```kotlin
package com.mhq.fynecast.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FyneCastDarkScheme = darkColorScheme(
    // Primary action — buttons, active segmented buttons, focused borders
    primary = AbyssalVoid,
    onPrimary = ElectricLimeGreen,
    // Secondary — weather metric accents (temperature highs, UV)
    secondary = Honey,
    onSecondary = AbyssalVoid,
    // Tertiary — storm/alert accents, special callouts
    tertiary = VividViolet,
    onTertiary = Color.White,
    // Background — the deepest layer, full screen canvas
    background = AbyssalVoid,
    onBackground = Color.White,
    // Surface — cards, bottom sheets, elevated containers
    surface = BiscayBlue,
    onSurface = ElectricLimeGreen,
    // SurfaceVariant — chips, hourly/daily weather rows, input fields
    surfaceVariant = SlateGray,
    onSurfaceVariant = ElectricLimeGreen.copy(alpha = 0.85f),
    // Error — weather warnings, validation errors
    error = VividRed,
    onError = Color.White,
    // Outline — dividers, inactive borders, field outlines
    outline = Color.White.copy(alpha = 0.15f),
    outlineVariant = Color.White.copy(alpha = 0.08f)
)

private val FyneCastLightScheme = lightColorScheme(
    // Primary action — buttons, active states, focused borders
    primary = SkyBlue,
    onPrimary = AbyssalVoid,
    // Secondary — warm weather accents
    secondary = Honey,
    onSecondary = Color.White,
    // Tertiary — cool blue accents for humidity, precipitation
    tertiary = SkyBlue,
    onTertiary = AbyssalVoid,
    // Background — soft ice blue canvas
    background = BabyBlue,
    onBackground = AbyssalVoid,
    // Surface — pure white cards, input fields, bottom sheets
    surface = PalerSkyBlue,
    onSurface = AbyssalVoid,
    // SurfaceVariant — weather grid tiles, hourly rows
    surfaceVariant = PalerSkyBlue,
    onSurfaceVariant = AbyssalVoid,
    // Error
    error = VividRed,
    onError = Color.White,
    // Outline — field borders, dividers
    outline = AbyssalVoid,
    outlineVariant = PalerSkyBlue
)

@Composable
fun FyneCastTheme(
    isDarkMode: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (isDarkMode)
            FyneCastDarkScheme
        else
            FyneCastLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(),
        content = content
    )
}
```

## core\ui\theme\Type.kt

```kotlin
package com.mhq.fynecast.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

## favorites\data\database\FavoriteDao.kt

```kotlin
package com.mhq.fynecast.favorites.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavoriteDao {

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND locationKey = :key)")
    abstract fun observeIsFavorite(userId: String, key: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND locationKey = :key)")
    abstract suspend fun isFavorite(userId: String, key: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND locationKey = :key")
    abstract suspend fun deleteFavorite(userId: String, key: String)

    @Query(
        """UPDATE favorites SET temperatureC = :tempC, temperatureF = :tempF, weatherIconUrl = :icon
              WHERE userId = :userId AND locationKey = :key"""
    )
    abstract suspend fun updateSnapshot(
        userId: String,
        key: String,
        tempC: Double,
        tempF: Double,
        icon: String
    )

    @Query(
        """UPDATE favorites SET cityName = :cityName, countryName = :countryName
              WHERE userId = :userId AND locationKey = :key"""
    )
    abstract suspend fun updateNames(
        userId: String,
        key: String,
        cityName: String,
        countryName: String
    )

    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY cityName ASC, countryName ASC")
    abstract fun getAllFavorites(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE userId = :userId")
    abstract suspend fun getFavoritesOnce(userId: String): List<FavoriteEntity>

    @Transaction
    open suspend fun toggle(entity: FavoriteEntity) {
        if (isFavorite(entity.userId, entity.locationKey))
            deleteFavorite(entity.userId, entity.locationKey)
        else
            insertFavorite(entity)
    }
}
```

## favorites\data\database\FavoriteEntity.kt

```kotlin
package com.mhq.fynecast.favorites.data.database

import androidx.room.Entity

@Entity(
    tableName = "favorites",
    primaryKeys = ["userId", "locationKey"])
data class FavoriteEntity(
    val userId: String,
    val locationKey: String,
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val temperatureF: Double,
    val weatherIconUrl: String
)
```

## favorites\data\database\FavoritesDatabase.kt

```kotlin
package com.mhq.fynecast.favorites.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavoriteEntity::class], version = 1)
abstract class FavoritesDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
```

## favorites\data\datasource\FavoritesLocalDataSource.kt

```kotlin
package com.mhq.fynecast.favorites.data.datasource

import com.mhq.fynecast.favorites.data.database.FavoriteEntity
import kotlinx.coroutines.flow.Flow

/** Where favorites are stored on the phone. Works with database entities, never domain models. */
interface FavoritesLocalDataSource {
    fun observeIsFavorite(userId: String, key: String): Flow<Boolean>
    fun observeAll(userId: String): Flow<List<FavoriteEntity>>
    suspend fun getAllOnce(userId: String): List<FavoriteEntity>

    /** Adds the favorite if it is missing and removes it if present, as one atomic step. */
    suspend fun toggle(entity: FavoriteEntity)
    suspend fun insert(entity: FavoriteEntity)
    suspend fun delete(userId: String, key: String)
    suspend fun updateNames(userId: String, key: String, cityName: String, countryName: String)
    suspend fun updateSnapshot(
        userId: String,
        key: String,
        tempC: Double,
        tempF: Double,
        icon: String
    )
}
```

## favorites\data\datasource\FavoritesRoomDataSource.kt

```kotlin
package com.mhq.fynecast.favorites.data.datasource

import com.mhq.fynecast.favorites.data.database.FavoriteDao
import com.mhq.fynecast.favorites.data.database.FavoriteEntity
import kotlinx.coroutines.flow.Flow

class FavoritesRoomDataSource(
    private val favoriteDao: FavoriteDao
) : FavoritesLocalDataSource {

    override fun observeIsFavorite(userId: String, key: String): Flow<Boolean> =
        favoriteDao.observeIsFavorite(userId, key)

    override fun observeAll(userId: String): Flow<List<FavoriteEntity>> =
        favoriteDao.getAllFavorites(userId)

    override suspend fun getAllOnce(userId: String): List<FavoriteEntity> =
        favoriteDao.getFavoritesOnce(userId)

    override suspend fun toggle(entity: FavoriteEntity) = favoriteDao.toggle(entity)

    override suspend fun insert(entity: FavoriteEntity) = favoriteDao.insertFavorite(entity)

    override suspend fun delete(userId: String, key: String) =
        favoriteDao.deleteFavorite(userId, key)

    override suspend fun updateNames(
        userId: String,
        key: String,
        cityName: String,
        countryName: String
    ) = favoriteDao.updateNames(userId, key, cityName, countryName)

    override suspend fun updateSnapshot(
        userId: String,
        key: String,
        tempC: Double,
        tempF: Double,
        icon: String
    ) = favoriteDao.updateSnapshot(userId, key, tempC, tempF, icon)
}
```

## favorites\data\mapper\FavoritesMapper.kt

```kotlin
package com.mhq.fynecast.favorites.data.mapper

import com.mhq.fynecast.favorites.data.database.FavoriteEntity
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel

fun FavoriteEntity.toDomain() = FavoriteCityDomainModel(
    cityName = cityName,
    countryName = countryName,
    latitude = latitude,
    longitude = longitude,
    temperatureC = temperatureC,
    temperatureF = temperatureF,
    weatherIconUrl = weatherIconUrl
)

fun FavoriteCityDomainModel.toEntity(userId: String) = FavoriteEntity(
    userId = userId,
    locationKey = id,
    cityName = cityName,
    countryName = countryName,
    latitude = latitude,
    longitude = longitude,
    temperatureC = temperatureC,
    temperatureF = temperatureF,
    weatherIconUrl = weatherIconUrl
)
```

## favorites\data\repoimpl\FavoritesRepoImpl.kt

```kotlin
package com.mhq.fynecast.favorites.data.repoimpl

import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import com.mhq.fynecast.favorites.data.datasource.FavoritesLocalDataSource
import com.mhq.fynecast.favorites.data.mapper.toDomain
import com.mhq.fynecast.favorites.data.mapper.toEntity
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesRepoImpl(
    private val localDataSource: FavoritesLocalDataSource,
    private val currentUserProvider: CurrentUserProvider
) : FavoritesRepository {

    override fun observeIsFavorite(id: String): Flow<Boolean> =
        currentUserProvider.observeUserId().flatMapLatest { userId ->
            if (userId == null)
                flowOf(false)
            else
                localDataSource.observeIsFavorite(userId, id)
        }

    override suspend fun toggleFavorite(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        localDataSource.toggle(city.toEntity(userId))
    }

    override suspend fun saveFavorite(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        localDataSource.insert(city.toEntity(userId))
    }

    override suspend fun deleteFavorite(id: String) {
        val userId = currentUserProvider.currentUserId() ?: return
        localDataSource.delete(userId, id)
    }

    override suspend fun refreshSnapshot(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        localDataSource.updateSnapshot(
            userId,
            city.id,
            city.temperatureC,
            city.temperatureF,
            city.weatherIconUrl
        )
    }

    override suspend fun updateNames(id: String, cityName: String, countryName: String) {
        val userId = currentUserProvider.currentUserId() ?: return
        localDataSource.updateNames(userId, id, cityName, countryName)
    }

    override suspend fun getFavoritesList(): List<FavoriteCityDomainModel> {
        val userId = currentUserProvider.currentUserId() ?: return emptyList()
        return localDataSource.getAllOnce(userId).map { it.toDomain() }
    }

    override fun getAllFavorites(): Flow<List<FavoriteCityDomainModel>> =
        currentUserProvider.observeUserId().flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                localDataSource.observeAll(userId)
                    .map { entities -> entities.map { it.toDomain() } }
            }
        }
}
```

## favorites\domain\models\FavoriteCityDomainModel.kt

```kotlin
package com.mhq.fynecast.favorites.domain.models

import kotlin.math.roundToLong

data class FavoriteCityDomainModel(
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val temperatureF: Double,
    val weatherIconUrl: String
) {
    /** The only identity of a favorite:
     * its location, rounded to about 1 km. */
    val id: String get() = favoriteId(latitude, longitude)
}

fun favoriteId(latitude: Double, longitude: Double): String =
    "${(latitude * 100).roundToLong() / 100.0},${(longitude * 100).roundToLong() / 100.0}"
```

## favorites\domain\repository\FavoritesRepository.kt

```kotlin
package com.mhq.fynecast.favorites.domain.repository

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    /** Observes the signed-in user's favorites, sorted by city then country. */
    fun getAllFavorites(): Flow<List<FavoriteCityDomainModel>>
    /** Observes whether the city is a favorite to perform CRUD operations accordingly. */
    fun observeIsFavorite(id: String): Flow<Boolean>
    /** Adds the city if it isn't a favorite and removes it if it is, as one atomic step. */
    suspend fun toggleFavorite(city: FavoriteCityDomainModel)
    /** used to undo a removal. */
    suspend fun saveFavorite(city: FavoriteCityDomainModel)
    /** used to delete a favorite. */
    suspend fun deleteFavorite(id: String)
    /** updates temperature and icon. */
    suspend fun refreshSnapshot(city: FavoriteCityDomainModel)
    /** rewrites the saved city and country names, e.g. after the app language changed. */
    suspend fun updateNames(id: String, cityName: String, countryName: String)
    /** one-off read, not observed. */
    suspend fun getFavoritesList(): List<FavoriteCityDomainModel>
}
```

## favorites\domain\usecases\DeleteFavoriteUseCase.kt

```kotlin
package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository

class DeleteFavoriteUseCase(private val favoritesRepository: FavoritesRepository) {
    suspend operator fun invoke(id: String) = favoritesRepository.deleteFavorite(id)
}
```

## favorites\domain\usecases\GetFavoritesUseCase.kt

```kotlin
package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow

class GetFavoritesUseCase(private val favoritesRepository: FavoritesRepository) {
    operator fun invoke(): Flow<List<FavoriteCityDomainModel>> =
        favoritesRepository.getAllFavorites()
}
```

## favorites\domain\usecases\KeepFavoriteNamesLocalizedUseCase.kt

```kotlin
package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.core.domain.infra.AppLanguageProvider
import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * Keeps the saved favorite names in the app language without any screen being involved.
 *
 * It rewrites them whenever the app language changes while someone is signed in,
 * and when a user signs in (their favorites may have been saved in another language).
 * Nothing runs at app start: the names are already in the language they were saved in.
 */
class KeepFavoriteNamesLocalizedUseCase(
    private val appLanguageProvider: AppLanguageProvider,
    private val currentUserProvider: CurrentUserProvider,
    private val relocalizeFavoriteNames: RelocalizeFavoriteNamesUseCase
) {
    /** Never returns on its own: it runs for as long as the scope that calls it lives. */
    suspend operator fun invoke() {
        combine(
            appLanguageProvider.observeLanguageCode(),
            currentUserProvider.observeUserId()
        ) { lang, userId -> lang to userId }
            .distinctUntilChanged()
            .drop(1) // the state the app started in
            .collectLatest { (lang, userId) ->
                if (userId == null) return@collectLatest
                try {
                    relocalizeFavoriteNames(lang)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // The old names stay; the next language change or sign-in tries again.
                }
            }
    }
}
```

## favorites\domain\usecases\RefreshAllFavoritesUseCase.kt

```kotlin
package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import com.mhq.fynecast.core.domain.weather.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Brings every favorite's temperature and icon up to date (B41).
 * Runs at most once per [MIN_INTERVAL_MILLIS] and never twice at the same time.
 */
class RefreshAllFavoritesUseCase(
    private val favoritesRepository: FavoritesRepository,
    private val weatherRepository: WeatherRepository
) {
    private val runLock = Mutex()
    private var lastSuccessfulRunMillis = 0L

    suspend operator fun invoke(lang: String) {
        if (!runLock.tryLock()) return
        try {
            val now = System.currentTimeMillis()
            if (now - lastSuccessfulRunMillis < MIN_INTERVAL_MILLIS) return
            if (refreshAll(lang)) lastSuccessfulRunMillis = System.currentTimeMillis()
        } finally {
            runLock.unlock()
        }
    }

    /** True when at least one favorite was refreshed. */
    private suspend fun refreshAll(lang: String): Boolean {
        val favorites = favoritesRepository.getFavoritesList()
        if (favorites.isEmpty()) return false
        val gate = Semaphore(MAX_PARALLEL_REQUESTS)
        return coroutineScope {
            favorites.map { favorite ->
                async {
                    gate.withPermit {
                        try {
                            val weather = weatherRepository.fetchWeather(
                                "${favorite.latitude},${favorite.longitude}",
                                lang
                            )
                            favoritesRepository.refreshSnapshot(
                                favorite.copy(
                                    temperatureC = weather.currentTempC,
                                    temperatureF = weather.currentTempF,
                                    weatherIconUrl = weather.conditionIcon
                                )
                            )
                            true
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            false
                        }
                    }
                }
            }.awaitAll().any { it }
        }
    }

    private companion object {
        const val MIN_INTERVAL_MILLIS = 10 * 60 * 1000L
        const val MAX_PARALLEL_REQUESTS = 3
    }
}
```

## favorites\domain\usecases\RefreshFavoriteSnapshotUseCase.kt

```kotlin
package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository

class RefreshFavoriteSnapshotUseCase(private val favoritesRepository: FavoritesRepository) {
    suspend operator fun invoke(city: FavoriteCityDomainModel) =
        favoritesRepository.refreshSnapshot(city)
}
```

## favorites\domain\usecases\RelocalizeFavoriteNamesUseCase.kt

```kotlin
package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.core.domain.location.LocationRepository
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * One pass over the favorites of the signed-in user: rewrites each saved city and country
 * in [lang] (a city saved in English reads in Arabic after the switch, and back).
 *
 * A favorite whose name cannot be looked up (offline, no match near its coordinates)
 * keeps the name it already has.
 */
class RelocalizeFavoriteNamesUseCase(
    private val favoritesRepository: FavoritesRepository,
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(lang: String) {
        val favorites = favoritesRepository.getFavoritesList()
        if (favorites.isEmpty()) return
        val gate = Semaphore(MAX_PARALLEL_LOOKUPS)
        coroutineScope {
            favorites.map { favorite ->
                async { gate.withPermit { relocalize(favorite, lang) } }
            }.awaitAll()
        }
    }

    private suspend fun relocalize(favorite: FavoriteCityDomainModel, lang: String) {
        try {
            val localized = locationRepository.getLocalizedPlaceName(
                name = "${favorite.cityName}, ${favorite.countryName}",
                lat = favorite.latitude,
                lon = favorite.longitude,
                lang = lang
            ) ?: return

            val city = localized.substringBefore(",").trim()
            // The geocoder leaves the country out when it equals the city (e.g. "Singapore").
            val country = localized.substringAfter(",", "").trim().ifBlank { city }
            if (city.isBlank()) return
            if (city == favorite.cityName && country == favorite.countryName) return

            favoritesRepository.updateNames(favorite.id, city, country)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep the old name.
        }
    }

    private companion object {
        const val MAX_PARALLEL_LOOKUPS = 3
    }
}
```

## favorites\domain\usecases\RestoreFavoriteUseCase.kt

```kotlin
package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository

class RestoreFavoriteUseCase(private val favoritesRepository: FavoritesRepository) {
    suspend operator fun invoke(city: FavoriteCityDomainModel) =
        favoritesRepository.saveFavorite(city)
}
```

## favorites\domain\usecases\ToggleFavoriteUseCase.kt

```kotlin
package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository

class ToggleFavoriteUseCase(private val favoritesRepository: FavoritesRepository) {
    suspend operator fun invoke(city: FavoriteCityDomainModel) =
        favoritesRepository.toggleFavorite(city)
}
```

## favorites\presentation\FavoritesEvent.kt

```kotlin
package com.mhq.fynecast.favorites.presentation

/** One-off events for the Favorites screen (snackbars). */
sealed interface FavoritesEvent {
    data class Removed(val cityName: String) : FavoritesEvent
    data class RemoveFailed(val cityName: String) : FavoritesEvent
}
```

## favorites\presentation\FavoritesUiState.kt

```kotlin
package com.mhq.fynecast.favorites.presentation

import com.mhq.fynecast.favorites.presentation.model.FavoriteCityPresentationModel

/** What the Favorites screen shows. */
sealed interface FavoritesUiState {
    /** The first load has not finished yet. */
    data object Loading : FavoritesUiState

    /** The user has no favorites. */
    data object Empty : FavoritesUiState

    data class Success(val favorites: List<FavoriteCityPresentationModel>) : FavoritesUiState
}
```

## favorites\presentation\FavoritesViewModel.kt

```kotlin
package com.mhq.fynecast.favorites.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.core.domain.location.SelectCityUseCase
import com.mhq.fynecast.core.domain.location.SelectedCity
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshAllFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RestoreFavoriteUseCase
import com.mhq.fynecast.favorites.presentation.mapper.toPresentation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel(
    private val selectCityUseCase: SelectCityUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val deleteFavoriteUseCase: DeleteFavoriteUseCase,
    private val restoreFavoriteUseCase: RestoreFavoriteUseCase,
    private val refreshAllFavoritesUseCase: RefreshAllFavoritesUseCase
) : ViewModel() {

    /** The domain favorites, kept privately: null = the first load has not finished yet. */
    private val domainFavorites: StateFlow<List<FavoriteCityDomainModel>?> =
        getFavoritesUseCase().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /** The screen only ever sees presentation models. */
    val uiState: StateFlow<FavoritesUiState> = domainFavorites
        .map { favorites ->
            when {
                favorites == null -> FavoritesUiState.Loading
                favorites.isEmpty() -> FavoritesUiState.Empty
                else -> FavoritesUiState.Success(favorites.map { it.toPresentation() })
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoritesUiState.Loading
        )

    private val _events = Channel<FavoritesEvent>(Channel.BUFFERED)
    val events: Flow<FavoritesEvent> = _events.receiveAsFlow()

    private var lastRemoved: FavoriteCityDomainModel? = null

    /** The user tapped a favorite row: make that city the one Home shows. */
    fun onFavoriteSelected(favoriteId: String) {
        val city = findFavorite(favoriteId) ?: return
        viewModelScope.launch {
            selectCityUseCase(
                SelectedCity(
                    name = "${city.cityName}, ${city.countryName}",
                    latitude = city.latitude,
                    longitude = city.longitude
                )
            )
        }
    }

    fun onDeleteFavorite(favoriteId: String) {
        val city = findFavorite(favoriteId) ?: return
        viewModelScope.launch {
            try {
                withContext(NonCancellable) { deleteFavoriteUseCase(city.id) }
                lastRemoved = city
                _events.send(FavoritesEvent.Removed(city.cityName))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(FavoritesEvent.RemoveFailed(city.cityName))
            }
        }
    }

    fun onUndoRemove() {
        val city = lastRemoved ?: return
        lastRemoved = null
        viewModelScope.launch { withContext(NonCancellable) { restoreFavoriteUseCase(city) } }
    }

    /** Called when the Favorites tab is shown; the use case skips the call if it ran recently. */
    fun onScreenShown(langCode: String) {
        viewModelScope.launch {
            try {
                refreshAllFavoritesUseCase(langCode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Old temperatures stay; nothing else to do.
            }
        }
    }

    private fun findFavorite(favoriteId: String): FavoriteCityDomainModel? =
        domainFavorites.value?.firstOrNull { it.id == favoriteId }
}
```

## favorites\presentation\mapper\FavoritePresentationMapper.kt

```kotlin
package com.mhq.fynecast.favorites.presentation.mapper

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.presentation.model.FavoriteCityPresentationModel

fun FavoriteCityDomainModel.toPresentation(): FavoriteCityPresentationModel =
    FavoriteCityPresentationModel(
        id = id,
        cityName = cityName,
        countryName = countryName,
        temperatureC = temperatureC,
        temperatureF = temperatureF,
        weatherIconUrl = weatherIconUrl
    )
```

## favorites\presentation\model\FavoriteCityPresentationModel.kt

```kotlin
package com.mhq.fynecast.favorites.presentation.model

/** What the Favorites screen needs to show one city. The UI never sees the domain model. */
data class FavoriteCityPresentationModel(
    /** Stable identity of the row; the UI sends it back to the ViewModel. */
    val id: String,
    val cityName: String,
    val countryName: String,
    val temperatureC: Double,
    val temperatureF: Double,
    val weatherIconUrl: String
)
```

## favorites\ui\components\FavoriteCityItem.kt

```kotlin
package com.mhq.fynecast.favorites.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.format.TemperatureFormatter
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.globalcomponents.unlessArabic
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.favorites.presentation.model.FavoriteCityPresentationModel

/** Corner radius shared with the swipe-to-delete background so the red layer matches the card. */
val FavoriteCardCornerRadius = 24.dp

/**
 * One favorite city, about 68 dp tall: icon bubble, city and country, big light temperature.
 * The tap is handled inside the card so the ripple follows its rounded corners.
 */
@Composable
fun FavoriteCityItem(
    favorite: FavoriteCityPresentationModel,
    isMetric: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onBackground = MaterialTheme.colorScheme.onBackground

    GlassyCard(
        cornerRadius = FavoriteCardCornerRadius,
        padding = 0.dp,
        containerAlpha = 0.06f,
        borderAlpha = 0.15f,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(onBackground.copy(alpha = 0.06f))
            ) {
                AsyncImage(
                    model = "https:${favorite.weatherIconUrl}",
                    placeholder = painterResource(R.drawable.ic_launcher_foreground),
                    error = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.weather_icon),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = favorite.cityName,
                    color = onBackground,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp.unlessArabic()
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = favorite.countryName.uppercase(),
                    color = onBackground.copy(alpha = 0.45f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp.unlessArabic()
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = TemperatureFormatter.format(
                    if (isMetric) favorite.temperatureC else favorite.temperatureF
                ),
                color = onBackground,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.W300,
                maxLines = 1
            )
        }
    }
}

private val previewFavorite = FavoriteCityPresentationModel(
    id = "31.2,29.92",
    cityName = "Alexandria",
    countryName = "Egypt",
    temperatureC = 24.0,
    temperatureF = 75.2,
    weatherIconUrl = "Url"
)

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE)
@Composable
private fun FavoriteCityItemLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        FavoriteCityItem(
            favorite = previewFavorite,
            isMetric = true,
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
private fun FavoriteCityItemDarkPreview() {
    FyneCastTheme(isDarkMode = true) {
        FavoriteCityItem(
            favorite = previewFavorite,
            isMetric = true,
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
```

## favorites\ui\components\SwipeBackground.kt

```kotlin
package com.mhq.fynecast.favorites.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeBackground(dismissState: SwipeToDismissBoxState) {
    val isSwipingToDelete = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
    val color = if (isSwipingToDelete) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
    } else {
        Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color, shape = RoundedCornerShape(FavoriteCardCornerRadius))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (isSwipingToDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.onError
            )
        }
    }
}
```

## favorites\ui\components\UndoDeleteSnackbar.kt

```kotlin
package com.mhq.fynecast.favorites.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen

@Composable
fun UndoDeleteSnackbar(
    snackbarData: SnackbarData?
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .padding(horizontal = 36.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            // Display message
            snackbarData?.visuals?.message?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Display "Undo" Action Button if present
            snackbarData?.visuals?.actionLabel?.let { label ->
                Text(
                    text = label.uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { snackbarData.performAction() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UndoDeleteSnackbarPreview() {
    FyneCastTheme() {
        UndoDeleteSnackbar(snackbarData = null)
    }
}
```

## favorites\ui\screens\FavoritesBlank.kt

```kotlin
package com.mhq.fynecast.favorites.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.BlankPagePlaceholder
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun FavoritesBlank(
    contentPadding: PaddingValues = PaddingValues(8.dp),
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize()
    ) {
        BlankPagePlaceholder(
            blankSectionIcon = Icons.Default.FavoriteBorder,
            blankSectionIconDescription = null,
            blankSectionTitle = stringResource(R.string.no_favorites_yet),
            blankSectionSubtitle = stringResource(R.string.cities_you_like_will_appear_here)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FavoritesBlankPreview() {
    FyneCastTheme() {
        FavoritesBlank()
    }
}
```

## favorites\ui\screens\FavoritesContainer.kt

```kotlin
package com.mhq.fynecast.favorites.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.R
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.ui.globalcomponents.currentLocale
import com.mhq.fynecast.favorites.presentation.FavoritesEvent
import com.mhq.fynecast.favorites.presentation.FavoritesUiState
import com.mhq.fynecast.favorites.presentation.FavoritesViewModel
import com.mhq.fynecast.favorites.ui.components.UndoDeleteSnackbar
import kotlinx.coroutines.launch

@Composable
fun FavoritesContainer(
    favoritesViewModel: FavoritesViewModel = viewModel(factory = AppViewModelFactory),
    onFavoriteSelected: (favoriteId: String) -> Unit,
    isMetric: Boolean,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val undoLabel = stringResource(R.string.undo)
    val uiState by favoritesViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val langCode = currentLocale().language

    /**
     * Brings every favorite's temperature up to date when the tab is shown
     * (the use case skips the call if it ran in the last 10 minutes).
     * */
    LaunchedEffect(langCode) {
        favoritesViewModel.onScreenShown(langCode)
    }

    LaunchedEffect(favoritesViewModel, resources) {
        favoritesViewModel.events.collect { event ->
            launch {   // each event gets its own coroutine so a new message replaces the old one
                snackbarHostState.currentSnackbarData?.dismiss()
                when (event) {
                    is FavoritesEvent.Removed -> {
                        val result = snackbarHostState.showSnackbar(
                            message = resources.getString(R.string.city_removed, event.cityName),
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) favoritesViewModel.onUndoRemove()
                    }
                    is FavoritesEvent.RemoveFailed ->
                        snackbarHostState.showSnackbar(
                            message = resources.getString(R.string.city_remove_failed, event.cityName),
                            duration = SnackbarDuration.Short
                        )
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            FavoritesUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            FavoritesUiState.Empty -> {
                FavoritesBlank(contentPadding = contentPadding)
            }

            is FavoritesUiState.Success -> {
                FavoritesContent(
                    favorites = state.favorites,
                    onFavoriteClick = onFavoriteSelected,
                    onFavoriteDelete = favoritesViewModel::onDeleteFavorite,
                    contentPadding = contentPadding,
                    isMetric = isMetric
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 160.dp)
        ) { data -> UndoDeleteSnackbar(snackbarData = data) }
    }
}
```

## favorites\ui\screens\FavoritesContent.kt

```kotlin
package com.mhq.fynecast.favorites.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.favorites.presentation.model.FavoriteCityPresentationModel
import com.mhq.fynecast.favorites.ui.components.FavoriteCityItem
import com.mhq.fynecast.favorites.ui.components.SwipeBackground

@Composable
fun FavoritesContent(
    favorites: List<FavoriteCityPresentationModel>,
    onFavoriteClick: (favoriteId: String) -> Unit,
    onFavoriteDelete: (favoriteId: String) -> Unit,
    isMetric: Boolean,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    modifier: Modifier = Modifier
) {
    // Same 16 dp side margin as the Home cards.
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = favorites,
                key = { it.id }
            ) { favorite ->
                val currentFavorite by rememberUpdatedState(favorite)
                val currentOnDelete by rememberUpdatedState(onFavoriteDelete)

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            /** Runs once, when the user finishes the swipe. */
                            currentOnDelete(currentFavorite.id)
                        }
                        /**
                         * Never keep a row in the "swiped away" state.
                         * The row leaves the list when the delete lands, and a row brought back
                         * by Undo must start settled so it can be swiped again.
                         * */
                        false
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    modifier = Modifier.animateItem(),
                    backgroundContent = { SwipeBackground(dismissState) },
                    enableDismissFromStartToEnd = false
                ) {
                    FavoriteCityItem(
                        favorite = favorite,
                        isMetric = isMetric,
                        onClick = { onFavoriteClick(favorite.id) }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun FavoritesContentPreview() {
    FyneCastTheme {
        FavoritesContent(
            favorites = emptyList(),
            onFavoriteClick = {},
            onFavoriteDelete = {},
            isMetric = true
        )
    }
}
```

## home\data\api\location\dto\PhotonFeatureDto.kt

```kotlin
package com.mhq.fynecast.home.data.api.location.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhotonFeatureDto(
    @SerialName("properties")
    val properties: PhotonPropertiesDto,
    @SerialName("geometry")
    val geometry: PhotonGeometryDto
)
```

## home\data\api\location\dto\PhotonGeometryDto.kt

```kotlin
package com.mhq.fynecast.home.data.api.location.dto

import kotlinx.serialization.Serializable

@Serializable
data class PhotonGeometryDto(
    val coordinates: List<Double>
)
```

## home\data\api\location\dto\PhotonPropertiesDto.kt

```kotlin
package com.mhq.fynecast.home.data.api.location.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhotonPropertiesDto(
    val name: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val municipality: String? = null,
    val county: String? = null,
    val state: String? = null,
    val country: String? = null,
    val type: String? = null,
    @SerialName("osm_value")
    val osmValue: String? = null
)
```

## home\data\api\location\dto\PhotonResponseDto.kt

```kotlin
package com.mhq.fynecast.home.data.api.location.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhotonResponseDto(
    @SerialName("features")
    val features: List<PhotonFeatureDto>
)
```

## home\data\api\location\LocationApiService.kt

```kotlin
package com.mhq.fynecast.home.data.api.location

import com.mhq.fynecast.home.data.api.location.dto.PhotonResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface LocationApiService {
    @GET("api/")
    suspend fun getSuggestions(
        @Query("q") query: String,
        @Query("lang") lang: String,
        @Query("limit") limit: Int = 5,
        @Query("osm_tag") tag: String = "place"
    ): PhotonResponseDto

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("lang") lang: String,
        @Query("limit") limit: Int = 10
    ): PhotonResponseDto
}
```

## home\data\api\weather\dto\AirQualityDto.kt

```kotlin
package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AirQualityDto(
    @SerialName("us-epa-index")
    val usEpaIndex: Int? = null,
    @SerialName("pm2_5")
    val pm25: Double? = null,
    @SerialName("pm10")
    val pm10: Double? = null
)
```

## home\data\api\weather\dto\AlertDto.kt

```kotlin
package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object (DTO) representing an individual weather alert payload from network APIs.
 */
@Serializable
data class AlertDto(
    @SerialName("headline")
    val headline: String,
    @SerialName("severity")
    val severity: String,
    @SerialName("event")
    val event: String,
    @SerialName("effective")
    val effective: String,
    @SerialName("expires")
    val expires: String,
    @SerialName("desc")
    val description: String,
    @SerialName("instruction")
    val instruction: String = "",
    @SerialName("areas")
    val areas: String = ""
)
```

## home\data\api\weather\dto\AlertsDto.kt

```kotlin
package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wrapper DTO containing a list of weather alert transfer objects.
 */
@Serializable
data class AlertsDto(
    @SerialName("alert")
    val alerts: List<AlertDto> = emptyList()
)
```

## home\data\api\weather\dto\ConditionDto.kt

```kotlin
package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConditionDto(
    @SerialName("text")
    val description: String,
    @SerialName("icon")
    val icon: String
)
```

## home\data\api\weather\dto\CurrentDto.kt

```kotlin
package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentDto(
    @SerialName("temp_c")
    val tempC: Double,
    @SerialName("temp_f")
    val tempF: Double,
    @SerialName("is_day")
    val isDay: Int,
    @SerialName("condition")
    val condition: ConditionDto,
    @SerialName("wind_kph")
    val windKph: Double,
    @SerialName("wind_mph")
    val windMph: Double,
    @SerialName("pressure_mb")
    val pressureMb: Double,
    @SerialName("pressure_in")
    val pressureIn: Double,
    @SerialName("humidity")
    val humidity: Int,
    @SerialName("cloud")
    val cloud: Int,
    @SerialName("feelslike_c")
    val feelsLikeC: Double,
    @SerialName("feelslike_f")
    val feelsLikeF: Double,
    @SerialName("uv")
    val uv: Double,
    @SerialName("last_updated")
    val lastUpdated: String
)
```

## home\data\api\weather\dto\DayDto.kt

```kotlin
package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DayDto(
    @SerialName("maxtemp_c")
    val maxTempC: Double,
    @SerialName("mintemp_c")
    val minTempC: Double,
    @SerialName("maxtemp_f")
    val maxTempF: Double,
    @SerialName("mintemp_f")
    val minTempF: Double,
    @SerialName("maxwind_kph")
    val maxWindKph: Double,
    @SerialName("maxwind_mph")
    val maxWindMph: Double,
    @SerialName("avghumidity")
    val avgHumidity: Int,
    @SerialName("condition")
    val dayCondition: ConditionDto,
    @SerialName("uv")
    val dayUv: Double
)
```

## home\data\api\weather\dto\ForecastDayDto.kt

```kotlin
package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastDayDto(
    @SerialName("date")
    val date: String,
    @SerialName("day")
    val day: DayDto,
    @SerialName("hour")
    val hour: List<HourDto>
)
```

## home\data\api\weather\dto\ForecastDto.kt

```kotlin
package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastDto(
    @SerialName("forecastday")
    val forecastDay: List<ForecastDayDto>
)
```

## home\data\api\weather\dto\HourDto.kt

```kotlin
package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HourDto(
    @SerialName("time")
    val hourTime: String,
    @SerialName("temp_c")
    val hourTempC: Double,
    @SerialName("temp_f")
    val hourTempF: Double,
    @SerialName("condition")
    val hourCondition: ConditionDto,
    @SerialName("is_day")
    val isDay: Int,
    @SerialName("humidity")
    val hourHumidity: Int,
    @SerialName("wind_kph")
    val hourWindKph: Double
)
```

## home\data\api\weather\dto\LocationDto.kt

```kotlin
package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationDto(
    @SerialName("name")
    val cityName: String,
    @SerialName("region")
    val cityRegion: String,
    @SerialName("country")
    val country: String,
    @SerialName("lat")
    val cityLatitude: Double,
    @SerialName("lon")
    val cityLongitude: Double,
    @SerialName("localtime")
    val cityLocaltime: String,
    @SerialName("tz_id")
    val timeZoneId: String? = null
)
```

## home\data\api\weather\dto\WeatherApiResponse.kt

```kotlin
package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherApiResponse(
    @SerialName("location")
    val location: LocationDto,
    @SerialName("current")
    val current: CurrentDto,
    @SerialName("forecast")
    val forecast: ForecastDto,
    @SerialName("alerts")
    val alerts: AlertsDto? = null
)
```

## home\data\api\weather\WeatherApiService.kt

```kotlin
package com.mhq.fynecast.home.data.api.weather

import com.mhq.fynecast.home.data.api.weather.dto.WeatherApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v1/forecast.json")
    suspend fun getWeatherData(
        @Query("key") key: String,
        @Query("q") city: String,
        @Query("days") days: Int = 7,
        @Query("aqi") airQualityInfo: String = "no",
        @Query("alerts") alerts: String = "yes",
        @Query("lang") languageCode: String
    ): WeatherApiResponse
}
```

## home\data\datasource\location\AndroidPlaceNameSource.kt

```kotlin
package com.mhq.fynecast.home.data.datasource.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/** Place names from Android's built-in Geocoder
 * (Google backend): works for any app language. */
class AndroidPlaceNameSource(context: Context) : PlaceNameSource {

    private val appContext = context.applicationContext

    override suspend fun nameAt(latitude: Double, longitude: Double, langCode: String): String? {
        val geocoder = geocoderFor(langCode) ?: return null
        val address = withTimeoutOrNull(LOOKUP_TIMEOUT_MILLIS.milliseconds) {
            reverseLookup(geocoder, latitude, longitude)
        } ?: return null
        return displayName(address)
    }

    override suspend fun localize(
        name: String,
        latitude: Double,
        longitude: Double,
        langCode: String
    ): String? {
        val geocoder = geocoderFor(langCode) ?: return null
        val addresses = withTimeoutOrNull(LOOKUP_TIMEOUT_MILLIS.milliseconds) {
            forwardLookup(geocoder, name)
        } ?: return null
        val closest = addresses
            .filter { it.hasLatitude() && it.hasLongitude() }
            .minByOrNull {
                distanceMeters(
                    latitude,
                    longitude,
                    it.latitude,
                    it.longitude
                )
            }
            ?: return null
        if (distanceMeters(
                latitude,
                longitude,
                closest.latitude,
                closest.longitude
            )
            > MAX_MATCH_DISTANCE_METERS
        ) return null
        return displayName(closest)
    }

    private fun geocoderFor(langCode: String): Geocoder? =
        if (Geocoder.isPresent())
            Geocoder(appContext, Locale.forLanguageTag(langCode))
        else
            null

    @Suppress("DEPRECATION")
    private suspend fun reverseLookup(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): Address? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive)
                                continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive)
                                continuation.resume(null)
                        }
                    }
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                } catch (e: IOException) {
                    null
                }
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun forwardLookup(geocoder: Geocoder, name: String): List<Address> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(
                    name,
                    MAX_RESULTS,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(emptyList())
                        }
                    }
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    geocoder.getFromLocationName(name, MAX_RESULTS) ?: emptyList()
                } catch (e: IOException) {
                    emptyList()
                }
            }
        }

    /** "City, Country" (or just the name when both are the same). */
    private fun displayName(address: Address): String? {
        val place = listOf(address.locality, address.subAdminArea, address.adminArea)
            .firstOrNull { !it.isNullOrBlank() } ?: address.countryName
        if (place.isNullOrBlank()) return null
        val country = address.countryName
        return if (!country.isNullOrBlank() && country != place) "$place, $country" else place
    }

    private fun distanceMeters(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double
    ): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            fromLatitude,
            fromLongitude,
            toLatitude,
            toLongitude,
            result
        )
        return result[0]
    }

    private companion object {
        const val LOOKUP_TIMEOUT_MILLIS = 3_000L
        const val MAX_RESULTS = 5
        const val MAX_MATCH_DISTANCE_METERS = 50_000f
    }
}
```

## home\data\datasource\location\DeviceLocationDataSource.kt

```kotlin
package com.mhq.fynecast.home.data.datasource.location

import android.location.Location

/** The phone's position from the device itself (no permission or settings checks here). */
interface DeviceLocationDataSource {
    /** A fresh fix, else a recent cached one, else null. Never throws except on cancellation. */
    suspend fun getCurrentLocation(): Location?
}
```

## home\data\datasource\location\FusedDeviceLocationDataSource.kt

```kotlin
package com.mhq.fynecast.home.data.datasource.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

class FusedDeviceLocationDataSource(
    private val fusedLocationClient: FusedLocationProviderClient
) : DeviceLocationDataSource {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Location? =
        try {
            withTimeoutOrNull(FRESH_FIX_TIMEOUT_MILLIS.milliseconds) { requestFreshLocation() }
                ?: withTimeoutOrNull(LAST_KNOWN_TIMEOUT_MILLIS.milliseconds) { lastKnownLocation() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null   // e.g. permission revoked between the check and the request
        }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellation.cancel() }

            fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
                .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
                .addOnCanceledListener { if (continuation.isActive) continuation.resume(null) }
        }

    /** A recent cached fix only; an old one could belong to a different city. */
    @SuppressLint("MissingPermission")
    private suspend fun lastKnownLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (continuation.isActive) continuation.resume(location?.takeIf { it.ageMillis() <= MAX_CACHED_FIX_AGE_MILLIS })
                }
                .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
                .addOnCanceledListener { if (continuation.isActive) continuation.resume(null) }
        }

    private fun Location.ageMillis(): Long =
        (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000

    private companion object {
        const val LAST_KNOWN_TIMEOUT_MILLIS = 2_000L
        const val FRESH_FIX_TIMEOUT_MILLIS = 8_000L
        const val MAX_CACHED_FIX_AGE_MILLIS = 30 * 60 * 1000L
    }
}
```

## home\data\datasource\location\LocationRemoteDataSource.kt

```kotlin
package com.mhq.fynecast.home.data.datasource.location

import com.mhq.fynecast.home.data.api.location.dto.PhotonResponseDto

/** Where place search and reverse geocoding come from over the network. Returns raw API data. */
interface LocationRemoteDataSource {
    suspend fun searchPlaces(query: String, lang: String): PhotonResponseDto
    suspend fun reverseGeocode(lat: Double, lon: Double, lang: String): PhotonResponseDto
}
```

## home\data\datasource\location\PhotonLocationRemoteDataSource.kt

```kotlin
package com.mhq.fynecast.home.data.datasource.location

import com.mhq.fynecast.home.data.api.location.LocationApiService
import com.mhq.fynecast.home.data.api.location.dto.PhotonResponseDto

class PhotonLocationRemoteDataSource(
    private val locationApiService: LocationApiService
) : LocationRemoteDataSource {

    override suspend fun searchPlaces(query: String, lang: String): PhotonResponseDto =
        locationApiService.getSuggestions(query = query, lang = photonLang(lang))

    override suspend fun reverseGeocode(lat: Double, lon: Double, lang: String): PhotonResponseDto =
        locationApiService.reverseGeocode(lat = lat, lon = lon, lang = photonLang(lang))

    /** Photon's public server only knows English, German and French;
     * anything else falls back to English. */
    private fun photonLang(lang: String): String =
        if (lang in PHOTON_LANGUAGES)
            lang
        else
            "en"

    private companion object {
        val PHOTON_LANGUAGES = setOf("en", "de", "fr")
    }
}
```

## home\data\datasource\location\PlaceNameSource.kt

```kotlin
package com.mhq.fynecast.home.data.datasource.location

interface PlaceNameSource {
    /** The city and country at these coordinates,
     * written in [langCode], or null. */
    suspend fun nameAt(
        latitude: Double,
        longitude: Double,
        langCode: String
    ): String?

    /**
     * [name] rewritten in [langCode],
     * for the place within about 50 km of the coordinates.
     * Null when nothing close enough is found,
     * so callers keep the name they already have.
     */
    suspend fun localize(
        name: String,
        latitude: Double,
        longitude: Double,
        langCode: String
    ): String?
}
```

## home\data\datasource\weather\WeatherRemoteDataSource.kt

```kotlin
package com.mhq.fynecast.home.data.datasource.weather

import com.mhq.fynecast.home.data.api.weather.dto.WeatherApiResponse

/** Where weather comes from over the network. Returns raw API data, never domain models. */
interface WeatherRemoteDataSource {
    suspend fun fetchWeather(query: String, lang: String): WeatherApiResponse
}
```

## home\data\datasource\weather\WeatherRemoteDataSourceImpl.kt

```kotlin
package com.mhq.fynecast.home.data.datasource.weather

import com.mhq.fynecast.home.data.api.weather.WeatherApiService
import com.mhq.fynecast.home.data.api.weather.dto.WeatherApiResponse

class WeatherRemoteDataSourceImpl(
    private val weatherApiService: WeatherApiService,
    private val apiKey: String
) : WeatherRemoteDataSource {

    override suspend fun fetchWeather(query: String, lang: String): WeatherApiResponse =
        weatherApiService.getWeatherData(
            key = apiKey,
            city = query,
            languageCode = lang
        )
}
```

## home\data\infra\AndroidLocationStatusChecker.kt

```kotlin
package com.mhq.fynecast.home.data.infra

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.mhq.fynecast.home.domain.repository.LocationProviderStatusChecker

class AndroidLocationStatusChecker(
    private val appContext: Context
) : LocationProviderStatusChecker {

    override fun hasLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    override fun isLocationEnabled(): Boolean {
        val locationManager =
            appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }
}
```

## home\data\mapper\AlertDtoMapper.kt

```kotlin
package com.mhq.fynecast.home.data.mapper

import com.mhq.fynecast.core.domain.weather.AlertDomainModel
import com.mhq.fynecast.home.data.api.weather.dto.AlertDto
import java.time.OffsetDateTime

/**
 * Calculates a unique string identifier for an alert based on its event type, effective timestamp, and affected areas.
 */
fun AlertDto.calculateId(): String = "${event}_${effective}_$areas"

/**
 * Maps an [AlertDto] from the weather API response into the domain model [AlertDomainModel].
 *
 * Edge case: Safely parses ISO date strings into epoch milliseconds, defaulting to `0L` or `null` if formatting fails.
 */
fun AlertDto.toDomain(): AlertDomainModel =
    AlertDomainModel(
        id = calculateId(),
        headline = headline,
        severity = severity,
        event = event,
        description = description,
        instruction = instruction,
        effectiveMillis = parseToEpochMillis(effective) ?: 0L,
        expiresMillis = parseToEpochMillis(expires)
    )

/**
 * Parses an ISO-8601 date string into epoch milliseconds.
 *
 * Edge case: A date with no offset is treated as unknown rather than guessed in the wrong time zone; returns `null` on parsing exceptions.
 */
private fun parseToEpochMillis(isoString: String): Long? =
    runCatching {
        OffsetDateTime
            .parse(isoString)
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
```

## home\data\mapper\WeatherMapper.kt

```kotlin
package com.mhq.fynecast.home.data.mapper

import com.mhq.fynecast.home.data.api.weather.dto.WeatherApiResponse
import com.mhq.fynecast.core.domain.weather.DailyForecastDomainModel
import com.mhq.fynecast.core.domain.weather.HourlyForecastDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherStatsDomainModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val API_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val API_LOCAL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm")
private const val HOURLY_WINDOW = 24

private fun parseDateTime(raw: String, formatter: DateTimeFormatter): LocalDateTime? =
    runCatching { LocalDateTime.parse(raw, formatter) }.getOrNull()

fun WeatherApiResponse.toDomain(): WeatherDomainModel {
    val domainStats = WeatherStatsDomainModel(
        humidity = this.current.humidity,
        windKph = this.current.windKph,
        windMph = this.current.windMph,
        pressureMb = this.current.pressureMb,
        pressureIn = this.current.pressureIn,
        uvIndex = this.current.uv
    )

    val domainDailyList = forecast.forecastDay.mapNotNull { dayDto ->
        val date =
            runCatching { LocalDate.parse(dayDto.date) }.getOrNull() ?: return@mapNotNull null
        DailyForecastDomainModel(
            date = date,
            iconUrl = dayDto.day.dayCondition.icon,
            description = dayDto.day.dayCondition.description,
            maxTempC = dayDto.day.maxTempC,
            maxTempF = dayDto.day.maxTempF,
            minTempC = dayDto.day.minTempC,
            minTempF = dayDto.day.minTempF
        )
    }

    // Hours of the city's current local hour onward, across all forecast days.
    val allHours = forecast.forecastDay
        .flatMap { it.hour }
        .mapNotNull { hourDto ->
            parseDateTime(hourDto.hourTime, API_DATE_TIME)?.let { time ->
                HourlyForecastDomainModel(
                    time = time,
                    iconUrl = hourDto.hourCondition.icon,
                    tempC = hourDto.hourTempC,
                    tempF = hourDto.hourTempF
                )
            }
        }

    val currentLocalHour = parseDateTime(
        location.cityLocaltime,
        API_LOCAL_TIME
    )?.truncatedTo(ChronoUnit.HOURS)

    val domainHourlyList = (
            if (currentLocalHour == null)
                allHours
            else
                allHours.filter { !it.time.isBefore(currentLocalHour) }
            ).take(HOURLY_WINDOW)

    val domainAlerts = alerts?.alerts?.map { it.toDomain() }?.distinctBy { it.id } ?: emptyList()

    return WeatherDomainModel(
        cityName = this.location.cityName,
        country = this.location.country,
        latitude = this.location.cityLatitude,
        longitude = this.location.cityLongitude,
        currentTempC = this.current.tempC,
        currentTempF = this.current.tempF,
        conditionText = this.current.condition.description,
        conditionIcon = this.current.condition.icon,
        lastUpdated = parseDateTime(current.lastUpdated, API_DATE_TIME),
        timeZoneId = this.location.timeZoneId,
        alerts = domainAlerts,
        stats = domainStats,
        dailyForecasts = domainDailyList,
        hourlyForecasts = domainHourlyList
    )
}
```

## home\data\repoimpl\location\DefaultLocationRepoImpl.kt

```kotlin
package com.mhq.fynecast.home.data.repoimpl.location

import com.mhq.fynecast.core.domain.location.GeoCoordinates
import com.mhq.fynecast.home.data.datasource.location.DeviceLocationDataSource
import com.mhq.fynecast.home.domain.repository.DeviceLocationRepository
import com.mhq.fynecast.home.domain.repository.LocationProviderStatusChecker

class DefaultLocationRepoImpl(
    private val deviceLocationDataSource: DeviceLocationDataSource,
    private val statusChecker: LocationProviderStatusChecker
) : DeviceLocationRepository {

    override suspend fun getCurrentLocation(): GeoCoordinates? {
        if (!statusChecker.hasLocationPermissions() || !statusChecker.isLocationEnabled()) return null

        return deviceLocationDataSource.getCurrentLocation()
            ?.let { GeoCoordinates(latitude = it.latitude, longitude = it.longitude) }
    }
}
```

## home\data\repoimpl\location\PhotonLocationRepoImpl.kt

```kotlin
package com.mhq.fynecast.home.data.repoimpl.location

import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.core.domain.location.LocationRepository
import com.mhq.fynecast.home.data.datasource.location.LocationRemoteDataSource
import com.mhq.fynecast.home.data.datasource.location.PlaceNameSource
import kotlinx.coroutines.CancellationException

class PhotonLocationRepoImpl(
    private val remoteDataSource: LocationRemoteDataSource,
    private val placeNameSource: PlaceNameSource
) : LocationRepository {

    override suspend fun getCitySuggestions(
        query: String,
        lang: String
    ): List<CitySuggestionModel> {
        return try {
            val response = remoteDataSource.searchPlaces(query, lang)
            response.features.map { feature ->
                val coordinates = feature.geometry.coordinates
                val props = feature.properties
                val nameParts = listOfNotNull(
                    props.name,
                    props.country
                ).distinct().filter { it.isNotBlank() }

                CitySuggestionModel(
                    fullName = nameParts.joinToString(", "),
                    latitude = coordinates.getOrNull(1) ?: 0.0,
                    longitude = coordinates.getOrNull(0) ?: 0.0
                )
            }.distinctBy { it.fullName }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    override suspend fun getCityNameOffMap(
        lat: Double,
        lon: Double,
        lang: String
    ): CitySuggestionModel? {
        placeNameSource.nameAt(lat, lon, lang)?.let { name ->
            return CitySuggestionModel(fullName = name, latitude = lat, longitude = lon)
        }
        return try {
            val response = remoteDataSource.reverseGeocode(lat, lon, lang)
            val bestFeature = response.features.firstOrNull { feature ->
                val props = feature.properties
                !props.state.isNullOrBlank() ||
                        !props.city.isNullOrBlank() ||
                        !props.town.isNullOrBlank() ||
                        !props.municipality.isNullOrBlank() ||
                        !props.village.isNullOrBlank()
            } ?: response.features.firstOrNull()
            val props = bestFeature?.properties ?: return null
            val cityName = when {
                !props.city.isNullOrBlank() -> props.city
                !props.town.isNullOrBlank() -> props.town
                !props.municipality.isNullOrBlank() -> props.municipality
                !props.village.isNullOrBlank() -> props.village
                !props.state.isNullOrBlank() -> props.state
                !props.county.isNullOrBlank() -> props.county
                else -> props.name ?: return null
            }
            val fullName = if (!props.country.isNullOrBlank() && cityName != props.country) {
                "$cityName, ${props.country}"
            } else {
                cityName
            }
            CitySuggestionModel(fullName = fullName, latitude = lat, longitude = lon)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    override suspend fun getLocalizedPlaceName(
        name: String,
        lat: Double,
        lon: Double,
        lang: String
    ): String? = placeNameSource.localize(name, lat, lon, lang)
}
```

## home\data\repoimpl\weather\InMemoryCurrentWeatherStore.kt

```kotlin
package com.mhq.fynecast.home.data.repoimpl.weather

import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.core.domain.weather.CurrentWeatherStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryCurrentWeatherStore : CurrentWeatherStore {
    private val _current = MutableStateFlow<WeatherDomainModel?>(null)
    override val current: StateFlow<WeatherDomainModel?> = _current.asStateFlow()
    override fun publish(weather: WeatherDomainModel) { _current.value = weather }
}
```

## home\data\repoimpl\weather\WeatherRepoImpl.kt

```kotlin
package com.mhq.fynecast.home.data.repoimpl.weather

import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherRepository
import com.mhq.fynecast.home.data.datasource.weather.WeatherRemoteDataSource
import com.mhq.fynecast.home.data.mapper.toDomain

class WeatherRepoImpl(
    private val remoteDataSource: WeatherRemoteDataSource
) : WeatherRepository {

    override suspend fun fetchWeather(query: String, lang: String): WeatherDomainModel =
        remoteDataSource.fetchWeather(query, lang).toDomain()
}
```

## home\domain\mapper\WeatherToFavoriteMapper.kt

```kotlin
package com.mhq.fynecast.home.domain.mapper

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherDomainModel

fun WeatherDomainModel.toFavoriteCity() = FavoriteCityDomainModel(
    cityName = cityName,
    countryName = country,
    latitude = latitude,
    longitude = longitude,
    temperatureC = currentTempC,
    temperatureF = currentTempF,
    weatherIconUrl = conditionIcon
)

/** Same as [toFavoriteCity], but uses the name the user picked ("City, Country") when there is one. */
fun WeatherDomainModel.toFavoriteCity(displayName: String): FavoriteCityDomainModel {
    val base = toFavoriteCity()
    if (displayName.isBlank()) return base
    return base.copy(
        cityName = displayName.substringBefore(",").trim(),
        countryName = displayName.substringAfter(",", "").trim().ifBlank { base.countryName }
    )
}
```

## home\domain\models\SavedCityDomainModel.kt

```kotlin
package com.mhq.fynecast.home.domain.models

data class SavedCityDomainModel(
    val name: String,
    val latitude: Double,
    val longitude: Double
)
```

## home\domain\models\StartupLocation.kt

```kotlin
package com.mhq.fynecast.home.domain.models

data class StartupLocation(
    val query: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val fromDevice: Boolean
)
```

## home\domain\repository\DeviceLocationRepository.kt

```kotlin
package com.mhq.fynecast.home.domain.repository

import com.mhq.fynecast.core.domain.location.GeoCoordinates

interface DeviceLocationRepository {
    suspend fun getCurrentLocation(): GeoCoordinates?
}
```

## home\domain\repository\LocationProviderStatusChecker.kt

```kotlin
package com.mhq.fynecast.home.domain.repository

interface LocationProviderStatusChecker {
    fun hasLocationPermissions(): Boolean
    fun isLocationEnabled(): Boolean
}
```

## home\domain\usecases\GetCurrentLocationUseCase.kt

```kotlin
package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.core.domain.location.GeoCoordinates
import com.mhq.fynecast.home.domain.repository.DeviceLocationRepository

class GetCurrentLocationUseCase(
    private val deviceLocationRepository: DeviceLocationRepository
) {
    suspend operator fun invoke(): GeoCoordinates? {
        return deviceLocationRepository.getCurrentLocation()
    }
}
```

## home\domain\usecases\GetLastSearchedCityUseCase.kt

```kotlin
package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.home.domain.models.SavedCityDomainModel
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class GetLastSearchedCityUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(): SavedCityDomainModel? {
        val prefs = userPreferencesRepository.getPreferences()
        val name = prefs.lastSearchedCityName
        val lat = prefs.lastSearchedCityLat
        val lon = prefs.lastSearchedCityLon
        return if (name != null && lat != null && lon != null) {
            SavedCityDomainModel(name, lat, lon)
        } else null
    }
}
```

## home\domain\usecases\GetLocationSuggestionsUseCase.kt

```kotlin
package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.core.domain.location.LocationRepository

class GetLocationSuggestionsUseCase(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(query: String, lang: String): List<CitySuggestionModel> {
        val sanitizedQuery = query.trim()
        if (sanitizedQuery.length <= 2) {
            return emptyList()
        }
        return locationRepository.getCitySuggestions(sanitizedQuery, lang)
    }
}
```

## home\domain\usecases\GetWeatherUseCase.kt

```kotlin
package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherRepository

class GetWeatherUseCase(private val weatherRepository: WeatherRepository) {
    suspend operator fun invoke(query: String, lang: String): WeatherDomainModel =
        weatherRepository.fetchWeather(query, lang)
}
```

## home\domain\usecases\LoadCurrentWeatherUseCase.kt

```kotlin
package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.core.domain.weather.CurrentWeatherStore
import com.mhq.fynecast.core.domain.weather.WeatherRepository

class LoadCurrentWeatherUseCase(
    private val weatherRepository: WeatherRepository,
    private val currentWeatherStore: CurrentWeatherStore
) {
    suspend operator fun invoke(query: String, lang: String): WeatherDomainModel =
        weatherRepository
            .fetchWeather(query, lang)
            .also(currentWeatherStore::publish)
}
```

## home\domain\usecases\LocalizePlaceNameUseCase.kt

```kotlin
package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.core.domain.location.LocationRepository

class LocalizePlaceNameUseCase(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(
        name: String,
        latitude: Double,
        longitude: Double,
        lang: String
    ): String? = locationRepository.getLocalizedPlaceName(
        name,
        latitude,
        longitude,
        lang
    )
}
```

## home\domain\usecases\ObserveCurrentLocationUseCase.kt

```kotlin
package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.core.domain.location.TrackedLocation
import com.mhq.fynecast.core.domain.weather.CurrentWeatherStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCurrentLocationUseCase(private val currentWeatherStore: CurrentWeatherStore) {
    operator fun invoke(): Flow<TrackedLocation?> =
        currentWeatherStore.current.map { weatherDomainModel ->
            weatherDomainModel?.let { TrackedLocation(it.cityName, it.latitude, it.longitude) }
        }
}
```

## home\domain\usecases\ObserveFavoriteStatusUseCase.kt

```kotlin
package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.favorites.domain.models.favoriteId
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

class ObserveFavoriteStatusUseCase(private val favoritesRepository: FavoritesRepository) {

    /**
     * The id of the saved favorite that stands for the city on screen, or null when it is not one.
     *
     * [places] are every coordinate pair that may identify the city: the weather service snaps a
     * query to its own point, so the point it reports can differ from the one we asked about.
     * A favorite counts if it was saved under any of them.
     */
    operator fun invoke(places: List<Pair<Double, Double>>): Flow<String?> {
        val ids = places.map { (latitude, longitude) -> favoriteId(latitude, longitude) }.distinct()
        if (ids.isEmpty()) return flowOf(null)
        return combine(ids.map { favoritesRepository.observeIsFavorite(it) }) { isSaved ->
            ids.zip(isSaved.toList()).firstOrNull { (_, saved) -> saved }?.first
        }
    }
}
```

## home\domain\usecases\ObserveSignedInUserIdUseCase.kt

```kotlin
package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import kotlinx.coroutines.flow.Flow

class ObserveSignedInUserIdUseCase(private val currentUserProvider: CurrentUserProvider) {
    operator fun invoke(): Flow<String?> = currentUserProvider.observeUserId()
}
```

## home\domain\usecases\ResolveStartupLocationUseCase.kt

```kotlin
package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.home.domain.models.StartupLocation
import kotlinx.coroutines.CancellationException

/** Where to load weather on startup: GPS (when allowed) → last searched city → default city. */
class ResolveStartupLocationUseCase(
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val getLastSearchedCityUseCase: GetLastSearchedCityUseCase,
    private val defaultCity: String = "Cairo"
) {
    suspend operator fun invoke(useDeviceLocation: Boolean): StartupLocation {
        val coordinates = if (useDeviceLocation) {
            try {
                getCurrentLocationUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        } else null

        if (coordinates != null) {
            return StartupLocation(
                query = "${coordinates.latitude},${coordinates.longitude}",
                name = null,
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
                fromDevice = true
            )
        }
        val savedCity = getLastSearchedCityUseCase()
        return if (savedCity != null) {
            StartupLocation(
                query = "${savedCity.latitude},${savedCity.longitude}",
                name = savedCity.name,
                latitude = savedCity.latitude,
                longitude = savedCity.longitude,
                fromDevice = false
            )
        } else {
            StartupLocation(
                query = defaultCity,
                name = null,
                latitude = null,
                longitude = null,
                fromDevice = false
            )
        }
    }
}
```

## home\presentation\HomeRetryAction.kt

```kotlin
package com.mhq.fynecast.home.presentation

enum class HomeRetryAction {
    RequestLocation,
    ReloadPlace
}
```

## home\presentation\HomeStateContainer.kt

```kotlin
package com.mhq.fynecast.home.presentation

import com.mhq.fynecast.home.presentation.model.CitySuggestionPresentationModel

data class HomeStateContainer(
    val uiState: HomeUiState = HomeUiState.Loading,
    val selectedCityName: String = "",
    val citySuggestions: List<CitySuggestionPresentationModel> = emptyList(),
    val isFavorite: Boolean = false,
    /** True while a new city loads and the previous weather is still on screen. */
    val isRefreshing: Boolean = false
)
```

## home\presentation\HomeUiState.kt

```kotlin
package com.mhq.fynecast.home.presentation

import com.mhq.fynecast.home.presentation.model.WeatherPresentationModel

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val weather: WeatherPresentationModel) : HomeUiState
    data class Error(val retry: HomeRetryAction) : HomeUiState
}
```

## home\presentation\HomeViewModel.kt

```kotlin
package com.mhq.fynecast.home.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.core.domain.location.ObserveSelectedCityUseCase
import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshFavoriteSnapshotUseCase
import com.mhq.fynecast.favorites.domain.usecases.ToggleFavoriteUseCase
import com.mhq.fynecast.home.domain.mapper.toFavoriteCity
import com.mhq.fynecast.home.domain.usecases.GetLocationSuggestionsUseCase
import com.mhq.fynecast.home.domain.usecases.LoadCurrentWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.LocalizePlaceNameUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveFavoriteStatusUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveSignedInUserIdUseCase
import com.mhq.fynecast.home.domain.usecases.ResolveStartupLocationUseCase
import com.mhq.fynecast.home.presentation.mapper.toPresentation
import com.mhq.fynecast.map.domain.usecases.ReverseGeocodeLocationUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SaveLastSearchedCityUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

class HomeViewModel(
    private val loadCurrentWeatherUseCase: LoadCurrentWeatherUseCase,
    private val resolveStartupLocationUseCase : ResolveStartupLocationUseCase,
    private val observeSelectedCityUseCase: ObserveSelectedCityUseCase,
    private val getLocationSuggestionsUseCase: GetLocationSuggestionsUseCase,
    private val observeSignedInUserIdUseCase: ObserveSignedInUserIdUseCase,
    private val observeFavoriteStatusUseCase: ObserveFavoriteStatusUseCase,
    private val refreshFavoriteSnapshotUseCase: RefreshFavoriteSnapshotUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteFavoriteUseCase: DeleteFavoriteUseCase,
    private val saveLastSearchedCityUseCase: SaveLastSearchedCityUseCase,
    private val reverseGeocodeLocationUseCase: ReverseGeocodeLocationUseCase,
    private val localizePlaceNameUseCase: LocalizePlaceNameUseCase,
    private val defaultCityFallback: String = "Cairo"
) : ViewModel() {

    private val _state = MutableStateFlow(HomeStateContainer())
    val state: StateFlow<HomeStateContainer> = _state.asStateFlow()

    /**
     * The domain weather behind the Success state, kept privately: the screen only ever sees
     * presentation models. It is null whenever the state is not Success.
     */
    private var domainWeather: WeatherDomainModel? = null

    /** The domain suggestions behind the search dropdown; the screen sends back an id. */
    private var domainSuggestions: List<CitySuggestionModel> = emptyList()

    private var loadTarget: LoadTarget = LoadTarget.Startup(useDeviceLocation = false)
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var favoriteJob: Job? = null

    /** The id the saved favorite for the city on screen has, or null when it is not a favorite. */
    private var savedFavoriteId: String? = null
    private var lastRefreshTime = 0L
    private val refreshCooldownMillis = 60 * 1000L
    private var loadedLangCode: String? = null
    private var latestLangCode: String? = null

    /** The place we queried (not the weather service's snapped station) & how its name was made. */
    private var shownSpot: ShownSpot? = null

    /** True once any load has been requested; the UI uses it to ask for location only once. */
    var isInitialLaunchPerformed = false
        private set

    private sealed interface LoadTarget {
        val retry: HomeRetryAction

        data class Startup(val useDeviceLocation: Boolean) : LoadTarget {
            override val retry = HomeRetryAction.RequestLocation
        }

        data class Place(
            val name: String,
            val latitude: Double,
            val longitude: Double,
            val relocalizeName: Boolean = false,
            val nameFromDevice: Boolean = false
        ) : LoadTarget {
            override val retry = HomeRetryAction.ReloadPlace
        }
    }

    private class ShownSpot(
        val latitude: Double,
        val longitude: Double,
        val fromDevice: Boolean
    )

    private class LoadedCity(
        val weather: WeatherDomainModel,
        val name: String,
        val spot: ShownSpot
    )

    init {
        observeSignedInUserIdUseCase()
            .drop(1)
            .onEach { resetForNewSession() }
            .launchIn(viewModelScope)

        observeSelectedCityUseCase()
            .onEach { city ->
                selectCity(city.name, city.latitude, city.longitude, latestLangCode ?: "en")
            }
            .launchIn(viewModelScope)
    }

    fun onLocationPermissionResult(isGranted: Boolean, langCode: String) {
        load(LoadTarget.Startup(useDeviceLocation = isGranted), langCode)
    }

    /** The user picked a row of the search dropdown. */
    fun onSuggestionSelected(suggestionId: String, langCode: String) {
        val suggestion = domainSuggestions.firstOrNull { it.toPresentation().id == suggestionId }
            ?: return
        selectCity(suggestion.fullName, suggestion.latitude, suggestion.longitude, langCode)
    }

    private fun selectCity(
        suggestionName: String,
        lat: Double,
        lon: Double,
        langCode: String
    ) {
        viewModelScope.launch {
            saveLastSearchedCityUseCase(
                name = suggestionName,
                lat = lat,
                lon = lon
            )
        }
        load(
            /**
             * Search results only come in English, German or French;
             * other languages get the name rewritten in the app language once the weather is in.
             * */
            LoadTarget.Place(
                suggestionName,
                lat,
                lon,
                relocalizeName = langCode != "en"
            ),
            langCode
        )
    }

    /** Reloads the place that failed. Startup failures retry through the location flow instead. */
    fun onRetryClicked(langCode: String) = load(loadTarget, langCode)

    private fun load(target: LoadTarget, langCode: String) {
        loadTarget = target
        isInitialLaunchPerformed = true

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                if (it.uiState is HomeUiState.Success) it.copy(isRefreshing = true)
                else it.copy(uiState = HomeUiState.Loading)
            }
            try {
                val loaded = withTimeout(LOAD_TIMEOUT_MILLIS.milliseconds) {
                    fetchCity(target, langCode)
                }
                domainWeather = loaded.weather
                _state.update {
                    it.copy(
                        uiState = HomeUiState.Success(loaded.weather.toPresentation()),
                        selectedCityName = loaded.name,
                        isRefreshing = false
                    )
                }
                shownSpot = loaded.spot
                loadedLangCode = langCode
                onWeatherShown(loaded.weather)
                if (target is LoadTarget.Place && target.relocalizeName && !target.nameFromDevice) {
                    viewModelScope.launch {
                        saveLastSearchedCityUseCase(
                            name = loaded.name,
                            lat = target.latitude,
                            lon = target.longitude
                        )
                    }
                }
                latestLangCode?.takeIf { it != langCode }?.let { onLanguageChanged(it) }
            } catch (e: TimeoutCancellationException) {
                showError(target.retry)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("HomeViewModel", "Load failed", e)
                showError(target.retry)
            }
        }
    }

    private fun showError(retry: HomeRetryAction) {
        domainWeather = null
        _state.update {
            it.copy(
                uiState = HomeUiState.Error(retry),
                isRefreshing = false
            )
        }
    }

    private suspend fun fetchCity(target: LoadTarget, langCode: String): LoadedCity =
        when (target) {
            is LoadTarget.Place -> {
                val weather = loadCurrentWeatherUseCase(
                    "${target.latitude},${target.longitude}",
                    langCode
                )
                val name = when {
                    !target.relocalizeName -> target.name
                    target.nameFromDevice ->
                        lookupCityName(target.latitude, target.longitude, langCode) ?: target.name

                    else ->
                        localizePlaceName(target.name, target.latitude, target.longitude, langCode)
                            ?: target.name
                }
                LoadedCity(
                    weather,
                    name,
                    ShownSpot(target.latitude, target.longitude, target.nameFromDevice)
                )
            }

            is LoadTarget.Startup -> {
                delay(300.milliseconds)
                val spot = resolveStartupLocationUseCase(target.useDeviceLocation)
                val weather = loadCurrentWeatherUseCase(spot.query, langCode)
                val deviceName = if (spot.fromDevice) {
                    lookupCityName(spot.latitude, spot.longitude, langCode)
                } else {
                    null
                }
                val name = spot.name
                    ?: deviceName
                    ?: "${weather.cityName}, ${weather.country}"
                LoadedCity(
                    weather,
                    name,
                    ShownSpot(
                        latitude = spot.latitude ?: weather.latitude,
                        longitude = spot.longitude ?: weather.longitude,
                        fromDevice = spot.fromDevice
                    )
                )
            }
        }

    fun onRefreshRequested(langCode: String) {
        val now = System.currentTimeMillis()
        if (now - lastRefreshTime < refreshCooldownMillis) return
        if (loadJob?.isActive == true) return
        val weather = domainWeather ?: return

        loadJob = viewModelScope.launch {
            try {
                val fresh =
                    loadCurrentWeatherUseCase("${weather.latitude},${weather.longitude}", langCode)
                domainWeather = fresh
                _state.update { it.copy(uiState = HomeUiState.Success(fresh.toPresentation())) }
                loadedLangCode = langCode
                onWeatherShown(fresh)
                lastRefreshTime = System.currentTimeMillis()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh weather", e)
            }
        }
    }

    fun onLanguageChanged(langCode: String) {
        latestLangCode = langCode
        if (langCode == loadedLangCode) return
        val weather = domainWeather ?: return
        val spot = shownSpot
        load(
            LoadTarget.Place(
                name = _state.value.selectedCityName,
                latitude = spot?.latitude ?: weather.latitude,
                longitude = spot?.longitude ?: weather.longitude,
                relocalizeName = true,
                nameFromDevice = spot?.fromDevice ?: false
            ),
            langCode
        )
    }

    private fun resetForNewSession() {
        loadJob?.cancel()
        searchJob?.cancel()
        favoriteJob?.cancel()
        savedFavoriteId = null
        _state.value = HomeStateContainer()
        domainWeather = null
        domainSuggestions = emptyList()
        isInitialLaunchPerformed = false
        lastRefreshTime = 0L
        loadTarget = LoadTarget.Startup(useDeviceLocation = false)
        loadedLangCode = null
        shownSpot = null
    }

    fun onSearchQueryChanged(newQuery: String, langCode: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500.milliseconds)
            val suggestions = try {
                getLocationSuggestionsUseCase(newQuery, langCode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            domainSuggestions = suggestions
            _state.update { it.copy(citySuggestions = suggestions.map { s -> s.toPresentation() }) }
        }
    }

    fun onToggleFavorite() {
        val weather = domainWeather ?: return
        val savedId = savedFavoriteId
        viewModelScope.launch {
            if (savedId != null) {
                // Remove the favorite that is actually saved, whichever coordinates it was saved under.
                deleteFavoriteUseCase(savedId)
            } else {
                toggleFavoriteUseCase(weather.toFavoriteCity(_state.value.selectedCityName))
            }
        }
    }

    private fun onWeatherShown(weather: WeatherDomainModel) {
        favoriteJob?.cancel()
        // The point the weather service reports and the point we asked about can differ,
        // so a favorite saved under either one is this city.
        val places = buildList {
            add(weather.latitude to weather.longitude)
            shownSpot?.let { add(it.latitude to it.longitude) }
        }
        favoriteJob = observeFavoriteStatusUseCase(places)
            .onEach { savedId ->
                savedFavoriteId = savedId
                _state.update { it.copy(isFavorite = savedId != null) }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch { refreshFavoriteSnapshotUseCase(weather.toFavoriteCity()) }
    }

    /** The name for these coordinates in the app language, or null if unknown, slow or failed. */
    private suspend fun lookupCityName(
        latitude: Double?,
        longitude: Double?,
        langCode: String
    ): String? {
        if (latitude == null || longitude == null) return null
        return withTimeoutOrNull(NAME_LOOKUP_TIMEOUT_MILLIS.milliseconds) {
            reverseGeocodeLocationUseCase(latitude, longitude, langCode)?.fullName
        }
    }

    /** [name] rewritten in the app language for the place near these coordinates, or null. */
    private suspend fun localizePlaceName(
        name: String,
        latitude: Double,
        longitude: Double,
        langCode: String
    ): String? = withTimeoutOrNull(NAME_LOOKUP_TIMEOUT_MILLIS.milliseconds) {
        localizePlaceNameUseCase(name, latitude, longitude, langCode)
    }

    companion object {
        private const val TAG = "HomeViewModel"
        private const val LOAD_TIMEOUT_MILLIS = 30_000L
        private const val NAME_LOOKUP_TIMEOUT_MILLIS = 4_000L
    }
}
```

## home\presentation\mapper\CitySuggestionPresentationMapper.kt

```kotlin
package com.mhq.fynecast.home.presentation.mapper

import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.home.presentation.model.CitySuggestionPresentationModel

/** The id is the place's coordinates: two suggestions at the same spot are the same place. */
fun CitySuggestionModel.toPresentation(): CitySuggestionPresentationModel =
    CitySuggestionPresentationModel(
        id = "$latitude,$longitude",
        name = fullName
    )
```

## home\presentation\mapper\WeatherPresentationMapper.kt

```kotlin
package com.mhq.fynecast.home.presentation.mapper

import com.mhq.fynecast.core.domain.weather.DailyForecastDomainModel
import com.mhq.fynecast.core.domain.weather.HourlyForecastDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherStatsDomainModel
import com.mhq.fynecast.home.presentation.model.DailyForecastPresentationModel
import com.mhq.fynecast.home.presentation.model.HourlyForecastPresentationModel
import com.mhq.fynecast.home.presentation.model.WeatherPresentationModel
import com.mhq.fynecast.home.presentation.model.WeatherStatsPresentationModel

fun WeatherDomainModel.toPresentation(): WeatherPresentationModel = WeatherPresentationModel(
    latitude = latitude,
    longitude = longitude,
    currentTempC = currentTempC,
    currentTempF = currentTempF,
    conditionText = conditionText,
    conditionIcon = conditionIcon,
    lastUpdated = lastUpdated,
    timeZoneId = timeZoneId,
    alertsCount = alerts.size,
    stats = stats.toPresentation(),
    dailyForecasts = dailyForecasts.map { it.toPresentation() },
    hourlyForecasts = hourlyForecasts.map { it.toPresentation() }
)

internal fun WeatherStatsDomainModel.toPresentation() = WeatherStatsPresentationModel(
    humidity = humidity,
    windKph = windKph,
    windMph = windMph,
    pressureMb = pressureMb,
    pressureIn = pressureIn,
    uvIndex = uvIndex
)

internal fun DailyForecastDomainModel.toPresentation() = DailyForecastPresentationModel(
    date = date,
    iconUrl = iconUrl,
    description = description,
    maxTempC = maxTempC,
    maxTempF = maxTempF,
    minTempC = minTempC,
    minTempF = minTempF
)

internal fun HourlyForecastDomainModel.toPresentation() = HourlyForecastPresentationModel(
    time = time,
    iconUrl = iconUrl,
    tempC = tempC,
    tempF = tempF
)
```

## home\presentation\model\CitySuggestionPresentationModel.kt

```kotlin
package com.mhq.fynecast.home.presentation.model

/** One row of the search dropdown. The UI sends [id] back when the user picks it. */
data class CitySuggestionPresentationModel(
    val id: String,
    val name: String
)
```

## home\presentation\model\DailyForecastPresentationModel.kt

```kotlin
package com.mhq.fynecast.home.presentation.model

import java.time.LocalDate

data class DailyForecastPresentationModel(
    val date: LocalDate,
    val iconUrl: String,
    val description: String,
    val maxTempC: Double,
    val maxTempF: Double,
    val minTempC: Double,
    val minTempF: Double
)
```

## home\presentation\model\HourlyForecastPresentationModel.kt

```kotlin
package com.mhq.fynecast.home.presentation.model

import java.time.LocalDateTime

data class HourlyForecastPresentationModel(
    val time: LocalDateTime,
    val iconUrl: String,
    val tempC: Double,
    val tempF: Double
)
```

## home\presentation\model\WeatherPresentationModel.kt

```kotlin
package com.mhq.fynecast.home.presentation.model

import java.time.LocalDateTime

/** What the Home screen needs to show the weather. The UI never sees the domain model. */
data class WeatherPresentationModel(
    val latitude: Double,
    val longitude: Double,
    val currentTempC: Double,
    val currentTempF: Double,
    val conditionText: String,
    val conditionIcon: String,
    val lastUpdated: LocalDateTime?,
    val timeZoneId: String?,
    /** The banner only shows how many alerts there are; the Alerts screen shows the alerts themselves. */
    val alertsCount: Int,
    val stats: WeatherStatsPresentationModel,
    val dailyForecasts: List<DailyForecastPresentationModel>,
    val hourlyForecasts: List<HourlyForecastPresentationModel>
)
```

## home\presentation\model\WeatherStatsPresentationModel.kt

```kotlin
package com.mhq.fynecast.home.presentation.model

data class WeatherStatsPresentationModel(
    val humidity: Int,
    val windKph: Double,
    val windMph: Double,
    val pressureMb: Double,
    val pressureIn: Double,
    val uvIndex: Double
)
```

## home\ui\components\LikeButton.kt

```kotlin
package com.mhq.fynecast.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikeButton(
    isLiked: Boolean,
    onLikeChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (isLiked)
                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                else
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
            )
            .clickable(
                onClick = onLikeChanged,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 18.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(R.string.favorite),
            tint = if (isLiked)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}
```

## home\ui\components\rememberCityTime.kt

```kotlin
package com.mhq.fynecast.home.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mhq.fynecast.core.ui.format.DateTimeFormats
import com.mhq.fynecast.core.ui.globalcomponents.currentLocale
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.milliseconds

/** Live clock for the selected city.
 * Falls back to [fallback] when the timezone is unknown. */
@Composable
fun rememberCityTime(timeZoneId: String?, fallback: String): String {
    val locale = currentLocale()
    val zone = remember(timeZoneId) {
        timeZoneId?.takeIf { it.isNotBlank() }
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
    }
    var now by remember(zone) { mutableStateOf(zone?.let { LocalDateTime.now(it) }) }

    LaunchedEffect(zone) {
        if (zone == null) return@LaunchedEffect
        while (true) {
            now = LocalDateTime.now(zone)
            delay((60_000L - System.currentTimeMillis() % 60_000L).milliseconds)
        }
    }

    return now?.let { DateTimeFormats.lastUpdated(it, locale) } ?: fallback
}
```

## home\ui\components\WeatherDailyCarousel.kt

```kotlin
package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.format.DateTimeFormats
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.globalcomponents.currentLocale
import com.mhq.fynecast.core.ui.globalcomponents.unlessArabic
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.home.presentation.model.DailyForecastPresentationModel
import java.time.LocalDate

@Composable
fun WeatherDailyCarousel(
    dailyForecasts: List<DailyForecastPresentationModel>,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    val locale = currentLocale()
    val weekMin = dailyForecasts.minOfOrNull { if (isMetric) it.minTempC else it.minTempF } ?: 0.0
    val weekMax = dailyForecasts.maxOfOrNull { if (isMetric) it.maxTempC else it.maxTempF } ?: 0.0

    GlassyCard(
        cornerRadius = 24.dp,
        padding = 14.dp,
        containerAlpha = 0.08f,
        borderAlpha = 0.15f,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.daily_forecast),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp.unlessArabic()
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            dailyForecasts.forEach { item ->
                WeatherDailyRowItem(
                    day = DateTimeFormats.dayName(item.date, locale),
                    date = DateTimeFormats.monthDay(item.date, locale),
                    weekMin = weekMin,
                    weekMax = weekMax,
                    weatherIconUrl = item.iconUrl,
                    weatherDescription = item.description,
                    highTemperature = if (isMetric) item.maxTempC else item.maxTempF,
                    lowTemperature = if (isMetric) item.minTempC else item.minTempF
                )
            }
        }
    }
}

private fun previewDays() = List(7) { i ->
    DailyForecastPresentationModel(
        date = LocalDate.of(2026, 10, 25).plusDays(i.toLong()),
        iconUrl = "",
        description = "Sunny",
        maxTempC = 28.0 - i,
        maxTempF = 82.0 - i,
        minTempC = 17.0 + i % 3,
        minTempF = 63.0 + i % 3
    )
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE)
@Composable
private fun WeatherDailyCarouselLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        WeatherDailyCarousel(dailyForecasts = previewDays(), isMetric = true)
    }
}

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
private fun WeatherDailyCarouselDarkPreview() {
    FyneCastTheme(isDarkMode = true) {
        WeatherDailyCarousel(dailyForecasts = previewDays(), isMetric = true)
    }
}
```

## home\ui\components\WeatherDailyRowItem.kt

```kotlin
package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.core.ui.format.TemperatureFormatter
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

/** One day, about 30 dp tall: day and date, icon with description, then low, range bar, high. */
@Composable
fun WeatherDailyRowItem(
    day: String,
    date: String,
    weekMin: Double,
    weekMax: Double,
    weatherIconUrl: String,
    weatherDescription: String,
    highTemperature: Double,
    lowTemperature: Double,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.width(64.dp)) {
            Text(
                text = day,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Text(
                text = date,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        ) {
            AsyncImage(
                model = "https:$weatherIconUrl",
                contentDescription = weatherDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = weatherDescription,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                ),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.width(124.dp)
        ) {
            Text(
                text = TemperatureFormatter.format(lowTemperature),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.End,
                modifier = Modifier.width(32.dp)
            )

            val range = (weekMax - weekMin).coerceAtLeast(1.0)
            val start = ((lowTemperature - weekMin) / range).toFloat().coerceIn(0f, 1f)
            val end = ((highTemperature - weekMin) / range).toFloat().coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            ) {
                Row(Modifier.fillMaxSize()) {
                    if (start > 0f) Spacer(Modifier.weight(start))
                    Box(
                        Modifier
                            .weight((end - start).coerceAtLeast(0.08f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    if (end < 1f) Spacer(Modifier.weight(1f - end))
                }
            }

            Text(
                text = TemperatureFormatter.format(highTemperature),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.End,
                modifier = Modifier.width(32.dp)
            )
        }
    }
}

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
private fun WeatherDailyRowItemPreview() {
    FyneCastTheme(isDarkMode = true) {
        WeatherDailyRowItem(
            day = "Mon",
            date = "Oct 26",
            weekMin = 14.0,
            weekMax = 30.0,
            weatherIconUrl = "",
            weatherDescription = "Sunny",
            highTemperature = 28.0,
            lowTemperature = 17.0,
            modifier = Modifier.padding(16.dp)
        )
    }
}
```

## home\ui\components\WeatherHeroCard.kt

```kotlin
package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.globalcomponents.unlessArabic
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

/**
 * The main weather card: city and time, big temperature with the condition icon,
 * and a one-line High / Low / Humidity / Wind strip. Every value arrives already formatted.
 */
@Composable
fun WeatherHeroCard(
    icon: String,
    city: String,
    timestamp: String,
    description: String,
    temperature: String,
    high: String,
    low: String,
    humidity: String,
    wind: String,
    isFavorite: Boolean,
    alertsNumber: Int,
    onToggleFavorite: () -> Unit,
    onAlertsBannerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val glowColor = MaterialTheme.colorScheme.secondary
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    GlassyCard(
        cornerRadius = 28.dp,
        padding = 0.dp,
        containerAlpha = 0.06f,
        borderAlpha = 0.15f,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // A soft warm glow behind the icon corner (the card clips it to its shape).
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(
                                x = if (isRtl) size.width * 0.15f else size.width * 0.85f,
                                y = size.height * 0.25f
                            ),
                            radius = size.width * 0.6f
                        )
                    )
                }
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = city,
                        color = onBackground,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp.unlessArabic()
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = timestamp.uppercase(),
                        color = onBackground.copy(alpha = 0.45f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp.unlessArabic()
                        )
                    )
                }

                LikeButton(
                    isLiked = isFavorite,
                    onLikeChanged = onToggleFavorite
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = temperature,
                        color = onBackground,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.W200,
                        lineHeight = 64.sp,
                        letterSpacing = (-2).sp.unlessArabic()
                    )
                    Text(
                        text = description.uppercase(),
                        color = onBackground.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp.unlessArabic()
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AsyncImage(
                    model = "https:$icon",
                    contentDescription = description,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(84.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = onBackground.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroStat(
                    value = high,
                    label = stringResource(R.string.stat_high),
                    modifier = Modifier.weight(1f)
                )
                HeroStatDivider()
                HeroStat(
                    value = low,
                    label = stringResource(R.string.stat_low),
                    modifier = Modifier.weight(1f)
                )
                HeroStatDivider()
                HeroStat(
                    value = humidity,
                    label = stringResource(R.string.humidity),
                    modifier = Modifier.weight(1f)
                )
                HeroStatDivider()
                HeroStat(
                    value = wind,
                    label = stringResource(R.string.wind),
                    modifier = Modifier.weight(1f)
                )
            }

            if (alertsNumber > 0) {
                Spacer(modifier = Modifier.height(10.dp))

                val alertShape = RoundedCornerShape(14.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(alertShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            shape = alertShape
                        )
                        .clickable { onAlertsBannerClick() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = stringResource(R.string.warning),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.active_weather_warnings,
                            alertsNumber,
                            alertsNumber
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp.unlessArabic()
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label.uppercase(),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp.unlessArabic()
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HeroStatDivider() {
    VerticalDivider(
        modifier = Modifier.height(26.dp),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
    )
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE)
@Composable
private fun WeatherHeroCardLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        WeatherHeroCard(
            icon = "",
            city = "Cairo",
            timestamp = "3:42 PM",
            description = "Mostly sunny",
            temperature = "24°",
            high = "28°",
            low = "17°",
            humidity = "42%",
            wind = "12 km/h",
            isFavorite = true,
            alertsNumber = 2,
            onToggleFavorite = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
private fun WeatherHeroCardDarkPreview() {
    FyneCastTheme(isDarkMode = true) {
        WeatherHeroCard(
            icon = "",
            city = "Cairo",
            timestamp = "3:42 PM",
            description = "Mostly sunny",
            temperature = "24°",
            high = "28°",
            low = "17°",
            humidity = "42%",
            wind = "12 km/h",
            isFavorite = false,
            alertsNumber = 0,
            onToggleFavorite = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
```

## home\ui\components\WeatherHourlyCarousel.kt

```kotlin
package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.format.DateTimeFormats
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.globalcomponents.currentLocale
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.home.presentation.model.HourlyForecastPresentationModel
import java.time.LocalDateTime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.core.ui.globalcomponents.unlessArabic

/**
 * A glass card holding one row of compact hour chips. The first chip is the current hour ("Now").
 * The row scrolls inside the card, and the next chip peeks in to show there is more.
 */
@Composable
fun WeatherHourlyCarousel(
    hourlyForecasts: List<HourlyForecastPresentationModel>,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    val locale = currentLocale()
    val nowLabel = stringResource(R.string.hourly_now)

    GlassyCard(
        cornerRadius = 24.dp,
        padding = 14.dp,
        containerAlpha = 0.08f,
        borderAlpha = 0.15f,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.hourly_forecast),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp.unlessArabic()
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = hourlyForecasts,
                    key = { _, item -> item.time.toString() }
                ) { index, item ->
                    WeatherHourlyItem(
                        header = if (index == 0) nowLabel else DateTimeFormats.hour(item.time, locale),
                        weatherIconUrl = item.iconUrl,
                        temperature = if (isMetric) item.tempC else item.tempF,
                        isCurrentHour = index == 0
                    )
                }
            }
        }
    }
}

private fun previewHours() = List(6) { i ->
    HourlyForecastPresentationModel(
        time = LocalDateTime.of(2026, 10, 25, 15, 0).plusHours(i.toLong()),
        iconUrl = "",
        tempC = 26.0 - i,
        tempF = 79.0 - i
    )
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE)
@Composable
private fun WeatherHourlyCarouselLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        WeatherHourlyCarousel(hourlyForecasts = previewHours(), isMetric = true)
    }
}

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
private fun WeatherHourlyCarouselDarkPreview() {
    FyneCastTheme(isDarkMode = true) {
        WeatherHourlyCarousel(hourlyForecasts = previewHours(), isMetric = true)
    }
}

//package com.mhq.fynecast.home.ui.components
//
//import android.content.res.Configuration
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import com.mhq.fynecast.R
//import com.mhq.fynecast.core.ui.format.DateTimeFormats
//import com.mhq.fynecast.core.ui.globalcomponents.currentLocale
//import com.mhq.fynecast.core.ui.theme.FyneCastTheme
//import com.mhq.fynecast.home.presentation.model.HourlyForecastPresentationModel
//import java.time.LocalDateTime
//
///**
// * A single row of compact hour chips. The first chip is the current hour ("Now").
// * It scrolls edge to edge, so the next chip peeks in to show there is more.
// */
//@Composable
//fun WeatherHourlyCarousel(
//    hourlyForecasts: List<HourlyForecastPresentationModel>,
//    isMetric: Boolean,
//    modifier: Modifier = Modifier
//) {
//    val locale = currentLocale()
//    val nowLabel = stringResource(R.string.hourly_now)
//
//    LazyRow(
//        modifier = modifier.fillMaxWidth(),
//        contentPadding = PaddingValues(horizontal = 16.dp),
//        horizontalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        itemsIndexed(
//            items = hourlyForecasts,
//            key = { _, item -> item.time.toString() }
//        ) { index, item ->
//            WeatherHourlyItem(
//                header = if (index == 0) nowLabel else DateTimeFormats.hour(item.time, locale),
//                weatherIconUrl = item.iconUrl,
//                temperature = if (isMetric) item.tempC else item.tempF,
//                isCurrentHour = index == 0
//            )
//        }
//    }
//}
//
//private fun previewHours() = List(6) { i ->
//    HourlyForecastPresentationModel(
//        time = LocalDateTime.of(2026, 10, 25, 15, 0).plusHours(i.toLong()),
//        iconUrl = "",
//        tempC = 26.0 - i,
//        tempF = 79.0 - i
//    )
//}
//
//@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE)
//@Composable
//private fun WeatherHourlyCarouselLightPreview() {
//    FyneCastTheme(isDarkMode = false) {
//        WeatherHourlyCarousel(hourlyForecasts = previewHours(), isMetric = true)
//    }
//}
//
//@Preview(
//    name = "Dark",
//    uiMode = Configuration.UI_MODE_NIGHT_YES,
//    showBackground = true,
//    backgroundColor = 0xFF0F172A
//)
//@Composable
//private fun WeatherHourlyCarouselDarkPreview() {
//    FyneCastTheme(isDarkMode = true) {
//        WeatherHourlyCarousel(hourlyForecasts = previewHours(), isMetric = true)
//    }
//}
```

## home\ui\components\WeatherHourlyItem.kt

```kotlin
package com.mhq.fynecast.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.format.TemperatureFormatter
import com.mhq.fynecast.core.ui.globalcomponents.unlessArabic
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

/** One compact hour chip. The current hour gets the accent colour on its label and border. */
@Composable
fun WeatherHourlyItem(
    header: String,
    weatherIconUrl: String,
    temperature: Double,
    isCurrentHour: Boolean,
    modifier: Modifier = Modifier
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.onPrimary
    val shape = RoundedCornerShape(20.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .width(48.dp)
            .clip(shape)
            .background(
                if (isCurrentHour) accent.copy(alpha = 0.10f) else onBackground.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = if (isCurrentHour) accent.copy(alpha = 0.55f) else onBackground.copy(alpha = 0.08f),
                shape = shape
            )
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Text(
            text = header,
            color = if (isCurrentHour) accent else onBackground.copy(alpha = 0.55f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isCurrentHour) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.3.sp.unlessArabic()
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        AsyncImage(
            model = "https:$weatherIconUrl",
            contentDescription = stringResource(R.string.hourly_condition),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(vertical = 2.dp)
                .size(30.dp)
        )

        Text(
            text = TemperatureFormatter.format(temperature),
            color = onBackground,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun WeatherHourlyItemPreview() {
    FyneCastTheme(isDarkMode = true) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeatherHourlyItem("Now", "", 26.0, isCurrentHour = true)
            WeatherHourlyItem("4 PM", "", 25.0, isCurrentHour = false)
        }
    }
}
```

## home\ui\components\WeatherSearchBar.kt

```kotlin
package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.home.presentation.model.CitySuggestionPresentationModel

private val SearchBarHeight = 44.dp

/** A slim glass pill: search icon, the field, a clear button and a map button. */
@Composable
fun WeatherSearchBar(
    suggestions: List<CitySuggestionPresentationModel>,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (CitySuggestionPresentationModel) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }
    val isListVisible = showSuggestions && suggestions.isNotEmpty() && searchQuery.length > 2
    val focusManager = LocalFocusManager.current

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val onSurfaceColor = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.onPrimary
    val pillShape = RoundedCornerShape(SearchBarHeight / 2)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(10f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SearchBarHeight)
                .clip(pillShape)
                .background(onSurfaceColor.copy(alpha = if (isFocused) 0.10f else 0.06f))
                .border(
                    width = 1.dp,
                    color = if (isFocused) accent.copy(alpha = 0.5f) else onSurfaceColor.copy(alpha = 0.12f),
                    shape = pillShape
                )
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search_icon),
                tint = if (isFocused) accent else onSurfaceColor.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = stringResource(R.string.enter_a_city_name),
                        color = onSurfaceColor.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onQueryChange(it)
                        showSuggestions = it.isNotEmpty()
                    },
                    singleLine = true,
                    interactionSource = interactionSource,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = onSurfaceColor),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (isListVisible) {
                                onSuggestionClick(suggestions[0])
                                searchQuery = suggestions[0].name
                                showSuggestions = false
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(SearchBarHeight)
                        .clip(CircleShape)
                        .clickable {
                            searchQuery = ""
                            onQueryChange("")
                            showSuggestions = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clear_input),
                        tint = onSurfaceColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // The whole 44 dp square is the tap target; the visible circle is smaller.
            Box(
                modifier = Modifier
                    .size(SearchBarHeight)
                    .clip(CircleShape)
                    .clickable { onMapClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(onSurfaceColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = stringResource(R.string.pick_from_map),
                        tint = onSurfaceColor.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isListVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            GlassyCard(
                cornerRadius = 22.dp,
                padding = 4.dp,
                containerAlpha = 0.25f,
                borderAlpha = 0.35f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .zIndex(11f)
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    items(suggestions) { suggestion ->
                        Column {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = suggestion.name,
                                        color = onSurfaceColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = accent.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        searchQuery = suggestion.name
                                        showSuggestions = false
                                        onSuggestionClick(suggestion)
                                        focusManager.clearFocus()
                                    }
                                    .padding(vertical = 2.dp)
                            )
                            if (suggestion != suggestions.last()) {
                                HorizontalDivider(
                                    color = onSurfaceColor.copy(alpha = 0.06f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE)
@Composable
private fun WeatherSearchBarLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        WeatherSearchBar(
            suggestions = emptyList(),
            onQueryChange = {},
            onSuggestionClick = {},
            onMapClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
private fun WeatherSearchBarDarkPreview() {
    FyneCastTheme(isDarkMode = true) {
        WeatherSearchBar(
            suggestions = emptyList(),
            onQueryChange = {},
            onSuggestionClick = {},
            onMapClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
```

## home\ui\components\WeatherStatChip.kt

```kotlin
package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.globalcomponents.unlessArabic
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun WeatherStatChip(
    chipIcon: ImageVector,
    chipHeader: String,
    chipBody: String,
    chipSubtitle: String?,
    modifier: Modifier = Modifier
) {
    GlassyCard(
        cornerRadius = 24.dp,
        padding = 16.dp,
        containerAlpha = 0.08f,
        borderAlpha = 0.15f,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Upper Metadata Layer: Inline icon & subtle descriptive title text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), //surfaceVariantColor
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f) //surfaceVariantColor
                                )
                            )
                        )
                ) {
                    Icon(
                        imageVector = chipIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), //onSurfaceColor
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = chipHeader.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp.unlessArabic()
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), //onSurfaceColor
                    maxLines = 1
                )
            }

            // Lower Metric Block: Combined inline string for a flawless shared baseline
            Text(
                text = buildAnnotatedString {
                    // 1. The main numeric value
                    withStyle(
                        style = SpanStyle(
                            fontSize = 28.sp, //32
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground //onSurfaceColor
                        )
                    ) {
                        append(chipBody)
                    }

                    // 2. The unit suffix
                    if (!chipSubtitle.isNullOrBlank()) {
                        withStyle(
                            style = SpanStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f) //onSurfaceColor
                            )
                        ) {
                            append(" ${chipSubtitle.lowercase()}")
                        }
                    }
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview
@Composable
private fun WeatherStatChipPreview() {
    FyneCastTheme() {
        WeatherStatChip(
            chipIcon = Icons.Default.WbSunny,
            chipHeader = "Chip Header",
            chipBody = "Chip Body",
            chipSubtitle = "Chip Subtitle"
        )
    }
}
```

## home\ui\components\WeatherStatsGrid.kt

```kotlin
package com.mhq.fynecast.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.home.presentation.model.WeatherStatsPresentationModel
import java.text.NumberFormat

@Composable
fun WeatherStatsGrid(
    stats: WeatherStatsPresentationModel,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val numberFormat = remember(locale) {
        NumberFormat.getInstance(locale).apply { maximumFractionDigits = 1 }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.WaterDrop,
                    chipHeader = stringResource(R.string.humidity),
                    chipBody = numberFormat.format(stats.humidity.toDouble()),
                    chipSubtitle = stringResource(R.string.percent)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.Air,
                    chipHeader = stringResource(R.string.wind_speed),
                    chipBody = numberFormat.format(if (isMetric) stats.windKph else stats.windMph),
                    chipSubtitle = if (isMetric) stringResource(R.string.kph) else stringResource(R.string.mph)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.Compress,
                    chipHeader = stringResource(R.string.pressure),
                    chipBody = numberFormat.format(if (isMetric) stats.pressureMb else stats.pressureIn),
                    chipSubtitle = if (isMetric) stringResource(R.string.millibars) else stringResource(
                        R.string.inches
                    )
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.WbSunny,
                    chipHeader = stringResource(R.string.uv_index),
                    chipBody = numberFormat.format(stats.uvIndex),
                    chipSubtitle = stringResource(R.string.unit)
                )
            }
        }
    }
}
```

## home\ui\screens\HomeContainer.kt

```kotlin
package com.mhq.fynecast.home.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.home.presentation.HomeRetryAction
import com.mhq.fynecast.home.presentation.HomeUiState
import com.mhq.fynecast.home.presentation.HomeViewModel

@Composable
fun HomeContainer(
    homeViewModel: HomeViewModel = viewModel(factory = AppViewModelFactory),
    onNavigateToMap: () -> Unit,
    onAlertsBannerClick: () -> Unit,
    onRequestPermission: (String) -> Unit,
    isMetric: Boolean,
    langCode: String,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {

    val stateContainer by homeViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (!homeViewModel.isInitialLaunchPerformed) {
            onRequestPermission(langCode)
        }
    }

    /** Read the nested uiState directly from the container wrapper */
    when (val uiState = stateContainer.uiState) {
        is HomeUiState.Loading -> {
            HomeLoading(contentPadding = contentPadding, modifier = modifier)
        }

        is HomeUiState.Success -> {
            HomeContent(
                weather = uiState.weather,
                citySuggestions = stateContainer.citySuggestions,
                selectedCityName = stateContainer.selectedCityName,
                isMetric = isMetric,
                isFavorite = stateContainer.isFavorite,
                isRefreshing = stateContainer.isRefreshing,
                onQueryChange = { homeViewModel.onSearchQueryChanged(it, langCode) },
                onSuggestionClick = { suggestion ->
                    homeViewModel.onSuggestionSelected(suggestion.id, langCode)
                },
                onMapIconClick = onNavigateToMap,
                onAlertsBannerClick = onAlertsBannerClick,
                onToggleFavorite = { homeViewModel.onToggleFavorite() },
                contentPadding = contentPadding,
                modifier = modifier
            )
        }

        is HomeUiState.Error -> {
            HomeError(
                retryAction = {
                    when (uiState.retry) {
                         // One load: the permission result starts it.
                         // If the permission is already decided,
                         // the callback fires immediately and the load starts anyway.
                        HomeRetryAction.RequestLocation -> onRequestPermission(langCode)
                        HomeRetryAction.ReloadPlace -> homeViewModel.onRetryClicked(langCode)
                    }
                },
                contentPadding = contentPadding,
                modifier = modifier
            )
        }
    }
}
```

## home\ui\screens\HomeContent.kt

```kotlin
package com.mhq.fynecast.home.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.format.DateTimeFormats
import com.mhq.fynecast.core.ui.format.TemperatureFormatter
import com.mhq.fynecast.core.ui.globalcomponents.currentLocale
import com.mhq.fynecast.home.presentation.model.CitySuggestionPresentationModel
import com.mhq.fynecast.home.presentation.model.WeatherPresentationModel
import com.mhq.fynecast.home.ui.components.WeatherDailyCarousel
import com.mhq.fynecast.home.ui.components.WeatherHeroCard
import com.mhq.fynecast.home.ui.components.WeatherHourlyCarousel
import com.mhq.fynecast.home.ui.components.WeatherSearchBar
import com.mhq.fynecast.home.ui.components.rememberCityTime
import kotlin.math.roundToInt

@Composable
fun HomeContent(
    weather: WeatherPresentationModel,
    citySuggestions: List<CitySuggestionPresentationModel>,
    selectedCityName: String,
    isMetric: Boolean,
    isFavorite: Boolean,
    isRefreshing: Boolean,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (CitySuggestionPresentationModel) -> Unit,
    onMapIconClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAlertsBannerClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // Today's range comes from the first forecast day.
    val today = weather.dailyForecasts.firstOrNull()
    val high =
        today?.let { TemperatureFormatter.format(if (isMetric) it.maxTempC else it.maxTempF) }
            ?: "–"
    val low = today?.let { TemperatureFormatter.format(if (isMetric) it.minTempC else it.minTempF) }
        ?: "–"
    val humidity = "${weather.stats.humidity}%"
    val windSpeed = (if (isMetric) weather.stats.windKph else weather.stats.windMph).roundToInt()
    val wind = "$windSpeed ${stringResource(if (isMetric) R.string.kph else R.string.mph)}"

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Sized to fit a normal phone without scrolling; the scroll is only a fallback for small screens.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = contentPadding.calculateBottomPadding() + 12.dp)
                .statusBarsPadding()
        ) {
            WeatherSearchBar(
                suggestions = citySuggestions,
                onQueryChange = onQueryChange,
                onSuggestionClick = onSuggestionClick,
                onMapClick = onMapIconClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
            WeatherHeroCard(
                icon = weather.conditionIcon,
                city = selectedCityName.substringBefore(","),
                timestamp = rememberCityTime(
                    timeZoneId = weather.timeZoneId,
                    fallback = weather.lastUpdated?.let {
                        DateTimeFormats.lastUpdated(it, currentLocale())
                    }.orEmpty()
                ),
                description = weather.conditionText,
                temperature = TemperatureFormatter.format(
                    if (isMetric)
                        weather.currentTempC
                    else
                        weather.currentTempF
                ),
                high = high,
                low = low,
                humidity = humidity,
                wind = wind,
                isFavorite = isFavorite,
                alertsNumber = weather.alertsCount,
                onToggleFavorite = onToggleFavorite,
                onAlertsBannerClick = onAlertsBannerClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
            WeatherHourlyCarousel(
                hourlyForecasts = weather.hourlyForecasts,
                isMetric = isMetric,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
            WeatherDailyCarousel(
                dailyForecasts = weather.dailyForecasts,
                isMetric = isMetric,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
        }
        AnimatedVisibility(
            visible = isRefreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }
    }
}
```

## home\ui\screens\HomeError.kt

```kotlin
package com.mhq.fynecast.home.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.core.ui.theme.AbyssalVoid
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen

@Composable
fun HomeError(
    retryAction: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Text(
            text = stringResource(R.string.please_check_your_network_then_retry),
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(16.dp)
        )
        Button(
            onClick = retryAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview(showBackground = true)
@Composable
fun HomeErrorPreview() {
    FyneCastTheme() {
        HomeError(
            {},
            contentPadding = PaddingValues(0.dp)
        )
    }
}
```

## home\ui\screens\HomeLoading.kt

```kotlin
package com.mhq.fynecast.home.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen

@Composable
fun HomeLoading(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .rotate(rotation)
                .padding(20.dp)
        )
        Text(
            text = stringResource(R.string.please_wait),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview(showBackground = true)
@Composable
fun HomeLoadingPreview() {
    FyneCastTheme() {
        HomeLoading(contentPadding = PaddingValues(0.dp))
    }
}
```

## map\domain\usecases\ReverseGeocodeLocationUseCase.kt

```kotlin
package com.mhq.fynecast.map.domain.usecases

import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.core.domain.location.LocationRepository

class ReverseGeocodeLocationUseCase(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(
        latitude: Double,
        longitude: Double,
        lang: String
    ): CitySuggestionModel? {
        return locationRepository.getCityNameOffMap(latitude, longitude, lang)
    }
}
```

## map\presentation\mapper\MapPlacePresentationMapper.kt

```kotlin
package com.mhq.fynecast.map.presentation.mapper

import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.map.presentation.model.MapPlacePresentationModel

fun CitySuggestionModel.toPresentation(): MapPlacePresentationModel =
    MapPlacePresentationModel(name = fullName)
```

## map\presentation\MapPickerUiState.kt

```kotlin
package com.mhq.fynecast.map.presentation

import com.mhq.fynecast.core.presentation.text.UiText
import com.mhq.fynecast.map.presentation.model.MapPlacePresentationModel

/**
 * What the map picker screen shows.
 *
 * The marker coordinates are set immediately on tap so the marker survives rotation
 * and appears before the place name has been resolved.
 */
data class MapPickerUiState(
    val selectedPlace: MapPlacePresentationModel? = null,
    val markerLatitude: Double? = null,
    val markerLongitude: Double? = null,
    val isSearching: Boolean = false,
    val errorMessage: UiText? = null
)
```

## map\presentation\MapPickerViewModel.kt

```kotlin
package com.mhq.fynecast.map.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.R
import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.core.domain.location.SelectCityUseCase
import com.mhq.fynecast.core.domain.location.SelectedCity
import com.mhq.fynecast.core.presentation.text.UiText
import com.mhq.fynecast.map.domain.usecases.ReverseGeocodeLocationUseCase
import com.mhq.fynecast.map.presentation.mapper.toPresentation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapPickerViewModel(
    private val selectCityUseCase: SelectCityUseCase,
    private val reverseGeocodeLocationUseCase: ReverseGeocodeLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapPickerUiState())
    val uiState: StateFlow<MapPickerUiState> = _uiState.asStateFlow()

    private var geocodeJob: Job? = null

    /** The domain city for the last tap, kept privately: the screen only sees the presentation model. */
    private var resolvedCity: CitySuggestionModel? = null

    fun onMapClicked(latitude: Double, longitude: Double, langCode: String) {
        geocodeJob?.cancel()
        resolvedCity = null
        geocodeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearching = true,
                    selectedPlace = null,
                    errorMessage = null,
                    markerLatitude = latitude,
                    markerLongitude = longitude
                )
            }
            try {
                val resolved = reverseGeocodeLocationUseCase(latitude, longitude, langCode)
                if (resolved != null) {
                    resolvedCity = resolved
                    _uiState.update { it.copy(selectedPlace = resolved.toPresentation()) }
                } else {
                    _uiState.update {
                        it.copy(errorMessage = UiText.res(R.string.error_map_no_location))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = UiText.res(R.string.error_map_resolve_failed))
                }
            }
            _uiState.update { it.copy(isSearching = false) }
        }
    }

    /** The user pressed "Confirm" on the card: publish the city that was resolved for the last tap. */
    fun onLocationConfirmed() {
        val city = resolvedCity ?: return
        viewModelScope.launch {
            selectCityUseCase(SelectedCity(city.fullName, city.latitude, city.longitude))
        }
    }
}
```

## map\presentation\model\MapPlacePresentationModel.kt

```kotlin
package com.mhq.fynecast.map.presentation.model

/** The place the user tapped, as the confirm card needs it. The UI never sees the domain model. */
data class MapPlacePresentationModel(
    val name: String
)
```

## map\ui\components\ErrorCard.kt

```kotlin
package com.mhq.fynecast.map.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.core.ui.theme.AbyssalVoid

@Composable
fun ErrorCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AbyssalVoid.copy(alpha = 0.9f)
        )
    ) {
        Text(
            text = message,
            color = Color.White,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun ErrorCardPreview() {
    FyneCastTheme(){
        ErrorCard(
            message = "Hi"
        )
    }
}
```

## map\ui\components\LoadingCard.kt

```kotlin
package com.mhq.fynecast.map.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.core.ui.theme.AbyssalVoid
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen

@Composable
fun LoadingCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AbyssalVoid.copy(alpha = 0.9f)
        )
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = ElectricLimeGreen,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview
@Composable
private fun LoadingCardPreview() {
    FyneCastTheme(){
        LoadingCard()
    }
}
```

## map\ui\components\LocationConfirmCard.kt

```kotlin
package com.mhq.fynecast.map.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.AbyssalVoid
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.map.presentation.model.MapPlacePresentationModel

@Composable
fun LocationConfirmCard(
    place: MapPlacePresentationModel,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AbyssalVoid.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = place.name,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(
                modifier = Modifier.height(12.dp)
            )
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricLimeGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.confirm_location),
                    color = AbyssalVoid
                )
            }
        }
    }
}

@Preview
@Composable
private fun LocationConfirmCardPreview() {
    FyneCastTheme {
        LocationConfirmCard(
            place = MapPlacePresentationModel(name = "City Name"),
            onConfirm = {}
        )
    }
}
```

## map\ui\screens\MapPickerScreen.kt

```kotlin
package com.mhq.fynecast.map.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.AbyssalVoid
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen
import com.mhq.fynecast.map.presentation.MapPickerViewModel
import com.mhq.fynecast.map.ui.components.ErrorCard
import com.mhq.fynecast.map.ui.components.LoadingCard
import com.mhq.fynecast.map.ui.components.LocationConfirmCard
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import androidx.core.graphics.createBitmap

private const val MARKER_ICON_ID = "map_picker_marker"
private const val MARKER_SOURCE_ID = "map_picker_marker_source"
private const val MARKER_LAYER_ID = "map_picker_marker_layer"
private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val MAX_ZOOM = 14.0     // street level; the old 10.0 stopped at a whole-city view
private const val CITY_ZOOM = 8.0     // opening zoom when we know where to look
private const val WORLD_ZOOM = 1.5    // opening zoom when we don't know where to look

@Composable
fun MapPickerScreen(
    mapPickerViewModel: MapPickerViewModel,
    langCode: String,
    initialLatitude: Double?,
    initialLongitude: Double?,
    onLocationConfirmed: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by mapPickerViewModel.uiState.collectAsStateWithLifecycle()
    val latestLangCode by rememberUpdatedState(langCode)

    remember { MapLibre.getInstance(context) }
    val mapView = remember { MapView(context) }
    var markerSource by remember { mutableStateOf<GeoJsonSource?>(null) }

    val markerFillColor = ElectricLimeGreen.toArgb()
    val markerStrokeColor = AbyssalVoid.toArgb()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // The marker follows the ViewModel state, so it survives rotation and shows the moment
    // a tap happens, even while the place name is still being resolved.
    LaunchedEffect(markerSource, uiState.markerLatitude, uiState.markerLongitude) {
        val source = markerSource ?: return@LaunchedEffect
        val latitude = uiState.markerLatitude
        val longitude = uiState.markerLongitude
        if (latitude != null && longitude != null) {
            source.setGeoJson(Feature.fromGeometry(Point.fromLngLat(longitude, latitude)))
        } else {
            source.setGeoJson(FeatureCollection.fromFeatures(emptyList<Feature>()))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        map.setMaxZoomPreference(MAX_ZOOM)

                        // After a rotation the old marker is the best place to look; otherwise
                        // open on the current city, or on a world view if we have neither.
                        val savedState = mapPickerViewModel.uiState.value
                        val startLatitude = savedState.markerLatitude ?: initialLatitude
                        val startLongitude = savedState.markerLongitude ?: initialLongitude
                        map.cameraPosition = if (startLatitude != null && startLongitude != null) {
                            CameraPosition.Builder()
                                .target(LatLng(startLatitude, startLongitude))
                                .zoom(CITY_ZOOM)
                                .build()
                        } else {
                            CameraPosition.Builder()
                                .target(LatLng(20.0, 0.0))
                                .zoom(WORLD_ZOOM)
                                .build()
                        }

                        map.addOnMapClickListener { point ->
                            mapPickerViewModel.onMapClicked(
                                point.latitude,
                                point.longitude,
                                latestLangCode
                            )
                            true
                        }

                        map.setStyle(MAP_STYLE_URL) { style ->
                            localizeLabels(style, latestLangCode)
                            style.addImage(
                                MARKER_ICON_ID,
                                createMarkerBitmap(markerFillColor, markerStrokeColor)
                            )
                            // The marker is a plain GeoJSON point drawn by a symbol layer, so
                            // no annotation plugin is needed.
                            val source = GeoJsonSource(MARKER_SOURCE_ID)
                            style.addSource(source)
                            style.addLayer(
                                SymbolLayer(MARKER_LAYER_ID, MARKER_SOURCE_ID).withProperties(
                                    PropertyFactory.iconImage(MARKER_ICON_ID),
                                    PropertyFactory.iconAllowOverlap(true),
                                    PropertyFactory.iconIgnorePlacement(true)
                                )
                            )
                            markerSource = source
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .background(
                    color = AbyssalVoid.copy(alpha = 0.7f),
                    shape = CircleShape
                )
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                stringResource(R.string.back),
                tint = Color.White
            )
        }

        if (uiState.isSearching) {
            LoadingCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        } else {
            uiState.errorMessage?.let { errorMessage ->
                ErrorCard(
                    message = errorMessage.asString(context),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            } ?: uiState.selectedPlace?.let { place ->
                LocationConfirmCard(
                    place = place,
                    onConfirm = onLocationConfirmed,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            }
        }
    }
}

/**
 * We draw a simple dot bitmap using the app's own theme colors and register it with the
 * style. Swap this out for a real drawable asset (via
 * ContextCompat.getDrawable(context, R.drawable.xxx).toBitmap()) whenever one exists.
 */
private fun createMarkerBitmap(fillColor: Int, strokeColor: Int): Bitmap {
    val size = 64
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val radius = size / 2f - 4f

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    canvas.drawCircle(size / 2f, size / 2f, radius, fillPaint)
    canvas.drawCircle(size / 2f, size / 2f, radius, strokePaint)
    return bitmap
}

/**
 * Shows map labels in the app language (e.g. "ar" or "en"). Every symbol layer whose label
 * uses a name field is switched to "name:<lang>", falling back to the local name when the
 * place has no translation. Call this before adding our own marker layer.
 */
private fun localizeLabels(style: Style, langCode: String) {
    val localizedName = Expression.coalesce(
        Expression.get("name:$langCode"),
        Expression.get("name")
    )
    style.layers.filterIsInstance<SymbolLayer>().forEach { layer ->
        if (layer.textField.toString().contains("name")) {
            layer.setProperties(PropertyFactory.textField(localizedName))
        }
    }
}
```

## settings\data\infra\PreferencesAppLanguageProvider.kt

```kotlin
package com.mhq.fynecast.settings.data.infra

import com.mhq.fynecast.core.domain.infra.AppLanguageProvider
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PreferencesAppLanguageProvider(
    private val preferencesRepository: UserPreferencesRepository
) : AppLanguageProvider {

    override fun observeLanguageCode(): Flow<String> =
        preferencesRepository.userPreferencesFlow
            .map { it.currentLanguage.code }
            .distinctUntilChanged()
}
```

## settings\data\repoimpl\UserPreferencesRepositoryImpl.kt

```kotlin
package com.mhq.fynecast.settings.data.repoimpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mhq.fynecast.settings.domain.models.AppLanguage
import com.mhq.fynecast.settings.domain.models.UserPreferences
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

class UserPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val IS_METRIC = booleanPreferencesKey("is_metric")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val CURRENT_LANGUAGE = stringPreferencesKey("current_language")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LAST_CITY_NAME = stringPreferencesKey("last_searched_city_name")
        val LAST_CITY_LAT = doublePreferencesKey("last_searched_city_lat")
        val LAST_CITY_LON = doublePreferencesKey("last_searched_city_lon")
        val NOTIFICATION_PROMPT_SHOWN = booleanPreferencesKey("notification_prompt_shown")
    }

    override val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .map { preferences -> mapPreferences(preferences) }

    override suspend fun getPreferences(): UserPreferences {
        return mapPreferences(dataStore.data.first())
    }

    override suspend fun updateMetricSystem(isMetric: Boolean) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.IS_METRIC] = isMetric }
    }

    override suspend fun updateNotifications(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled }
    }

    override suspend fun updateLanguage(language: AppLanguage) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.CURRENT_LANGUAGE] = language.storedName }
    }

    override suspend fun updateDarkMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.IS_DARK_MODE] = enabled }
    }

    override suspend fun updateLastSearchedCity(name: String, lat: Double, lon: Double) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_CITY_NAME] = name
            prefs[PreferencesKeys.LAST_CITY_LAT] = lat
            prefs[PreferencesKeys.LAST_CITY_LON] = lon
        }
    }

    override suspend fun clearLastSearchedCity() {
        dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.LAST_CITY_NAME)
            prefs.remove(PreferencesKeys.LAST_CITY_LAT)
            prefs.remove(PreferencesKeys.LAST_CITY_LON)
        }
    }

    /** True exactly once, ever. The read and write happen inside one DataStore transaction. */
    override suspend fun consumeNotificationPrompt(): Boolean {
        var firstTime = false
        dataStore.edit { prefs ->
            if (prefs[PreferencesKeys.NOTIFICATION_PROMPT_SHOWN] != true) {
                prefs[PreferencesKeys.NOTIFICATION_PROMPT_SHOWN] = true
                firstTime = true
            }
        }
        return firstTime
    }

    private fun mapPreferences(preferences: Preferences): UserPreferences {
        return UserPreferences(
            isMetric = preferences[PreferencesKeys.IS_METRIC] ?: true,
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
            currentLanguage = preferences[PreferencesKeys.CURRENT_LANGUAGE]
                ?.let { AppLanguage.fromStoredName(it) }
                ?: phoneLanguage(),
            isDarkMode = preferences[PreferencesKeys.IS_DARK_MODE],
            lastSearchedCityName = preferences[PreferencesKeys.LAST_CITY_NAME],
            lastSearchedCityLat = preferences[PreferencesKeys.LAST_CITY_LAT],
            lastSearchedCityLon = preferences[PreferencesKeys.LAST_CITY_LON]
        )
    }

    /** First run: follow the phone's language. Anything that isn't Arabic gets English. */
    private fun phoneLanguage(): AppLanguage =
        if (Locale.getDefault().language == "ar") AppLanguage.Arabic else AppLanguage.English
}
```

## settings\domain\models\AppLanguage.kt

```kotlin
package com.mhq.fynecast.settings.domain.models

enum class AppLanguage(val storedName: String, val code: String) {
    English("English", "en"),
    Arabic("Arabic", "ar");

    companion object {
        fun fromStoredName(name: String?): AppLanguage =
            entries.firstOrNull { it.storedName == name } ?: English
    }
}
```

## settings\domain\models\UserPreferences.kt

```kotlin
package com.mhq.fynecast.settings.domain.models

data class UserPreferences(
    val isMetric: Boolean,
    val notificationsEnabled: Boolean,
    val currentLanguage: AppLanguage,
    val isDarkMode: Boolean?,
    val lastSearchedCityName: String? = null,
    val lastSearchedCityLat: Double? = null,
    val lastSearchedCityLon: Double? = null,
    val notificationPromptShown: Boolean = false
)
```

## settings\domain\repository\UserPreferencesRepository.kt

```kotlin
package com.mhq.fynecast.settings.domain.repository

import com.mhq.fynecast.settings.domain.models.AppLanguage
import com.mhq.fynecast.settings.domain.models.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userPreferencesFlow: Flow<UserPreferences>

    suspend fun getPreferences(): UserPreferences
    suspend fun updateMetricSystem(isMetric: Boolean)
    suspend fun updateNotifications(enabled: Boolean)
    suspend fun updateLanguage(language: AppLanguage)
    suspend fun updateDarkMode(enabled: Boolean)
    suspend fun updateLastSearchedCity(name: String, lat: Double, lon: Double)
    suspend fun clearLastSearchedCity()
    suspend fun consumeNotificationPrompt(): Boolean
}
```

## settings\domain\usecases\profile\ClearLastSearchedCityUseCase.kt

```kotlin
package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class ClearLastSearchedCityUseCase(private val preferencesRepository: UserPreferencesRepository) {
    suspend operator fun invoke() = preferencesRepository.clearLastSearchedCity()
}
```

## settings\domain\usecases\profile\ObserveUserPreferencesUseCase.kt

```kotlin
package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.models.UserPreferences
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserPreferencesUseCase(private val preferencesRepository: UserPreferencesRepository) {
    operator fun invoke(): Flow<UserPreferences> = preferencesRepository.userPreferencesFlow
}
```

## settings\domain\usecases\profile\SaveLastSearchedCityUseCase.kt

```kotlin
package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class SaveLastSearchedCityUseCase(private val preferencesRepository: UserPreferencesRepository) {
    suspend operator fun invoke(name: String, lat: Double, lon: Double) =
        preferencesRepository.updateLastSearchedCity(name, lat, lon)
}
```

## settings\domain\usecases\profile\SetDarkModeUseCase.kt

```kotlin
package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class SetDarkModeUseCase(private val preferencesRepository: UserPreferencesRepository) {
    suspend operator fun invoke(enabled: Boolean) =
        preferencesRepository.updateDarkMode(enabled)
}
```

## settings\domain\usecases\profile\SetLanguageUseCase.kt

```kotlin
package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.models.AppLanguage
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class SetLanguageUseCase(private val preferencesRepository: UserPreferencesRepository) {
    suspend operator fun invoke(language: AppLanguage) =
        preferencesRepository.updateLanguage(language)
}
```

## settings\domain\usecases\profile\SetNotificationsEnabledUseCase.kt

```kotlin
package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class SetNotificationsEnabledUseCase(private val preferencesRepository: UserPreferencesRepository) {
    suspend operator fun invoke(enabled: Boolean) =
        preferencesRepository.updateNotifications(enabled)
}
```

## settings\domain\usecases\profile\SetUnitSystemUseCase.kt

```kotlin
package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class SetUnitSystemUseCase(private val preferencesRepository: UserPreferencesRepository) {
    suspend operator fun invoke(isMetric: Boolean) =
        preferencesRepository.updateMetricSystem(isMetric)
}
```

## settings\presentation\mapper\LanguageOptionMapper.kt

```kotlin
package com.mhq.fynecast.settings.presentation.mapper

import com.mhq.fynecast.settings.domain.models.AppLanguage
import com.mhq.fynecast.settings.presentation.model.LanguageOption

// Both `when` blocks are exhaustive on purpose: adding a language to one enum
// breaks the build here until the other enum and these mappers are updated.
fun AppLanguage.toPresentation(): LanguageOption = when (this) {
    AppLanguage.English -> LanguageOption.English
    AppLanguage.Arabic -> LanguageOption.Arabic
}

fun LanguageOption.toDomain(): AppLanguage = when (this) {
    LanguageOption.English -> AppLanguage.English
    LanguageOption.Arabic -> AppLanguage.Arabic
}
```

## settings\presentation\model\LanguageOption.kt

```kotlin
package com.mhq.fynecast.settings.presentation.model

/** The languages the Settings screen offers. The UI never sees the domain AppLanguage. */
enum class LanguageOption(val code: String) {
    English("en"),
    Arabic("ar")
}
```

## settings\presentation\SettingsViewModel.kt

```kotlin
package com.mhq.fynecast.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.alerts.notifications.domain.usecases.ConsumeNotificationPromptUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.SyncNotificationPermissionUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ObserveUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.session.LogoutUserUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetDarkModeUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetLanguageUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetUnitSystemUseCase
import com.mhq.fynecast.settings.presentation.mapper.toDomain
import com.mhq.fynecast.settings.presentation.mapper.toPresentation
import com.mhq.fynecast.settings.presentation.model.LanguageOption
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val setUnitSystem: SetUnitSystemUseCase,
    private val setLanguage: SetLanguageUseCase,
    private val setDarkMode: SetDarkModeUseCase,
    private val toggleNotifications: ToggleNotificationsUseCase,
    private val handleNotificationPermissionResult: HandleNotificationPermissionResultUseCase,
    private val syncNotificationPermission: SyncNotificationPermissionUseCase,
    private val consumeNotificationPrompt: ConsumeNotificationPromptUseCase,
    private val logoutUserUseCase: LogoutUserUseCase
) : ViewModel() {

    private val profileFlow = observeUserProfileUseCase().filter { it.id.isNotEmpty() }
    private val preferencesFlow = observeUserPreferencesUseCase()
    private val _requestNotificationPermissionEvent = Channel<Unit>(Channel.BUFFERED)
    val requestNotificationPermissionEvent = _requestNotificationPermissionEvent.receiveAsFlow()
    private var isAwaitingTogglePermission = false
    private val _notificationsBlockedChannel = Channel<Unit>(Channel.BUFFERED)
    val notificationsBlocked = _notificationsBlockedChannel.receiveAsFlow()
    private val _logoutCompleteChannel = Channel<Unit>(Channel.BUFFERED)
    val logoutComplete = _logoutCompleteChannel.receiveAsFlow()
    private val _logoutFailedChannel = Channel<Unit>(Channel.BUFFERED)
    val logoutFailed = _logoutFailedChannel.receiveAsFlow()
    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()
    private val _isMetric = MutableStateFlow(true)
    val isMetric: StateFlow<Boolean> = _isMetric.asStateFlow()
    private val _currentLanguage = MutableStateFlow(LanguageOption.English)
    val currentLanguage: StateFlow<LanguageOption> = _currentLanguage.asStateFlow()
    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()
    private val _arePreferencesLoaded = MutableStateFlow(false)
    /** True once the first preferences read has finished; the splash screen waits for it. */
    val arePreferencesLoaded: StateFlow<Boolean> = _arePreferencesLoaded.asStateFlow()

    val profileName: StateFlow<String> =
        profileFlow.map { it.name }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                "Username"
            )

    val profileEmail: StateFlow<String> =
        profileFlow.map { it.email }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                "username@gmail.com"
            )

    val profileImageUri: StateFlow<String?> =
        profileFlow.map { it.profileImageUri }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                null
            )

    init {
        viewModelScope.launch {
            viewModelScope.launch {
                // A new signed-in profile means the previous logout is over.
                profileFlow.collect { _isLoggingOut.value = false }
            }

            preferencesFlow.collect { prefs ->
                _isMetric.value = prefs.isMetric
                _currentLanguage.value = prefs.currentLanguage.toPresentation()
                _isDarkMode.value = prefs.isDarkMode
                _notificationsEnabled.value = prefs.notificationsEnabled
                // Last, so the splash only lifts once every value above is set.
                _arePreferencesLoaded.value = true
            }
        }
    }

    fun onUnitSystemChanged(isMetric: Boolean) {
        viewModelScope.launch {
            setUnitSystem(isMetric)
        }
    }

    /** Called once the location permission flow finishes, whether it was granted or denied. */
    fun requestNotificationPermissionOnFirstLaunch() {
        viewModelScope.launch {
            if (consumeNotificationPrompt()) _requestNotificationPermissionEvent.trySend(Unit)
        }
    }

    fun toggleNotifications(wantsEnabled: Boolean) {
        viewModelScope.launch {
            when (toggleNotifications.invoke(wantsEnabled)) {
                ToggleNotificationsUseCase.Result.PermissionRequired -> {
                    isAwaitingTogglePermission = true
                    _requestNotificationPermissionEvent.trySend(Unit)
                }
                else -> Unit
            }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean, isBlockedForGood: Boolean) {
        val fromToggle = isAwaitingTogglePermission
        isAwaitingTogglePermission = false
        viewModelScope.launch {
            handleNotificationPermissionResult(granted)
            if (!granted && isBlockedForGood && fromToggle) {
                _notificationsBlockedChannel.send(Unit)
            }
        }
    }

    fun onAppResumed() {
        viewModelScope.launch {
            syncNotificationPermission()
        }
    }

    fun onLanguageChanged(language: LanguageOption) {
        viewModelScope.launch { setLanguage(language.toDomain()) }
    }

    fun onThemeChanged(enabled: Boolean) {
        viewModelScope.launch { setDarkMode(enabled) }
    }

    fun onLogoutAction() {
        if (_isLoggingOut.value) return
        _isLoggingOut.value = true
        viewModelScope.launch {
            logoutUserUseCase().fold(
                onSuccess = { _logoutCompleteChannel.send(Unit) },
                onFailure = {
                    _isLoggingOut.value = false
                    _logoutFailedChannel.send(Unit)
                }
            )
        }
    }
}
```

## settings\ui\components\customSegmentedColors.kt

```kotlin
package com.mhq.fynecast.settings.ui.components

import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun customSegmentedColors(): SegmentedButtonColors {
    return SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary,
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
```

## settings\ui\components\SettingsLogoutButton.kt

```kotlin
package com.mhq.fynecast.settings.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun SettingsLogoutButton(
    onLogoutClick: () -> Unit,
    isLoggingOut: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onLogoutClick,
        enabled = !isLoggingOut,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
            contentColor = MaterialTheme.colorScheme.error
        ),
        modifier = modifier
            .padding(horizontal = 16.dp)
            .height(52.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoggingOut) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.logout),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Preview
@Composable
private fun SettingsLogoutButtonPreview() {
    FyneCastTheme() {
        SettingsLogoutButton(
            onLogoutClick = {}
        )
    }
}
```

## settings\ui\components\SettingsPanel.kt

```kotlin
package com.mhq.fynecast.settings.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.settings.presentation.model.LanguageOption

@Composable
fun SettingsPanel(
    notificationsEnabled: Boolean,
    isDarkMode: Boolean,
    isMetric: Boolean,
    currentLanguage: LanguageOption,
    onNotificationsToggle: (Boolean) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onMetricToggle: (Boolean) -> Unit,
    onLanguageChange: (LanguageOption) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassyCard(
        padding = 0.dp,
        modifier = modifier.padding(
                start = 16.dp,
                top = 32.dp,
                bottom = 32.dp,
                end = 16.dp
        ).fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsToggleRow(
                label = stringResource(R.string.units),
                isFirstOptionSelected = isMetric,
                firstOptionLabel = stringResource(R.string.metric),
                secondOptionLabel = stringResource(R.string.imperial),
                onFirstOptionSelected = { onMetricToggle(true) },
                onSecondOptionSelected = { onMetricToggle(false) }
            )
            SettingsPanelDivider()
            SettingsToggleRow(
                label = stringResource(R.string.notifications),
                isFirstOptionSelected = notificationsEnabled,
                firstOptionLabel = stringResource(R.string.on),
                secondOptionLabel = stringResource(R.string.off),
                onFirstOptionSelected = { onNotificationsToggle(true) },
                onSecondOptionSelected = { onNotificationsToggle(false) }
            )
            SettingsPanelDivider()
            SettingsToggleRow(
                label = stringResource(R.string.language),
                isFirstOptionSelected = currentLanguage == LanguageOption.English,
                firstOptionLabel = stringResource(R.string.english),
                secondOptionLabel = "العربية",
                onFirstOptionSelected = { onLanguageChange(LanguageOption.English) },
                onSecondOptionSelected = { onLanguageChange(LanguageOption.Arabic) }
            )
            SettingsPanelDivider()
            SettingsToggleRow(
                label = stringResource(R.string.dark_mode),
                isFirstOptionSelected = !isDarkMode,
                firstOptionLabel = stringResource(R.string.light),
                secondOptionLabel = stringResource(R.string.dark),
                onFirstOptionSelected = { onDarkModeToggle(false) },
                onSecondOptionSelected = { onDarkModeToggle(true) }
            )
        }
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview
@Composable
private fun SettingsPanelPreview() {
    FyneCastTheme() {
        SettingsPanel(
            notificationsEnabled = true,
            isDarkMode = true,
            isMetric = true,
            currentLanguage = LanguageOption.English,
            onNotificationsToggle = {},
            onDarkModeToggle = {},
            onMetricToggle = {},
            onLanguageChange = {}
        )
    }
}
```

## settings\ui\components\SettingsPanelDivider.kt

```kotlin
package com.mhq.fynecast.settings.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun SettingsPanelDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.onBackground.copy(
            alpha = 0.08f
        ),
        modifier = Modifier.padding(
            horizontal = 16.dp
        )
    )
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview
@Composable
private fun SettingsPanelDividerPreview() {
    FyneCastTheme() {
        SettingsPanelDivider()
    }
}
```

## settings\ui\components\SettingsProfileHeader.kt

```kotlin
package com.mhq.fynecast.settings.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun SettingsProfileHeader(
    profileName: String,
    profileEmail: String,
    profileImageUri: String?,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val imageModel = profileImageUri ?: R.drawable.user

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = 32.dp,
                bottom = 16.dp
            )
    ) {
        AsyncImage(
            model = imageModel,
            placeholder = painterResource(R.drawable.user),
            contentDescription = stringResource(R.string.profile_photo),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .border(
                    1.5.dp,
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    CircleShape
                )
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        Text(
            text = profileName,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = profileEmail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        Button(
            onClick = onEditProfileClick,
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 0.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.1f
                )
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.25f
                )
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = stringResource(R.string.edit_profile),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview
@Composable
private fun SettingsProfileHeaderPreview() {
    FyneCastTheme() {
        SettingsProfileHeader(
            profileName = "name",
            profileEmail = "email",
            profileImageUri = null,
            onEditProfileClick = {},
        )
    }
}
```

## settings\ui\components\SettingsToggleRow.kt

```kotlin
package com.mhq.fynecast.settings.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun SettingsToggleRow(
    label: String,
    isFirstOptionSelected: Boolean,
    firstOptionLabel: String,
    secondOptionLabel: String,
    onFirstOptionSelected: () -> Unit,
    onSecondOptionSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 16.dp
            )
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Ltr
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .width(180.dp)
            ) {
                SegmentedButton(
                    selected = isFirstOptionSelected,
                    onClick = onFirstOptionSelected,
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 0,
                        count = 2
                    ),
                    colors = customSegmentedColors()
                ) {
                    Text(
                        text = firstOptionLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                SegmentedButton(
                    selected = !isFirstOptionSelected,
                    onClick = onSecondOptionSelected,
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 1,
                        count = 2
                    ),
                    colors = customSegmentedColors()
                ) {
                    Text(
                        text = secondOptionLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview
@Composable
private fun SettingsToggleRowPreview() {
    FyneCastTheme() {
        SettingsToggleRow(
            label = "",
            isFirstOptionSelected = true,
            firstOptionLabel = "",
            secondOptionLabel = "",
            onFirstOptionSelected = {},
            onSecondOptionSelected = {}
        )
    }
}
```

## settings\ui\screens\SettingsContainer.kt

```kotlin
package com.mhq.fynecast.settings.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.R
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.ui.openNotificationSettings
import com.mhq.fynecast.settings.presentation.SettingsViewModel

@Composable
fun SettingsContainer(
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory),
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val profileName by settingsViewModel.profileName.collectAsStateWithLifecycle()
    val profileEmail by settingsViewModel.profileEmail.collectAsStateWithLifecycle()
    val profileImageUri by settingsViewModel.profileImageUri.collectAsStateWithLifecycle()
    val isMetric by settingsViewModel.isMetric.collectAsStateWithLifecycle()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()
    val storedDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDarkMode = storedDarkMode ?: isSystemInDarkTheme()
    val isLoggingOut by settingsViewModel.isLoggingOut.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        settingsViewModel.logoutComplete.collect {
            onLogoutClick()
        }
    }

    LaunchedEffect(settingsViewModel, resources) {
        settingsViewModel.logoutFailed.collect {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(resources.getString(R.string.logout_failed))
        }
    }

    val context = LocalContext.current
    LaunchedEffect(settingsViewModel, resources, context) {
        settingsViewModel.notificationsBlocked.collect {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.notifications_settings_message),
                actionLabel = resources.getString(R.string.notifications_settings_confirm),
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) context.openNotificationSettings()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SettingsContent(
            profileName = profileName,
            profileEmail = profileEmail,
            profileImageUri = profileImageUri,
            onEditProfileClick = onEditProfileClick,
            isMetric = isMetric,
            onMetricToggle = settingsViewModel::onUnitSystemChanged,
            notificationsEnabled = notificationsEnabled,
            onNotificationsToggle = settingsViewModel::toggleNotifications,
            currentLanguage = currentLanguage,
            onLanguageChange = settingsViewModel::onLanguageChanged,
            isDarkMode = isDarkMode,
            onDarkModeToggle = settingsViewModel::onThemeChanged,
            isLoggingOut = isLoggingOut,
            onLogoutClick = { settingsViewModel.onLogoutAction() },
            contentPadding = contentPadding,
            modifier = modifier
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding())
        )
    }
}
```

## settings\ui\screens\SettingsContent.kt

```kotlin
package com.mhq.fynecast.settings.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.settings.presentation.model.LanguageOption
import com.mhq.fynecast.settings.ui.components.SettingsLogoutButton
import com.mhq.fynecast.settings.ui.components.SettingsPanel
import com.mhq.fynecast.settings.ui.components.SettingsProfileHeader

@Composable
fun SettingsContent(
    profileName: String,
    profileEmail: String,
    profileImageUri: String?,
    onEditProfileClick: () -> Unit,
    isMetric: Boolean,
    onMetricToggle: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    currentLanguage: LanguageOption,
    onLanguageChange: (LanguageOption) -> Unit,
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    isLoggingOut: Boolean = false,
    onLogoutClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsProfileHeader(
                profileName = profileName,
                profileEmail = profileEmail,
                profileImageUri = profileImageUri,
                onEditProfileClick = onEditProfileClick,
            )
            SettingsPanel(
                isMetric = isMetric,
                onMetricToggle = onMetricToggle,
                notificationsEnabled = notificationsEnabled,
                onNotificationsToggle = onNotificationsToggle,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                isDarkMode = isDarkMode,
                onDarkModeToggle = onDarkModeToggle
            )
            SettingsLogoutButton(
                isLoggingOut = isLoggingOut,
                onLogoutClick = onLogoutClick,
            )
        }
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    FyneCastTheme() {
        SettingsContent(
            profileName = "name",
            profileEmail = "email",
            profileImageUri = null,
            onEditProfileClick = {},
            isMetric = true,
            onMetricToggle = {},
            notificationsEnabled = true,
            onNotificationsToggle = {},
            currentLanguage = LanguageOption.English,
            onLanguageChange = {},
            isDarkMode = true,
            onDarkModeToggle = {},
            onLogoutClick = {},
            contentPadding = PaddingValues(0.dp)
        )
    }
}
```

