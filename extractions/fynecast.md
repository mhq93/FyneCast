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

## alerts\data\dto\AlertDto.kt

```kotlin
package com.mhq.fynecast.alerts.data.dto

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

## alerts\data\dto\AlertsDto.kt

```kotlin
package com.mhq.fynecast.alerts.data.dto

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

## alerts\data\mapper\AlertsMapper.kt

```kotlin
package com.mhq.fynecast.alerts.data.mapper

import com.mhq.fynecast.alerts.data.database.TrackedAlertEntity
import com.mhq.fynecast.alerts.data.dto.AlertDto
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import java.time.OffsetDateTime

/**
 * Calculates a unique string identifier for an alert based on its event type, effective timestamp, and affected areas.
 * 
 * @receiver [AlertDto] The raw alert data transfer object from the network response.
 * @return A unique composite string key formatted as `${event}_${effective}_$areas`.
 */
fun AlertDto.calculateId(): String = "${event}_${effective}_$areas"

/**
 * Maps an [AlertDto] data transfer object into the core domain model [AlertDomainModel].
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

## alerts\data\repoimpl\AlertsRepoImpl.kt

```kotlin
package com.mhq.fynecast.alerts.data.repoimpl

import com.mhq.fynecast.alerts.data.database.AlertDao
import com.mhq.fynecast.alerts.data.mapper.toDomain
import com.mhq.fynecast.alerts.data.mapper.toEntity
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository

/**
 * Concrete implementation of the [AlertsRepository] using Room database operations.
 */
class AlertsRepoImpl(private val alertDao: AlertDao) : AlertsRepository {
    override fun getTrackedAlertIds() =
        alertDao.getTrackedAlertIds()

    override suspend fun getTrackedAlerts() =
        alertDao.getAllTrackedAlerts().map { it.toDomain() }

    override suspend fun saveTrackedAlert(alert: AlertDomainModel, location: TrackedLocation) =
        alertDao.insertTrackedAlert(alert.toEntity(location))

    override suspend fun deleteTrackedAlert(alertId: String) =
        alertDao.deleteTrackedAlertById(alertId)

    override suspend fun deleteAllTrackedAlerts() =
        alertDao.deleteAllTrackedAlerts()
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
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.notifications.WeatherAlertTrackingWorker
import com.mhq.fynecast.alerts.notifications.WeatherRefreshWorker
import com.mhq.fynecast.alerts.notifications.ui.AlertsNotificationManager
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

## alerts\domain\models\AlertDomainModel.kt

```kotlin
package com.mhq.fynecast.alerts.domain.models

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

## alerts\domain\models\TrackedAlertDomainModel.kt

```kotlin
package com.mhq.fynecast.alerts.domain.models

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
    /** Derives a [TrackedLocation] instance from the alert's location properties. */
    val location: TrackedLocation
        get() = TrackedLocation(
            cityName,
            latitude,
            longitude
        )
}
```

## alerts\domain\models\TrackedLocation.kt

```kotlin
package com.mhq.fynecast.alerts.domain.models

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

## alerts\domain\repository\AlertsRepository.kt

```kotlin
package com.mhq.fynecast.alerts.domain.repository

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
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
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.core.domain.time.TimeProvider
import com.mhq.fynecast.home.domain.models.WeatherDomainModel
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

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.home.domain.repository.CurrentWeatherStore
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

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
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

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
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

import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase

/**
 * Use case that saves the user's response to the notification permission request.
 */
class HandleNotificationPermissionResultUseCase(
    private val updatePreferenceUseCase: UpdatePreferenceUseCase
) {
    suspend operator fun invoke(granted: Boolean) {
        updatePreferenceUseCase.notifications(granted)
    }
}
```

## alerts\notifications\domain\usecases\SyncNotificationPermissionUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.alerts.domain.usecases.CancelAllAlertTrackingUseCase
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase
import kotlinx.coroutines.flow.first

/**
 * Use case that synchronizes notification preferences with actual system runtime permissions.
 */
class SyncNotificationPermissionUseCase(
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val updatePreferenceUseCase: UpdatePreferenceUseCase,
    private val cancelAllAlertTrackingUseCase: CancelAllAlertTrackingUseCase
) {
    suspend operator fun invoke() {
        val current = observeUserPreferencesUseCase().first()
        if (current.notificationsEnabled && !notificationPermissionChecker.isGranted()) {
            // Permission was revoked in system settings: nothing can be shown, so stop tracking.
            cancelAllAlertTrackingUseCase()
            updatePreferenceUseCase.notifications(false)
        }
    }
}
```

## alerts\notifications\domain\usecases\ToggleNotificationsUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.alerts.domain.usecases.CancelAllAlertTrackingUseCase
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase

/**
 * Use case for enabling or disabling notifications, handling permissions and tracker cleanup.
 */
class ToggleNotificationsUseCase(
    private val updatePreferenceUseCase: UpdatePreferenceUseCase,
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
            updatePreferenceUseCase.notifications(false)
            return Result.Disabled
        }

        return if (notificationPermissionChecker.isGranted()) {
            updatePreferenceUseCase.notifications(true)
            Result.Enabled
        } else {
            Result.PermissionRequired
        }
    }
}
```

## alerts\notifications\ui\AlertsNotificationManager.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.ui

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

## alerts\notifications\WeatherAlertTrackingWorker.kt

```kotlin
package com.mhq.fynecast.alerts.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertStatusUseCase
import com.mhq.fynecast.alerts.notifications.ui.AlertsNotificationManager
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
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.alerts.notifications.ui.AlertsNotificationManager
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.settings.domain.models.AppLanguage
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
            val lang = AppLanguage.fromStoredName(prefs.currentLanguage).code
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

## alerts\ui\AlertsEvent.kt

```kotlin
package com.mhq.fynecast.alerts.ui

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

## alerts\ui\AlertsUiState.kt

```kotlin
package com.mhq.fynecast.alerts.ui

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel

/**
 * Represents the UI state of the alerts screen.
 */
sealed interface AlertsUiState {
    object Loading : AlertsUiState
    object Unavailable : AlertsUiState
    data class Success(val alerts: List<AlertDomainModel>) : AlertsUiState
}
```

## alerts\ui\AlertsViewModel.kt

```kotlin
package com.mhq.fynecast.alerts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.alerts.domain.usecases.GetAlertsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertIdsUseCase
import com.mhq.fynecast.alerts.domain.usecases.ToggleAlertsUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentLocationUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

/**
 * ViewModel managing state, user actions, permission flows, and event channels for the alerts feature.
 */
class AlertsViewModel(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val getTrackedAlertIdsUseCase: GetTrackedAlertIdsUseCase,
    private val observeCurrentLocationUseCase: ObserveCurrentLocationUseCase,
    private val toggleAlertsUseCase: ToggleAlertsUseCase,
    private val toggleNotificationsUseCase: ToggleNotificationsUseCase,
    private val handleNotificationPermissionResultUseCase: HandleNotificationPermissionResultUseCase
) : ViewModel() {

    /** Exposes reactive UI state for alerts, transitioning between loading and success states. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AlertsUiState> = getAlertsUseCase()
        .transformLatest { alerts ->
            if (alerts != null) {
                emit(AlertsUiState.Success(alerts))
            } else {
                emit(AlertsUiState.Loading)
                delay(LOAD_TIMEOUT_MILLIS)
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
    val currentLocation: StateFlow<TrackedLocation?> =
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
    fun onAlertTrackToggled(alert: AlertDomainModel) {
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
            ToggleAlertsUseCase.Result.NotificationsRequired -> { pendingAlert = alert; _showNotificationsPrompt.value = true }
            ToggleAlertsUseCase.Result.Untracked -> Unit
        }
    }

    companion object {
        private const val LOAD_TIMEOUT_MILLIS = 20_000L

        /** Factory providing dependency injection wiring from the application container. */
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)
                AlertsViewModel(
                    getAlertsUseCase = app.container.getAlertsUseCase,
                    getTrackedAlertIdsUseCase = app.container.getTrackedAlertIdsUseCase,
                    observeCurrentLocationUseCase = app.container.observeCurrentLocationUseCase,
                    toggleAlertsUseCase = app.container.toggleAlertsUseCase,
                    toggleNotificationsUseCase = app.container.toggleNotificationsUseCase,
                    handleNotificationPermissionResultUseCase = app.container.handleNotificationPermissionResultUseCase,
                )
            }
        }
    }
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
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.core.ui.theme.AppleRed
import com.mhq.fynecast.core.ui.theme.EditorialRoyalBlue
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.core.ui.theme.Tangerine
import com.mhq.fynecast.core.ui.theme.VividAmber

/**
 * Composable card displaying a weather alert item with expandable details and tracking controls.
 */
@Composable
fun AlertCard(
    alert: AlertDomainModel,
    isTracked: Boolean,
    isExpanded: Boolean,
    onTrackToggle: () -> Unit,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val severityColor = remember(alert.severity) {
        when (alert.severity.lowercase()) {
            "extreme" -> AppleRed
            "severe" -> Tangerine
            "moderate" -> VividAmber
            else -> EditorialRoyalBlue
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    )
                )
                .then(
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                severityColor.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                )
                .clip(RoundedCornerShape(24.dp))
                .clickable { onExpandToggle() }
                .padding(20.dp)
        ) {
            // Upper Header Layer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = severityColor,
                                shape = RoundedCornerShape(50)
                            )
                    )
                    Text(
                        text = alert.event.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = severityColor
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = severityColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = alert.severity.uppercase(),
                        color = severityColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Headline Layer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = alert.headline,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Smooth Expanded Metadata Area
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = alert.description,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )

                    if (alert.instruction.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
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
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
            alert = AlertDomainModel(
                id = "",
                headline = "",
                severity = "",
                event = "",
                effectiveMillis = 10L,
                expiresMillis = 10L,
                description = "",
                instruction = ""
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.R
import com.mhq.fynecast.alerts.ui.AlertsEvent
import com.mhq.fynecast.alerts.ui.AlertsUiState
import com.mhq.fynecast.alerts.ui.AlertsViewModel
import com.mhq.fynecast.alerts.ui.components.NotificationsDialog
import kotlinx.coroutines.launch

/**
 * Root container composable for the alerts feature, managing snackbars, dialogs, and permission flows.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AlertsContainer(
    alertsViewModel: AlertsViewModel = viewModel(factory = AlertsViewModel.factory),
    contentPadding: PaddingValues = PaddingValues(0.dp),
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
                        onTrackToggle = { alertsViewModel.onAlertTrackToggled(it) },
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

private fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    )
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
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.ui.components.AlertCard

private val ExpandedAlertIdsSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() }
)

/**
 * Composable list content displaying the lazy column of available weather alert cards.
 */
@Composable
fun AlertsContent(
    alerts: List<AlertDomainModel>,
    trackedAlertIds: List<String>,
    onTrackToggle: (AlertDomainModel) -> Unit,
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
                    onTrackToggle = { onTrackToggle(alert) }
                )
            }
        }
    }
}
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
import com.mhq.fynecast.favorites.ui.screens.FavoritesBlank

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
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.mhq.fynecast.auth.data.repository.auth.SocialAuthCredentialProvider
import com.mhq.fynecast.auth.data.session.SessionKeys
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val sessionDataStore: DataStore<Preferences>,
    private val socialAuthCredentialProvider: SocialAuthCredentialProvider
) : AuthRepository {

    override suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): Result<UserDomainModel> = runCatching {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("Auth registration failed.")

        /** Account already exists at this point; displayName is cosmetic,
         * so a failure here shouldn't fail the whole registration.
         * Retry once so a transient failure doesn't leave the name unpersisted server-side,
         * and surface it if it still doesn't stick so it isn't silently lost. */
        val profileSynced = runCatching {
            user.updateProfile(userProfileChangeRequest { displayName = name }).await()
        }.isSuccess
        if (!profileSynced) {
            val retrySynced = runCatching {
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
        runCatching {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Auth login failed.")

            /** The local cache lookup is a best-effort fallback only;
             * a DataStore read failure here must not fail an otherwise-successful login. */
            val snapshot = runCatching { sessionDataStore.data.first() }.getOrNull()
            val cachedUsername = snapshot?.get(SessionKeys.USERNAME)
                ?.takeIf { snapshot[SessionKeys.USER_ID] == user.uid }
            val resolvedName = user.displayName ?: cachedUsername ?: "Username"
            UserDomainModel(id = user.uid, name = resolvedName, email = user.email ?: email)
        }

    override suspend fun loginWithGoogle(idToken: String): Result<UserDomainModel> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        val user = authResult.user ?: throw Exception("Google token confirmation failed.")
        UserDomainModel(user.uid, user.displayName ?: "", user.email ?: "")
    }

    override suspend fun loginWithFacebook(accessToken: String): Result<UserDomainModel> =
        runCatching {
            val credential = FacebookAuthProvider.getCredential(accessToken)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Facebook session parsing failed.")
            UserDomainModel(user.uid, user.displayName ?: "", user.email ?: "")
        }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    override fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null

    override suspend fun saveLocalSession(user: UserDomainModel) {
        sessionDataStore.edit { prefs ->
            prefs[SessionKeys.USER_ID] = user.id
            prefs[SessionKeys.USERNAME] = user.name
            prefs[SessionKeys.EMAIL] = user.email
        }
    }

    override suspend fun clearSession() {
        firebaseAuth.signOut()
        sessionDataStore.edit { prefs -> prefs.clear() }
        socialAuthCredentialProvider.clearCredentials()
    }
}
```

## auth\data\repoimpl\auth\SocialAuthCredentialProviderImpl.kt

```kotlin
package com.mhq.fynecast.auth.data.repoimpl.auth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mhq.fynecast.auth.data.repository.auth.SocialAuthCredentialProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SocialAuthCredentialProviderImpl(
    private val appContext: Context,
    private val serverClientId: String
) : SocialAuthCredentialProvider {

    override suspend fun getGoogleIdToken(context: Context): String {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = credentialManager.getCredential(context, request)
        return GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    }

    override suspend fun getFacebookAccessToken(activity: ComponentActivity): String =
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
import com.mhq.fynecast.auth.ui.editprofile.ImageStorageManager
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

    override fun commitProfileImage(uriString: String) {
        imageStorageManager.commitProfileImage(uriString)
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.mhq.fynecast.auth.data.session.SessionKeys
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
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
        UserDomainModel(user?.uid ?: snapshot[SessionKeys.USER_ID] ?: "", name, email, img)
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
        runCatching {
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

    override suspend fun refreshActiveUserSession() {
        /** Picks up an email change verified outside the app. (Best effort), offline is fine. */
        try {
            firebaseAuth.currentUser?.reload()?.await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        _profileState.value = getProfile()
    }
}
```

## auth\data\repository\auth\SocialAuthCredentialProvider.kt

```kotlin
package com.mhq.fynecast.auth.data.repository.auth

import android.content.Context
import androidx.activity.ComponentActivity

interface SocialAuthCredentialProvider {
    suspend fun getGoogleIdToken(context: Context): String
    suspend fun getFacebookAccessToken(activity: ComponentActivity): String
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
}
```

## auth\data\usecases\auth\AuthenticateWithSocialProviderUseCase.kt

```kotlin
package com.mhq.fynecast.auth.data.usecases.auth

import android.content.Context
import androidx.activity.ComponentActivity
import com.mhq.fynecast.auth.data.repository.auth.SocialAuthCredentialProvider
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.usecases.auth.AuthenticateWithSocialTokenUseCase
import kotlinx.coroutines.CancellationException

class AuthenticateWithSocialProviderUseCase(
    private val socialAuthCredentialProvider: SocialAuthCredentialProvider,
    private val authenticateWithSocialTokenUseCase: AuthenticateWithSocialTokenUseCase
) {
    suspend fun withGoogle(context: Context): Result<UserDomainModel> = runCatching {
        socialAuthCredentialProvider.getGoogleIdToken(context)
    }.fold(
        onSuccess = { token -> authenticateWithSocialTokenUseCase(token, isGoogle = true) },
        onFailure = { e ->
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    )

    suspend fun withFacebook(activity: ComponentActivity): Result<UserDomainModel> = runCatching {
        socialAuthCredentialProvider.getFacebookAccessToken(activity)
    }.fold(
        onSuccess = { token -> authenticateWithSocialTokenUseCase(token, isGoogle = false) },
        onFailure = { e ->
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    )
}
```

## auth\domain\models\UserDomainModel.kt

```kotlin
package com.mhq.fynecast.auth.domain.models

data class UserDomainModel(
    val id: String,
    val name: String,
    val email: String,
    val profileImageUri: String? = null
)
```

## auth\domain\models\ValidationResult.kt

```kotlin
package com.mhq.fynecast.auth.domain.models

sealed interface ValidationResult {
    object Success : ValidationResult
    data class Failure(val errorMessage: String) : ValidationResult
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


    // Password Reset
    suspend fun sendPasswordResetEmail(
        email: String
    ): Result<Unit>


    // Session Management persistence
    fun isUserLoggedIn(): Boolean
    suspend fun saveLocalSession(
        user: UserDomainModel
    )
    suspend fun clearSession()
}
```

## auth\domain\repository\profile\ImageStorageRepository.kt

```kotlin
package com.mhq.fynecast.auth.domain.repository.profile

interface ImageStorageRepository {
    fun isAlreadySandboxed(uriString: String?): Boolean
    suspend fun sandboxAndCompressImage(uriString: String): String?
    fun commitProfileImage(uriString: String)
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
    suspend fun refreshActiveUserSession()
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

import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository

class SendPasswordResetUseCase(
    private val authRepository: AuthRepository,
    private val validateLoginFormUseCase: ValidateLoginFormUseCase
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email cannot be empty."))
        }

        if (!validateLoginFormUseCase.validateEmailFormat(email)) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }

        return authRepository.sendPasswordResetEmail(email)
    }
}
```

## auth\domain\usecases\auth\ValidateLoginFormUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.validation.EmailFormatValidator

class ValidateLoginFormUseCase {
    fun validateEmailFormat(email: String): Boolean {
        return EmailFormatValidator.isValidOrEmpty(email)
    }

    operator fun invoke(email: String, password: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Failure("Email field cannot be empty.")
            !EmailFormatValidator.isValid(email) -> ValidationResult.Failure("Please, enter a valid email address.")
            password.isBlank() -> ValidationResult.Failure("Password field cannot be empty.")
            else -> ValidationResult.Success
        }
    }
}
```

## auth\domain\usecases\auth\ValidateSignupFormUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.validation.EmailFormatValidator

class ValidateSignupFormUseCase {
    private val usernameRegex =
        Regex("^[a-zA-Z0-9_]{3,15}$")

    private val passwordRegex =
        Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$")

    fun validateUsername(username: String): Boolean =
        username.isEmpty() || username.matches(usernameRegex)

    fun validateEmail(email: String): Boolean =
        EmailFormatValidator.isValidOrEmpty(email)

    fun validatePassword(password: String): Boolean =
        password.isEmpty() || password.matches(passwordRegex)

    fun validatePasswordMatch(password: String, confirmPassword: String): Boolean =
        confirmPassword.isEmpty() || password == confirmPassword

    operator fun invoke(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Failure("Name field cannot be empty.")
            !username.matches(usernameRegex) -> ValidationResult.Failure("Name must be 3-15 non-special characters.")

            email.isBlank() -> ValidationResult.Failure("Email field cannot be empty.")
            !EmailFormatValidator.isValid(email) -> ValidationResult.Failure("Please, enter a valid email address.")

            password.isBlank() -> ValidationResult.Failure("Password field cannot be empty.")
            !password.matches(passwordRegex) -> ValidationResult.Failure("Password must be +6 chars with upper, lower, numeric and special character.")

            confirmPassword.isBlank() -> ValidationResult.Failure("Please, confirm your password.")
            password != confirmPassword -> ValidationResult.Failure("Passwords do not match.")

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

## auth\domain\usecases\profile\UpdateUserProfileUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.repository.profile.ImageStorageRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository

class UpdateUserProfileUseCase(
    private val profileRepository: UserProfileRepository,
    private val imageRepository: ImageStorageRepository
) {
    suspend operator fun invoke(
        newName: String,
        newEmail: String,
        localImageUriString: String?
    ): Result<Boolean> {
        val currentProfile = profileRepository.getProfile()
        var finalImageUri = currentProfile.profileImageUri
        var newImageWasCreated = false

        if (!localImageUriString.isNullOrBlank()) {
            if (!imageRepository.isAlreadySandboxed(localImageUriString)) {
                val sandboxedUri = imageRepository.sandboxAndCompressImage(localImageUriString)
                if (sandboxedUri != null) {
                    finalImageUri = sandboxedUri
                    newImageWasCreated = true
                }
            } else {
                finalImageUri = localImageUriString
            }
        }

        var profileSaved = false

        return runCatching {
            val remoteUpdateVerification =
                profileRepository.updateRemoteProfile(newName, newEmail).getOrThrow()

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

             /**The profile now durably references the new image,
              * so it's safe to purge the old one (ones).
              * Doing this any earlier risked leaving
              * the profile pointing at an image that was already deleted
              * if the save above failed.*/
            if (newImageWasCreated) {
                imageRepository.commitProfileImage(finalImageUri!!)
            }

            remoteUpdateVerification
        }.onFailure {
             /**Only roll back if the profile never ended up pointing at this file.
              * If saveProfile already succeeded
              * and a later step (refreshActiveUserSession, commitProfileImage)
              * is what threw, deleting the file now would leave
              * the saved profile pointing at a file that no longer exists
              * — the exact thing this rollback exists to prevent.*/
            if (newImageWasCreated && !profileSaved) {
                imageRepository.deleteProfileImage(finalImageUri!!)
            }
        }
    }
}
```

## auth\domain\usecases\profile\ValidateEditProfileUseCase.kt

```kotlin
package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.validation.EmailFormatValidator

class ValidateEditProfileUseCase {
    private val usernameRegex = Regex("^[a-zA-Z0-9_ -]{3,30}$")

    fun validateUsernameFormat(name: String): Boolean {
        return name.isEmpty() || name.matches(usernameRegex)
    }

    fun validateEmailFormat(email: String): Boolean {
        return EmailFormatValidator.isValidOrEmpty(email)
    }

    operator fun invoke(username: String, email: String): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Failure("Name field cannot be left empty.")
            !username.matches(usernameRegex) -> ValidationResult.Failure("Username must be 3-30 characters (spaces/letters/numbers allowed).")
            email.isBlank() -> ValidationResult.Failure("Email field cannot be left empty.")
            !EmailFormatValidator.isValid(email) -> ValidationResult.Failure("Please enter a valid email address.")
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
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase

class LogoutUserUseCase(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val cancelAllAlertTrackingUseCase: CancelAllAlertTrackingUseCase,
    private val updatePreferenceUseCase: UpdatePreferenceUseCase
) {
    suspend operator fun invoke(): Result<Unit> = runCatching {
        authRepository.clearSession()
        userProfileRepository.refreshActiveUserSession()

        runCatching { cancelAllAlertTrackingUseCase() }
        runCatching { updatePreferenceUseCase.clearLastSearchedCity() }
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

## auth\ui\editprofile\ImageStorageManager.kt

```kotlin
package com.mhq.fynecast.auth.ui.editprofile

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

                val correctedBitmap = faceCorrectImageOrientation(sourceUri, sampledBitmap)

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
     * Deletes every sandboxed profile image except the one referenced by
     * [keepUriString]. Call this only after the profile record has been
     * durably saved with the new image's URI — deleting the old image any
     * earlier risks leaving the profile pointing at a file that no longer
     * exists if the save fails.
     */
    fun commitProfileImage(keepUriString: String) {
        val keepFileName = keepUriString.toUri().path?.let { File(it).name }
        clearOldProfileImages(excludeFileName = keepFileName)
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

    /**
     * Purges previously generated files inside the profile_pics sandbox container directory,
     * keeping [excludeFileName] (the file that was just written) if provided.
     */
    private fun clearOldProfileImages(excludeFileName: String? = null) {
        try {
            val profilePicturesDir = File(context.filesDir, "profile_pics")
            if (profilePicturesDir.exists() && profilePicturesDir.isDirectory) {
                val files = profilePicturesDir.listFiles()
                files?.forEach { file ->
                    if (file.isFile && file.name != excludeFileName) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isWithinFilesDir(path: String?): Boolean {
        if (path == null) return false
        val canonicalFilesDir = File(context.filesDir.path).canonicalPath + File.separator
        val canonicalTarget = runCatching { File(path).canonicalPath }.getOrNull() ?: return false
        return canonicalTarget.startsWith(canonicalFilesDir)
    }

    private fun faceCorrectImageOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
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
import androidx.compose.material.icons.filled.Person
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

## auth\ui\editprofile\ui\EditProfileUiState.kt

```kotlin
package com.mhq.fynecast.auth.ui.editprofile.ui

sealed interface EditProfileUiState {
    object Idle : EditProfileUiState
    object Loading : EditProfileUiState
    object Success : EditProfileUiState
    data class Error(val message: String) : EditProfileUiState
}
```

## auth\ui\editprofile\ui\EditProfileViewModel.kt

```kotlin
package com.mhq.fynecast.auth.ui.editprofile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.usecases.profile.ObserveUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.UpdateUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ValidateEditProfileUseCase
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class EditProfileViewModel(
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val validateEditProfileUseCase: ValidateEditProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) : ViewModel() {

    private val _editProfileUiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Idle)

    private val _uiEventChannel = Channel<String>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()

    private val _navigationEventChannel = Channel<Unit>(Channel.BUFFERED)
    val navigationEvent = _navigationEventChannel.receiveAsFlow()

    private val _profileImageUri = MutableStateFlow<String?>(null)
    val profileImageUri = _profileImageUri.asStateFlow()
    fun onProfileImagePicked(uriString: String) {
        _profileImageUri.value = uriString
    }

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()
    fun onUsernameChanged(newUsername: String) {
        _username.value = newUsername
    }
    val isUsernameValid = _username
        .map { validateEditProfileUseCase.validateUsernameFormat(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    private val _userEmail = MutableStateFlow("")
    val userEmail = _userEmail.asStateFlow()
    fun onUserEmailChanged(newEmail: String) {
        _userEmail.value = newEmail.trim()
    }
    val isEmailValid = _userEmail
        .map { validateEditProfileUseCase.validateEmailFormat(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    init {
        viewModelScope.launch {
            /** Seed the form once.
            The flow starts with a placeholder (empty id) and re-emits on every refresh,
            so collecting it would overwrite what the user is typing. */
            val profile = observeUserProfileUseCase().first { it.id.isNotEmpty() }
            if (_username.value.isEmpty()) _username.value = profile.name
            if (_userEmail.value.isEmpty()) _userEmail.value = profile.email
            if (_profileImageUri.value == null) _profileImageUri.value = profile.profileImageUri
        }
    }

    fun handleSavingProfileChanges() {
        when (
            val validation = validateEditProfileUseCase(
                _username.value,
                _userEmail.value
            )
        ) {
            is ValidationResult.Success -> {
                if (_editProfileUiState.value is EditProfileUiState.Loading) return
                _editProfileUiState.value = EditProfileUiState.Loading

                viewModelScope.launch {
                    _uiEventChannel.send("Processing image and saving changes...")

                    updateUserProfileUseCase(
                        newName = _username.value,
                        newEmail = _userEmail.value,
                        localImageUriString = _profileImageUri.value
                    )
                        .onSuccess { verificationSent ->
                            _editProfileUiState.value = EditProfileUiState.Success
                            _uiEventChannel.send(
                                if (verificationSent)
                                    "Saved. Check your new email inbox to verify the change."
                                else
                                    "Profile changes saved successfully!"
                            )
                            delay(SUCCESS_MESSAGE_DISPLAY_DELAY_MS.milliseconds)
                            _navigationEventChannel.send(Unit)
                        }
                        .onFailure { e ->
                            val fallbackMessage = e.localizedMessage ?: "Failed to update profile settings."
                            _editProfileUiState.value = EditProfileUiState.Error(fallbackMessage)
                            _uiEventChannel.send(fallbackMessage)
                        }
                }
            }

            is ValidationResult.Failure -> {
                viewModelScope.launch { _uiEventChannel.send(validation.errorMessage) }
            }
        }
    }

    companion object {
        private const val SUCCESS_MESSAGE_DISPLAY_DELAY_MS = 1500L

        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)

                EditProfileViewModel(
                    observeUserProfileUseCase = app.container.observeUserProfileUseCase,
                    validateEditProfileUseCase = app.container.validateEditProfileUseCase,
                    updateUserProfileUseCase = app.container.updateUserProfileUseCase
                )
            }
        }
    }
}
```

## auth\ui\editprofile\ui\screens\EditProfileContainer.kt

```kotlin
package com.mhq.fynecast.auth.ui.editprofile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.auth.ui.editprofile.ui.EditProfileViewModel
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import com.mhq.fynecast.core.ui.theme.AbyssalVoid
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen
import kotlinx.coroutines.launch

@Composable
fun EditProfileContainer(
    editProfileViewModel: EditProfileViewModel = viewModel(factory = EditProfileViewModel.factory),
    onBackClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val profileImageUri by editProfileViewModel.profileImageUri.collectAsState()
    val username by editProfileViewModel.username.collectAsState()
    val isUsernameValid by editProfileViewModel.isUsernameValid.collectAsState()
    val userEmail by editProfileViewModel.userEmail.collectAsState()
    val isEmailValid by editProfileViewModel.isEmailValid.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        editProfileViewModel.navigationEvent.collect {
            onBackClick()
        }
    }

    LaunchedEffect(Unit) {
        editProfileViewModel.uiEvent.collect { message ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = message,
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
            onUsernameChanged = editProfileViewModel::onUsernameChanged,
            onUserEmailChanged = editProfileViewModel::onUserEmailChanged,
            onBackClick = onBackClick,
            onSaveClick = {
                editProfileViewModel.handleSavingProfileChanges()
            },
            contentPadding = contentPadding,
            modifier = modifier,
            profileImageUri = profileImageUri,
            onProfileImagePicked = editProfileViewModel::onProfileImagePicked
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .padding(horizontal = 24.dp)
        ) {
            data -> AuthWarningSnackbar(data)
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
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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

## auth\ui\forgotpassword\ui\ForgotPasswordUiState.kt

```kotlin
package com.mhq.fynecast.auth.ui.forgotpassword.ui

sealed interface ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState
    object Loading : ForgotPasswordUiState
    object Success : ForgotPasswordUiState
    data class Error(val message: String) : ForgotPasswordUiState
}
```

## auth\ui\forgotpassword\ui\ForgotPasswordViewModel.kt

```kotlin
package com.mhq.fynecast.auth.ui.forgotpassword.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.auth.domain.usecases.auth.SendPasswordResetUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateLoginFormUseCase
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ForgotPasswordViewModel(
    private val validateLoginFormUseCase: ValidateLoginFormUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase
) : ViewModel() {

    private val _forgotPasswordUiState =
        MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    private val _uiEventChannel = Channel<String>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()
    private val _navigationEvent = Channel<Unit>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()
    fun onEmailChanged(newEmail: String) {
        _email.value = newEmail.trim()
    }

    val isEmailValid = _email
        .map { validateLoginFormUseCase.validateEmailFormat(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    fun handlePasswordReset() {
        val state = _forgotPasswordUiState.value
        if (state is ForgotPasswordUiState.Loading || state is ForgotPasswordUiState.Success) return
        viewModelScope.launch {
            _forgotPasswordUiState.value = ForgotPasswordUiState.Loading
            sendPasswordResetUseCase(_email.value)
                .onSuccess {
                    _forgotPasswordUiState.value = ForgotPasswordUiState.Success
                    _uiEventChannel.send("Reset link successfully sent to ${_email.value}")
                    delay(SUCCESS_MESSAGE_DISPLAY_DELAY_MS.milliseconds)
                    _navigationEvent.send(Unit)
                }
                .onFailure { error ->
                    val msg = error.localizedMessage ?: "Failed to send reset link. Try again."
                    _forgotPasswordUiState.value = ForgotPasswordUiState.Error(msg)
                    _uiEventChannel.send(msg)
                }
        }
    }

    companion object {
        private const val SUCCESS_MESSAGE_DISPLAY_DELAY_MS = 1500L

        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)

                ForgotPasswordViewModel(
                    validateLoginFormUseCase = app.container.validateLoginFormUseCase,
                    sendPasswordResetUseCase = app.container.sendPasswordResetUseCase
                )
            }
        }
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.auth.ui.forgotpassword.ui.ForgotPasswordViewModel
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ForgotPasswordContainer(
    forgotPasswordViewModel: ForgotPasswordViewModel = viewModel(factory = ForgotPasswordViewModel.factory),
    onSendLinkSuccessNavigate: () -> Unit,
    onBackToLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val email by forgotPasswordViewModel.email.collectAsStateWithLifecycle()
    val isEmailValid by forgotPasswordViewModel.isEmailValid.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        forgotPasswordViewModel.navigationEvent.collect { onSendLinkSuccessNavigate() }
    }

    LaunchedEffect(Unit) {
        forgotPasswordViewModel.uiEvent.collectLatest { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ForgotPasswordContent(
            email = email,
            isEmailValid = isEmailValid,
            onEmailChanged = forgotPasswordViewModel::onEmailChanged,
            onBackToLoginClick = onBackToLoginClick,
            onSendLink = { forgotPasswordViewModel.handlePasswordReset() },
            modifier = modifier
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

## auth\ui\login\ui\LoginUiState.kt

```kotlin
package com.mhq.fynecast.auth.ui.login.ui

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}
```

## auth\ui\login\ui\LoginViewModel.kt

```kotlin
package com.mhq.fynecast.auth.ui.login.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.auth.data.usecases.auth.AuthenticateWithSocialProviderUseCase
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.usecases.auth.LoginWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateLoginFormUseCase
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(
    private val validateLoginFormUseCase: ValidateLoginFormUseCase,
    private val loginWithEmailUseCase: LoginWithEmailUseCase,
    private val authenticateWithSocialProviderUseCase: AuthenticateWithSocialProviderUseCase
) : ViewModel() {

    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginUiState = _loginUiState.asStateFlow()
    private val _uiEventChannel = Channel<String>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()
    private val _navigationEvent = Channel<Unit>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()
    fun onEmailChanged(newEmail: String) {
        _email.value = newEmail.trim()
    }
    val isEmailValid = _email
        .map { validateLoginFormUseCase.validateEmailFormat(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()
    fun onPasswordChanged(newPassword: String) {
        _password.value = newPassword
    }
    private val _passwordVisible = MutableStateFlow(false)
    val passwordVisible = _passwordVisible.asStateFlow()
    fun togglePasswordVisibility() {
        _passwordVisible.value = !_passwordVisible.value
    }

    fun validateThenLogin() {
        if (_loginUiState.value is LoginUiState.Loading) return
        when (val validation = validateLoginFormUseCase(_email.value, _password.value)) {
            is ValidationResult.Success -> {
                viewModelScope.launch {
                    _loginUiState.value = LoginUiState.Loading
                    loginWithEmailUseCase(_email.value, _password.value)
                        .onSuccess {
                            _loginUiState.value = LoginUiState.Success
                            _navigationEvent.send(Unit)
                        }
                        .onFailure { handleLoginFailure(it) }
                }
            }
            is ValidationResult.Failure -> {
                viewModelScope.launch { _uiEventChannel.send(validation.errorMessage) }
            }
        }
    }

    suspend fun onGoogleAuthenticate(context: Context) =
        runSocialSignIn("Google sign-in failed") {
            authenticateWithSocialProviderUseCase.withGoogle(context)
        }

    suspend fun onFacebookAuthenticate(activity: ComponentActivity) =
        runSocialSignIn("Facebook sign-in failed") {
            authenticateWithSocialProviderUseCase.withFacebook(activity)
        }

    private suspend fun runSocialSignIn(
        failureMessage: String,
        signIn: suspend () -> Result<UserDomainModel>
    ) {
        if (_loginUiState.value is LoginUiState.Loading) return
        _loginUiState.value = LoginUiState.Loading
        try {
            signIn()
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
        val message = error.localizedMessage ?: "Invalid email or password configuration."
        _loginUiState.value = LoginUiState.Error(message)
        _uiEventChannel.send(message)
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)

                LoginViewModel(
                    validateLoginFormUseCase = app.container.validateLoginFormUseCase,
                    loginWithEmailUseCase = app.container.loginWithEmailUseCase,
                    authenticateWithSocialProviderUseCase = app.container.authenticateWithSocialProviderUseCase
                )
            }
        }
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.auth.ui.login.ui.LoginViewModel
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun LoginContainer(
    loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.factory),
    onLoginClick: () -> Unit,
    onSignUpNavigate: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val email by loginViewModel.email.collectAsStateWithLifecycle()
    val isEmailValid by loginViewModel.isEmailValid.collectAsStateWithLifecycle()
    val password by loginViewModel.password.collectAsStateWithLifecycle()
    val isPasswordVisible by loginViewModel.passwordVisible.collectAsStateWithLifecycle()

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
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LoginContent(
            email = email,
            isEmailValid = isEmailValid,
            onEmailChanged = loginViewModel::onEmailChanged,
            password = password,
            onPasswordChanged = loginViewModel::onPasswordChanged,
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = loginViewModel::togglePasswordVisibility,
            onSignUpClick = onSignUpNavigate,
            onLoginClick = { loginViewModel.validateThenLogin() },
            onForgotPasswordClick = onForgotPasswordClick,
            onGoogleAuthenticate = {
                scope.launch { loginViewModel.onGoogleAuthenticate(context) }
            },
            onFacebookAuthenticate = {
                (context as? ComponentActivity)?.let { activity ->
                    scope.launch { loginViewModel.onFacebookAuthenticate(activity) }
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.auth.ui.signup.ui.SignupViewModel
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SignupContainer(
    signupViewModel: SignupViewModel = viewModel(factory = SignupViewModel.factory),
    onSignupClick: () -> Unit,
    onLoginNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userName by signupViewModel.username.collectAsStateWithLifecycle()
    val isUsernameValid by signupViewModel.isUsernameValid.collectAsStateWithLifecycle()
    val userEmail by signupViewModel.userEmail.collectAsStateWithLifecycle()
    val isEmailValid by signupViewModel.isEmailValid.collectAsStateWithLifecycle()
    val userPassword by signupViewModel.userPassword.collectAsStateWithLifecycle()
    val isPasswordValid by signupViewModel.isPasswordValid.collectAsStateWithLifecycle()
    val confirmPassword by signupViewModel.confirmPassword.collectAsStateWithLifecycle()
    val doPasswordsMatch by signupViewModel.doPasswordsMatch.collectAsStateWithLifecycle()
    val arePasswordsVisible by signupViewModel.arePasswordsVisible.collectAsStateWithLifecycle()

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
                message = errorMessage,
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
            onUsernameChanged = signupViewModel::onUsernameChanged,
            email = userEmail,
            isEmailValid = isEmailValid,
            onEmailChanged = signupViewModel::onUserEmailChanged,
            password = userPassword,
            isPasswordValid = isPasswordValid,
            onPasswordChanged = signupViewModel::onUserPasswordChanged,
            confirmPassword = confirmPassword,
            onConfirmPasswordChanged = signupViewModel::onConfirmPasswordChanged,
            doPasswordsMatch = doPasswordsMatch,
            arePasswordsVisible = arePasswordsVisible,
            onTogglePasswordVisibility = signupViewModel::togglePasswordsVisibility,
            onGoogleAuthenticate = {
                scope.launch { signupViewModel.onGoogleAuthenticate(context) }
            },
            onFacebookAuthenticate = {
                (context as? ComponentActivity)?.let { activity ->
                    scope.launch { signupViewModel.onFacebookAuthenticate(activity) }
                }
            },
            onLoginClick = onLoginNavigate,
            onSignupClick = { signupViewModel.validateThenRegister() },
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

## auth\ui\signup\ui\SignupUiState.kt

```kotlin
package com.mhq.fynecast.auth.ui.signup.ui

sealed interface SignupUiState {
    object Idle : SignupUiState
    object Loading : SignupUiState
    object Success : SignupUiState
    data class Error(val message: String) : SignupUiState
}
```

## auth\ui\signup\ui\SignupViewModel.kt

```kotlin
package com.mhq.fynecast.auth.ui.signup.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.auth.data.usecases.auth.AuthenticateWithSocialProviderUseCase
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.usecases.auth.RegisterWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateSignupFormUseCase
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SignupViewModel(
    private val validateSignupFormUseCase: ValidateSignupFormUseCase,
    private val registerWithEmailUseCase: RegisterWithEmailUseCase,
    private val authenticateWithSocialProviderUseCase: AuthenticateWithSocialProviderUseCase
) : ViewModel() {

    private val _signupUiState = MutableStateFlow<SignupUiState>(SignupUiState.Idle)
    private val _uiEventChannel = Channel<String>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()
    private val _navigationEvent = Channel<Unit>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()
    fun onUsernameChanged(newUsername: String) {
        _username.value = newUsername
    }

    val isUsernameValid = _username
        .map { validateSignupFormUseCase.validateUsername(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    private val _userEmail = MutableStateFlow("")
    val userEmail = _userEmail.asStateFlow()
    fun onUserEmailChanged(newEmail: String) {
        _userEmail.value = newEmail.trim()
    }

    val isEmailValid = _userEmail
        .map { validateSignupFormUseCase.validateEmail(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    private val _userPassword = MutableStateFlow("")
    val userPassword = _userPassword.asStateFlow()
    fun onUserPasswordChanged(newPassword: String) {
        _userPassword.value = newPassword
    }

    val isPasswordValid = _userPassword
        .map { validateSignupFormUseCase.validatePassword(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword = _confirmPassword.asStateFlow()
    fun onConfirmPasswordChanged(newConfirmedPassword: String) {
        _confirmPassword.value = newConfirmedPassword
    }

    private val _arePasswordsVisible = MutableStateFlow(false)
    val arePasswordsVisible = _arePasswordsVisible.asStateFlow()
    fun togglePasswordsVisibility() {
        _arePasswordsVisible.value = !_arePasswordsVisible.value
    }

    val doPasswordsMatch = combine(_userPassword, _confirmPassword) { password, confirmedPassword ->
        validateSignupFormUseCase.validatePasswordMatch(password, confirmedPassword)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    fun validateThenRegister() {
        val state = _signupUiState.value
        if (state is SignupUiState.Loading || state is SignupUiState.Success) return
        val validation = validateSignupFormUseCase(
            username = _username.value,
            email = _userEmail.value,
            password = _userPassword.value,
            confirmPassword = _confirmPassword.value
        )
        if (validation is ValidationResult.Failure) {
            viewModelScope.launch { _uiEventChannel.send(validation.errorMessage) }
            return
        }
        viewModelScope.launch {
            _signupUiState.value = SignupUiState.Loading
            registerWithEmailUseCase(
                username = _username.value,
                email = _userEmail.value,
                password = _userPassword.value
            )
                .onSuccess {
                    _signupUiState.value = SignupUiState.Success
                    _uiEventChannel.send("Account created successfully.")
                    delay(SUCCESS_MESSAGE_DISPLAY_DELAY_MS.milliseconds)
                    _navigationEvent.send(Unit)
                }
                .onFailure { error ->
                    val msg =
                        error.localizedMessage ?: "An unexpected authentication error occurred."
                    _signupUiState.value = SignupUiState.Error(msg)
                    _uiEventChannel.send(msg)
                }
        }
    }

    suspend fun onGoogleAuthenticate(context: Context) =
        runSocialSignIn("Google sign-in failed") {
            authenticateWithSocialProviderUseCase.withGoogle(context)
        }

    suspend fun onFacebookAuthenticate(activity: ComponentActivity) =
        runSocialSignIn("Facebook sign-in failed") {
            authenticateWithSocialProviderUseCase.withFacebook(activity)
        }

    private suspend fun runSocialSignIn(
        failureMessage: String,
        signIn: suspend () -> Result<UserDomainModel>
    ) {
        val state = _signupUiState.value
        if (state is SignupUiState.Loading || state is SignupUiState.Success) return
        _signupUiState.value = SignupUiState.Loading
        try {
            signIn()
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

        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)

                SignupViewModel(
                    validateSignupFormUseCase = app.container.validateSignupFormUseCase,
                    registerWithEmailUseCase = app.container.registerWithEmailUseCase,
                    authenticateWithSocialProviderUseCase = app.container.authenticateWithSocialProviderUseCase
                )
            }
        }
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

    override fun initializeSdkComponents() {
        // Handle standard global startup engines here if needed
        // e.g., FirebaseApp.initializeApp(application)
    }

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
import com.mhq.fynecast.auth.data.usecases.auth.AuthenticateWithSocialProviderUseCase
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
import com.mhq.fynecast.auth.domain.usecases.profile.UpdateUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ValidateEditProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.session.CheckUserLoggedInUseCase
import com.mhq.fynecast.auth.domain.usecases.session.LogoutUserUseCase
import com.mhq.fynecast.core.domain.infra.AppInitializer
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshFavoriteSnapshotUseCase
import com.mhq.fynecast.favorites.domain.usecases.RestoreFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.ToggleFavoriteUseCase
import com.mhq.fynecast.home.domain.repository.DeviceLocationRepository
import com.mhq.fynecast.home.domain.repository.LocationRepository
import com.mhq.fynecast.home.domain.usecases.GetCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.GetLastSearchedCityUseCase
import com.mhq.fynecast.home.domain.usecases.GetLocationSuggestionsUseCase
import com.mhq.fynecast.home.domain.usecases.GetWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.LoadCurrentWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentCityNameUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveFavoriteStatusUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveSignedInUserIdUseCase
import com.mhq.fynecast.map.domain.usecases.ReverseGeocodeLocationUseCase
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase

interface AppContainer {

    val appInitializer: AppInitializer

    // Core Repositories & Trackers
    val authRepository: AuthRepository
    val userProfileRepository: UserProfileRepository
    val imageStorageRepository: ImageStorageRepository
    val locationRepository: LocationRepository
    val favoritesRepository: FavoritesRepository

    val getWeatherUseCase: GetWeatherUseCase
    val loadCurrentWeatherUseCase: LoadCurrentWeatherUseCase
    val observeCurrentCityNameUseCase: ObserveCurrentCityNameUseCase
    val observeCurrentLocationUseCase: ObserveCurrentLocationUseCase

    // --- Authentication & Profile Use Cases ---
    val validateSignupFormUseCase: ValidateSignupFormUseCase
    val validateLoginFormUseCase: ValidateLoginFormUseCase
    val registerWithEmailUseCase: RegisterWithEmailUseCase
    val loginWithEmailUseCase: LoginWithEmailUseCase
    val authenticateWithSocialTokenUseCase: AuthenticateWithSocialTokenUseCase
    val authenticateWithSocialProviderUseCase: AuthenticateWithSocialProviderUseCase
    val completeAuthSessionUseCase: CompleteAuthSessionUseCase
    val sendPasswordResetUseCase: SendPasswordResetUseCase
    val updateUserProfileUseCase: UpdateUserProfileUseCase
    val validateEditProfileUseCase: ValidateEditProfileUseCase
    val observeUserProfileUseCase: ObserveUserProfileUseCase
    val observeSignedInUserIdUseCase: ObserveSignedInUserIdUseCase
    val userPreferencesRepository: UserPreferencesRepository
    val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase
    val updatePreferenceUseCase: UpdatePreferenceUseCase
    val logoutUserUseCase: LogoutUserUseCase
    val checkUserLoggedInUseCase: CheckUserLoggedInUseCase

    // Location Use Cases
    val deviceLocationRepository: DeviceLocationRepository
    val getCurrentLocationUseCase: GetCurrentLocationUseCase
    val getLocationSuggestionsUseCase: GetLocationSuggestionsUseCase
    val reverseGeocodeLocationUseCase: ReverseGeocodeLocationUseCase
    val getLastSearchedCityUseCase: GetLastSearchedCityUseCase

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
import com.mhq.fynecast.R
import com.mhq.fynecast.alerts.data.database.AlertsDatabase
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
import com.mhq.fynecast.auth.data.usecases.auth.AuthenticateWithSocialProviderUseCase
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
import com.mhq.fynecast.auth.domain.usecases.profile.UpdateUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ValidateEditProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.session.CheckUserLoggedInUseCase
import com.mhq.fynecast.auth.domain.usecases.session.LogoutUserUseCase
import com.mhq.fynecast.auth.ui.editprofile.ImageStorageManager
import com.mhq.fynecast.core.data.infra.AppInitializerImpl
import com.mhq.fynecast.core.domain.data.SystemTimeProvider
import com.mhq.fynecast.core.domain.data.infra.FirebaseCurrentUserProvider
import com.mhq.fynecast.core.domain.infra.AppInitializer
import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import com.mhq.fynecast.core.domain.time.TimeProvider
import com.mhq.fynecast.favorites.data.database.FavoritesDatabase
import com.mhq.fynecast.favorites.data.repoimpl.FavoritesRepoImpl
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshFavoriteSnapshotUseCase
import com.mhq.fynecast.favorites.domain.usecases.RestoreFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.ToggleFavoriteUseCase
import com.mhq.fynecast.home.data.api.location.LocationApiService
import com.mhq.fynecast.home.data.api.weather.WeatherApiService
import com.mhq.fynecast.home.data.infra.AndroidLocationStatusChecker
import com.mhq.fynecast.home.data.repoimpl.location.DefaultLocationRepoImpl
import com.mhq.fynecast.home.data.repoimpl.location.PhotonLocationRepoImpl
import com.mhq.fynecast.home.data.repoimpl.weather.InMemoryCurrentWeatherStore
import com.mhq.fynecast.home.data.repoimpl.weather.WeatherRepoImpl
import com.mhq.fynecast.home.domain.repository.CurrentWeatherStore
import com.mhq.fynecast.home.domain.repository.DeviceLocationRepository
import com.mhq.fynecast.home.domain.repository.LocationProviderStatusChecker
import com.mhq.fynecast.home.domain.repository.LocationRepository
import com.mhq.fynecast.home.domain.repository.WeatherRepository
import com.mhq.fynecast.home.domain.usecases.GetCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.GetLastSearchedCityUseCase
import com.mhq.fynecast.home.domain.usecases.GetLocationSuggestionsUseCase
import com.mhq.fynecast.home.domain.usecases.GetWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.LoadCurrentWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentCityNameUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveFavoriteStatusUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveSignedInUserIdUseCase
import com.mhq.fynecast.map.domain.usecases.ReverseGeocodeLocationUseCase
import com.mhq.fynecast.settings.data.repoimpl.UserPreferencesRepositoryImpl
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

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

    // Networking — Weather
    private val weatherApiBaseUrl = "https://api.weatherapi.com/"
    private val weatherApiJson =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
    private val weatherApiRetrofit = Retrofit.Builder()
        .addConverterFactory(weatherApiJson.asConverterFactory("application/json".toMediaType()))
        .baseUrl(weatherApiBaseUrl)
        .build()
    private val weatherApiRetrofitService: WeatherApiService by lazy {
        weatherApiRetrofit.create(WeatherApiService::class.java)
    }
    private val weatherRepository: WeatherRepository by lazy {
        WeatherRepoImpl(weatherApiService = weatherApiRetrofitService)
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
    override val observeCurrentCityNameUseCase by lazy {
        ObserveCurrentCityNameUseCase(currentWeatherStore)
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
    private val photonRetrofit: Retrofit = Retrofit.Builder()
        .addConverterFactory(photonJson.asConverterFactory("application/json".toMediaType()))
        .baseUrl(photonBaseUrl)
        .build()
    private val photonApiRetrofitService: LocationApiService by lazy {
        photonRetrofit.create(LocationApiService::class.java)
    }


    // Repositories
    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(firebaseAuth, sessionDataStore, socialAuthCredentialProvider)
    }
    override val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepositoryImpl(firebaseAuth, sessionDataStore, applicationScope)
    }
    override val imageStorageRepository: ImageStorageRepository by lazy {
        ImageStorageRepositoryImpl(context, imageStorageManager)
    }


    // Location Tracker
    private val locationProviderStatusChecker: LocationProviderStatusChecker by lazy {
        AndroidLocationStatusChecker(appContext = context)
    }
    override val deviceLocationRepository: DeviceLocationRepository by lazy {
        DefaultLocationRepoImpl(
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context),
            statusChecker = locationProviderStatusChecker
        )
    }
    override val locationRepository: LocationRepository by lazy {
        PhotonLocationRepoImpl(photonApiRetrofitService)
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
        SocialAuthCredentialProviderImpl(
            serverClientId = context.getString(R.string.default_web_client_id),
            appContext = context
        )
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
    override val authenticateWithSocialProviderUseCase: AuthenticateWithSocialProviderUseCase by lazy {
        AuthenticateWithSocialProviderUseCase(
            socialAuthCredentialProvider = socialAuthCredentialProvider,
            authenticateWithSocialTokenUseCase = authenticateWithSocialTokenUseCase
        )
    }
    override val sendPasswordResetUseCase by lazy {
        SendPasswordResetUseCase(authRepository, validateLoginFormUseCase)
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
    override val observeSignedInUserIdUseCase by lazy {
        ObserveSignedInUserIdUseCase(
            currentUserProvider
        )
    }
    override val updateUserProfileUseCase by lazy {
        UpdateUserProfileUseCase(
            userProfileRepository,
            imageStorageRepository
        )
    }
    override val checkUserLoggedInUseCase: CheckUserLoggedInUseCase by lazy {
        CheckUserLoggedInUseCase(authRepository)
    }
    override val logoutUserUseCase by lazy {
        LogoutUserUseCase(
            authRepository,
            userProfileRepository,
            cancelAllAlertTrackingUseCase,
            updatePreferenceUseCase
        )
    }

    // Preferences Use Cases
    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepositoryImpl(prefsDataStore)
    }
    override val observeUserPreferencesUseCase by lazy {
        ObserveUserPreferencesUseCase(userPreferencesRepository)
    }
    override val updatePreferenceUseCase by lazy {
        UpdatePreferenceUseCase(userPreferencesRepository)
    }


    // Favorites Use Cases
    override val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepoImpl(favoritesDatabase.favoriteDao(), currentUserProvider)
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
    override val restoreFavoriteUseCase: RestoreFavoriteUseCase by lazy {
        RestoreFavoriteUseCase(favoritesRepository)
    }

    // Time
    private val timeProvider: TimeProvider by lazy { SystemTimeProvider() }

    // Alerts
    private val alertTrackingController: AlertTrackingController by lazy {
        AlertTrackingControllerImpl(context)
    }
    private val alertsDatabase: AlertsDatabase by lazy {
        Room.databaseBuilder(context, AlertsDatabase::class.java, "alerts_db").build()
    }
    private val alertsRepository: AlertsRepository by lazy {
        AlertsRepoImpl(alertDao = alertsDatabase.alertDao())
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
            updatePreferenceUseCase = updatePreferenceUseCase,
            notificationPermissionChecker = notificationPermissionChecker,
            cancelAllAlertTrackingUseCase = cancelAllAlertTrackingUseCase
        )
    }
    override val handleNotificationPermissionResultUseCase by lazy {
        HandleNotificationPermissionResultUseCase(updatePreferenceUseCase)
    }
    override val syncNotificationPermissionUseCase by lazy {
        SyncNotificationPermissionUseCase(
            observeUserPreferencesUseCase = observeUserPreferencesUseCase,
            updatePreferenceUseCase = updatePreferenceUseCase,
            notificationPermissionChecker = notificationPermissionChecker,
            cancelAllAlertTrackingUseCase = cancelAllAlertTrackingUseCase
        )
    }
    override val consumeNotificationPromptUseCase by lazy {
        ConsumeNotificationPromptUseCase(notificationPermissionChecker, userPreferencesRepository)
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
                    "Active Weather Alerts",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows while you are tracking a weather alert"
                    setSound(null, null)
                    enableVibration(false)
                },

                // 2. High-priority critical emergency alarm channel
                NotificationChannel(
                    CHANNEL_ALERT_ALARM,
                    "Weather Alert Alarm",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Sounds when a tracked weather alert expires or escalates"
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

## core\domain\data\infra\FirebaseCurrentUserProvider.kt

```kotlin
package com.mhq.fynecast.core.domain.data.infra

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

## core\domain\data\SystemTimeProvider.kt

```kotlin
package com.mhq.fynecast.core.domain.data

import com.mhq.fynecast.core.domain.time.TimeProvider

class SystemTimeProvider : TimeProvider {
    override fun nowMillis() = System.currentTimeMillis()
}
```

## core\domain\infra\AppInitializer.kt

```kotlin
package com.mhq.fynecast.core.domain.infra

interface AppInitializer {
    fun initializeSdkComponents()
    fun initializeDebugFeatures()
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

## core\domain\time\TimeProvider.kt

```kotlin
package com.mhq.fynecast.core.domain.time

interface TimeProvider { fun nowMillis(): Long }
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
    onItemSelected: (BottomNavigationItem) -> Unit
) {
    val navigationItems = listOf(
        BottomNavigationItem.Home,
        BottomNavigationItem.Favorites,
        BottomNavigationItem.Alerts,
        BottomNavigationItem.Settings
    )

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
            navigationItems.forEach { screen ->
                val isSelected = currentRoute == screen.route

                NavigationBarItem(
                    selected = isSelected,
                    label = {
                        screen.titleRes?.let { resId ->
                            Text(
                                text = stringResource(id = resId),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    icon = {
                        screen.icon?.let {
                            Icon(
                                it,
                                contentDescription = null
                            )
                        }
                    },
                    onClick = { onItemSelected(screen) },
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

## core\navigation\BottomNavigationItem.kt

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

sealed class BottomNavigationItem(
    val route: String,
    @StringRes val titleRes: Int? = null,
    val icon: ImageVector? = null
) {
    object Signup : BottomNavigationItem("signup")
    object Login : BottomNavigationItem("login")
    object ForgotPassword : BottomNavigationItem("forgot_password")
    object EditProfile : BottomNavigationItem("edit_profile")

    object Home : BottomNavigationItem("home", R.string.home, Icons.Default.Home)
    object Favorites : BottomNavigationItem("favorites", R.string.favorites, Icons.Default.Favorite)
    object Alerts : BottomNavigationItem("alerts", R.string.alerts, Icons.Default.Notifications)
    object Settings : BottomNavigationItem("settings", R.string.settings, Icons.Default.Settings)
}
```

## core\navigation\SessionViewModel.kt

```kotlin
package com.mhq.fynecast.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.auth.domain.usecases.session.CheckUserLoggedInUseCase
import com.mhq.fynecast.core.di.FyneCastApplication

class SessionViewModel(
    checkUserLoggedIn: CheckUserLoggedInUseCase
) : ViewModel() {
    val startDestination: String =
        if (checkUserLoggedIn())
            BottomNavigationItem.Home.route
        else
            BottomNavigationItem.Signup.route

    companion object {
        val factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)
                SessionViewModel(
                    checkUserLoggedIn = app.container.checkUserLoggedInUseCase
                )
            }
        }
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

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mhq.fynecast.alerts.ui.AlertsViewModel
import com.mhq.fynecast.alerts.ui.screens.AlertsContainer
import com.mhq.fynecast.auth.ui.editprofile.ui.EditProfileViewModel
import com.mhq.fynecast.auth.ui.editprofile.ui.screens.EditProfileContainer
import com.mhq.fynecast.auth.ui.forgotpassword.ui.screens.ForgotPasswordContainer
import com.mhq.fynecast.auth.ui.login.ui.screens.LoginContainer
import com.mhq.fynecast.auth.ui.signup.ui.SignupViewModel
import com.mhq.fynecast.auth.ui.signup.ui.screens.SignupContainer
import com.mhq.fynecast.core.navigation.BottomNavigationBar
import com.mhq.fynecast.core.navigation.BottomNavigationItem
import com.mhq.fynecast.core.navigation.SessionViewModel
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.favorites.ui.FavoritesViewModel
import com.mhq.fynecast.favorites.ui.screens.FavoritesContainer
import com.mhq.fynecast.home.ui.HomeUiState
import com.mhq.fynecast.home.ui.HomeViewModel
import com.mhq.fynecast.home.ui.screens.HomeContainer
import com.mhq.fynecast.map.ui.MapPickerViewModel
import com.mhq.fynecast.map.ui.screens.MapPickerScreen
import com.mhq.fynecast.settings.domain.models.AppLanguage
import com.mhq.fynecast.settings.ui.SettingsViewModel
import com.mhq.fynecast.settings.ui.screens.SettingsContainer

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun FyneCastApp(
    homeViewModel: HomeViewModel,
    alertsViewModel: AlertsViewModel,
    settingsViewModel: SettingsViewModel,
    sessionViewModel: SessionViewModel,
    onRequestPermission: (String) -> Unit
) {

    val context = LocalContext.current
    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
    val isMetric by settingsViewModel.isMetric.collectAsState()
    val currentLanguage by settingsViewModel.currentLanguage.collectAsState()
    val appLangCode = AppLanguage.fromStoredName(currentLanguage).code

    LaunchedEffect(appLangCode) {
        homeViewModel.onLanguageChanged(appLangCode)
    }

    FyneCastTheme(isDarkMode = isDarkMode) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val homeUiState = homeViewModel.state.collectAsState().value.uiState

        val rootRoutes = listOf(
            BottomNavigationItem.Home.route,
            BottomNavigationItem.Favorites.route,
            BottomNavigationItem.Alerts.route,
            BottomNavigationItem.Settings.route
        )

        val shouldShowBar = if (currentRoute == BottomNavigationItem.Home.route) {
            homeUiState !is HomeUiState.Loading
        } else {
            currentRoute in rootRoutes
        }

        val bottomContentPadding = if (shouldShowBar) {
            PaddingValues(bottom = 120.dp)
        } else {
            PaddingValues(0.dp)
        }

        BackHandler(enabled = currentRoute in rootRoutes) {
            if (currentRoute == BottomNavigationItem.Home.route) {
                (context as? Activity)?.finish()
            } else {
                navController.navigate(BottomNavigationItem.Home.route) {
                    popUpTo(BottomNavigationItem.Home.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
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
                    composable(BottomNavigationItem.Signup.route) {
                        val signupViewModel: SignupViewModel =
                            viewModel(factory = SignupViewModel.factory)
                        SignupContainer(
                            onLoginNavigate = { navController.goToAuthScreen(BottomNavigationItem.Login.route) },
                            onSignupClick = { navController.enterApp() },
                            modifier = Modifier,
                            signupViewModel = signupViewModel
                        )
                    }

                    composable(BottomNavigationItem.Login.route) {
                        LoginContainer(
                            onSignUpNavigate = { navController.goToAuthScreen(BottomNavigationItem.Signup.route) },
                            onLoginClick = { navController.enterApp() },
                            onForgotPasswordClick = {
                                navController.goToAuthScreen(
                                    BottomNavigationItem.ForgotPassword.route
                                )
                            },
                            modifier = Modifier
                        )
                    }

                    composable(BottomNavigationItem.ForgotPassword.route) {
                        ForgotPasswordContainer(
                            onBackToLoginClick = { navController.goToAuthScreen(BottomNavigationItem.Login.route) },
                            onSendLinkSuccessNavigate = {
                                navController.goToAuthScreen(
                                    BottomNavigationItem.Login.route
                                )
                            },
                            modifier = Modifier
                        )
                    }

                    composable(BottomNavigationItem.EditProfile.route) {
                        val editProfileViewModel: EditProfileViewModel =
                            viewModel(factory = EditProfileViewModel.factory)
                        EditProfileContainer(
                            onBackClick = { navController.popBackStack() },
                            contentPadding = bottomContentPadding,
                            editProfileViewModel = editProfileViewModel
                        )
                    }

                    composable(BottomNavigationItem.Home.route) {
                        val isMetric by settingsViewModel.isMetric.collectAsState()
                        HomeContainer(
                            homeViewModel = homeViewModel,
                            onNavigateToMap = { navController.navigate("map_picker") },
                            onAlertsBannerClick = { navController.navigate(BottomNavigationItem.Alerts.route) },
                            onRequestPermission = onRequestPermission,
                            contentPadding = bottomContentPadding,
                            isMetric = isMetric,
                            langCode = appLangCode
                        )
                    }

                    composable(BottomNavigationItem.Favorites.route) {
                        val favoritesViewModel: FavoritesViewModel =
                            viewModel(factory = FavoritesViewModel.factory)

                        FavoritesContainer(
                            favoritesViewModel = favoritesViewModel,
                            onCityRowSelected = { favoriteCity ->
                                homeViewModel.onCitySelected(
                                    suggestionName = "${favoriteCity.cityName}, ${favoriteCity.countryName}",
                                    lat = favoriteCity.latitude,
                                    lon = favoriteCity.longitude,
                                    langCode = appLangCode
                                )
                                navController.navigate(BottomNavigationItem.Home.route) {
                                    popUpTo(BottomNavigationItem.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            contentPadding = bottomContentPadding,
                            isMetric = isMetric
                        )
                    }

                    composable(BottomNavigationItem.Alerts.route) {
                        AlertsContainer(
                            contentPadding = bottomContentPadding,
                            alertsViewModel = alertsViewModel
                        )
                    }

                    composable(BottomNavigationItem.Settings.route) {
                        SettingsContainer(
                            settingsViewModel = settingsViewModel,
                            contentPadding = bottomContentPadding,
                            onEditProfileClick = {
                                navController.navigate(BottomNavigationItem.EditProfile.route)
                            },
                            onLogoutClick = {
                                navController.navigate(BottomNavigationItem.Signup.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable("map_picker") {
                        val mapLangCode = AppLanguage.fromStoredName(currentLanguage).code
                        val mapPickerViewModel: MapPickerViewModel =
                            viewModel(factory = MapPickerViewModel.factory)

                        MapPickerScreen(
                            mapPickerViewModel = mapPickerViewModel,
                            langCode = mapLangCode,
                            onLocationPicked = { location ->
                                homeViewModel.onCitySelected(
                                    suggestionName = location.fullName,
                                    lat = location.latitude,
                                    lon = location.longitude,
                                    langCode = mapLangCode
                                )
                                if (navController.currentBackStackEntry?.destination?.route == "map_picker") {
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
                    onItemSelected = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
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

private fun NavController.enterApp() {
    navigate(BottomNavigationItem.Home.route) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
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
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mhq.fynecast.alerts.ui.AlertsViewModel
import com.mhq.fynecast.core.navigation.SessionViewModel
import com.mhq.fynecast.core.ui.globalcomponents.FyneCastApp
import com.mhq.fynecast.home.ui.HomeViewModel
import com.mhq.fynecast.settings.ui.SettingsViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val homeViewModel: HomeViewModel by lazy {
        ViewModelProvider(this, HomeViewModel.factory)[HomeViewModel::class.java]
    }

    private val alertsViewModel: AlertsViewModel by lazy {
        ViewModelProvider(this, AlertsViewModel.factory)[AlertsViewModel::class.java]
    }

    private val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProvider(this, SettingsViewModel.factory)[SettingsViewModel::class.java]
    }

    private val sessionViewModel: SessionViewModel by lazy {
        ViewModelProvider(this, SessionViewModel.factory)[SessionViewModel::class.java]
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        settingsViewModel.onNotificationPermissionResult(granted)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { false }

        lifecycleScope.launch {
            settingsViewModel.localeEvent.collect { langCode ->
                if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != langCode) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))
                }
            }
        }

        lifecycleScope.launch {
            settingsViewModel.requestNotificationPermissionEvent.collect {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val currentLangTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "en" }
        homeViewModel.refreshCurrentCity(langCode = currentLangTag)
        settingsViewModel.syncNotificationPermissionState()
    }
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

import androidx.paging.PagingSource
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

    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY cityName ASC, countryName ASC")
    abstract fun getAllFavorites(userId: String): PagingSource<Int, FavoriteEntity>

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

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import com.mhq.fynecast.favorites.data.database.FavoriteDao
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
    private val favoriteDao: FavoriteDao,
    private val currentUserProvider: CurrentUserProvider
) : FavoritesRepository {

    override fun observeIsFavorite(id: String): Flow<Boolean> =
        currentUserProvider.observeUserId().flatMapLatest { userId ->
            if (userId == null)
                flowOf(false)
            else
                favoriteDao.observeIsFavorite(userId, id)
        }

    override suspend fun toggleFavorite(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        favoriteDao.toggle(city.toEntity(userId))
    }

    override suspend fun saveFavorite(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        favoriteDao.insertFavorite(city.toEntity(userId))
    }

    override suspend fun deleteFavorite(id: String) {
        val userId = currentUserProvider.currentUserId() ?: return
        favoriteDao.deleteFavorite(userId, id)
    }

    override suspend fun refreshSnapshot(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        favoriteDao.updateSnapshot(
            userId,
            city.id,
            city.temperatureC,
            city.temperatureF,
            city.weatherIconUrl
        )
    }

    override fun getAllFavorites(): Flow<PagingData<FavoriteCityDomainModel>> =
        currentUserProvider.observeUserId().flatMapLatest { userId ->
            if (userId == null) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    pagingSourceFactory = { favoriteDao.getAllFavorites(userId) }
                ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
            }
        }
}
```

## favorites\domain\mapper\FavoritesMapper.kt

```kotlin
package com.mhq.fynecast.favorites.domain.mapper

import com.mhq.fynecast.favorites.data.database.FavoriteEntity
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel

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

import androidx.paging.PagingData
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getAllFavorites(): Flow<PagingData<FavoriteCityDomainModel>>
    fun observeIsFavorite(id: String): Flow<Boolean>
    /** Adds the city if it isn't a favorite and removes it if it is, as one atomic step. */
    suspend fun toggleFavorite(city: FavoriteCityDomainModel)
    suspend fun saveFavorite(city: FavoriteCityDomainModel)            // used to undo a removal
    suspend fun deleteFavorite(id: String)
    suspend fun refreshSnapshot(city: FavoriteCityDomainModel)         // updates temperature and icon
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

import androidx.paging.PagingData
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow

class GetFavoritesUseCase(private val favoritesRepository: FavoritesRepository) {
    operator fun invoke(): Flow<PagingData<FavoriteCityDomainModel>> {
        return favoritesRepository.getAllFavorites()
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

## favorites\ui\components\FavoriteCityItem.kt

```kotlin
package com.mhq.fynecast.favorites.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel

@Composable
fun FavoriteCityItem(
    favoriteCityDomainModel: FavoriteCityDomainModel,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    GlassyCard(
        modifier = modifier.padding(horizontal = 8.dp),
        containerAlpha = 0.12f,
        borderAlpha = 0.3f
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left — City info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = favoriteCityDomainModel.cityName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Cursive,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = favoriteCityDomainModel.countryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right — Weather icon + temp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AsyncImage(
                    model = "https:${favoriteCityDomainModel.weatherIconUrl}",
                    placeholder = painterResource(R.drawable.ic_launcher_foreground),
                    error = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.weather_icon),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text =
                        if (isMetric)
                            "${favoriteCityDomainModel.temperatureC}°"
                        else
                            "${favoriteCityDomainModel.temperatureF}°",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
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
private fun FavoriteCityItemPreview() {
    FyneCastTheme() {
        FavoriteCityItem(
            FavoriteCityDomainModel(
                cityName = "Alexandria",
                countryName = "Egypt",
                latitude = 100.0,
                longitude = 100.0,
                temperatureC = 100.0,
                temperatureF = 100.0,
                weatherIconUrl = "Url"
            ),
            isMetric = true
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
    val color = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = Color.White
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

## favorites\ui\FavoritesEvent.kt

```kotlin
package com.mhq.fynecast.favorites.ui

sealed interface FavoritesEvent {
    data class Removed(val cityName: String) : FavoritesEvent
    data class RemoveFailed(val cityName: String) : FavoritesEvent
}
```

## favorites\ui\FavoritesViewModel.kt

```kotlin
package com.mhq.fynecast.favorites.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RestoreFavoriteUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val deleteFavoriteUseCase: DeleteFavoriteUseCase,
    private val restoreFavoriteUseCase: RestoreFavoriteUseCase
) : ViewModel() {

    private val _uiEventChannel = Channel<String>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()

    val favoritesPageFlow: Flow<PagingData<FavoriteCityDomainModel>> =
        getFavoritesUseCase().cachedIn(viewModelScope)

    private val _events = Channel<FavoritesEvent>(Channel.BUFFERED)
    val events: Flow<FavoritesEvent> = _events.receiveAsFlow()

    private var lastRemoved: FavoriteCityDomainModel? = null

    fun onDeleteFavorite(city: FavoriteCityDomainModel) {
        viewModelScope.launch {
            try {
                // One quick DB call; it must finish even if the screen is closed right after.
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

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)

                FavoritesViewModel(
                    getFavoritesUseCase = app.container.getFavoritesUseCase,
                    deleteFavoriteUseCase = app.container.deleteFavoriteUseCase,
                    restoreFavoriteUseCase = app.container.restoreFavoriteUseCase
                )
            }
        }
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mhq.fynecast.R
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.ui.FavoritesEvent
import com.mhq.fynecast.favorites.ui.FavoritesViewModel
import com.mhq.fynecast.favorites.ui.components.UndoDeleteSnackbar
import kotlinx.coroutines.launch

@Composable
fun FavoritesContainer(
    favoritesViewModel: FavoritesViewModel = viewModel(factory = FavoritesViewModel.factory),
    onCityRowSelected: (FavoriteCityDomainModel) -> Unit,
    isMetric: Boolean,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val undoLabel = stringResource(R.string.undo)
    val lazyFavoriteCities = favoritesViewModel.favoritesPageFlow.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val cityRemovedTemplate = stringResource(R.string.city_removed)

    LaunchedEffect(Unit) {
        favoritesViewModel.uiEvent.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
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
        when {
            lazyFavoriteCities.loadState.refresh is LoadState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            lazyFavoriteCities.itemCount == 0 -> {
                FavoritesBlank(
                    contentPadding = contentPadding
                )
            }

            else -> {
                FavoritesContent(
                    favoriteCities = lazyFavoriteCities,
                    onCityRowSelected = onCityRowSelected,
                    onDeleteFavorite = favoritesViewModel::onDeleteFavorite,
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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.ui.components.FavoriteCityItem
import com.mhq.fynecast.favorites.ui.components.SwipeBackground
import kotlinx.coroutines.flow.flowOf

@Composable
fun FavoritesContent(
    favoriteCities: LazyPagingItems<FavoriteCityDomainModel>,
    onCityRowSelected: (FavoriteCityDomainModel) -> Unit,
    onDeleteFavorite: (FavoriteCityDomainModel) -> Unit,
    isMetric: Boolean,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(vertical = 48.dp, horizontal = 24.dp)
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                count = favoriteCities.itemCount,
                key = favoriteCities.itemKey { it.id }
            ) { index ->
                val favorite = favoriteCities[index]
                if (favorite != null) {
                    val currentFavorite by rememberUpdatedState(favorite)
                    val currentOnDelete by rememberUpdatedState(onDeleteFavorite)
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value -> value == SwipeToDismissBoxValue.EndToStart }
                    )

                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            currentOnDelete(currentFavorite)
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = { SwipeBackground(dismissState) },
                        enableDismissFromStartToEnd = false
                    ) {
                        FavoriteCityItem(
                            favoriteCityDomainModel = favorite,
                            isMetric = isMetric,
                            modifier = Modifier.clickable { onCityRowSelected(favorite) }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun FavoritesContentPreview() {
    FyneCastTheme() {
        val mockFavoriteCities = flowOf(
            PagingData.empty<FavoriteCityDomainModel>()
        ).collectAsLazyPagingItems()

        FavoritesContent(
            favoriteCities = mockFavoriteCities,
            onCityRowSelected = {},
            onDeleteFavorite = {},
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
    val cityLocaltime: String
)
```

## home\data\api\weather\dto\WeatherApiResponse.kt

```kotlin
package com.mhq.fynecast.home.data.api.weather.dto

import com.mhq.fynecast.alerts.data.dto.AlertsDto
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

## home\data\mapper\WeatherMapper.kt

```kotlin
package com.mhq.fynecast.home.data.mapper

import com.mhq.fynecast.alerts.data.mapper.toDomain
import com.mhq.fynecast.home.data.api.weather.dto.WeatherApiResponse
import com.mhq.fynecast.home.domain.models.DailyForecastDomainModel
import com.mhq.fynecast.home.domain.models.HourlyForecastDomainModel
import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.domain.models.WeatherStatsDomainModel
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

import android.annotation.SuppressLint
import android.location.Location
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mhq.fynecast.home.domain.models.GeoCoordinates
import com.mhq.fynecast.home.domain.repository.DeviceLocationRepository
import com.mhq.fynecast.home.domain.repository.LocationProviderStatusChecker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

class DefaultLocationRepoImpl(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val statusChecker: LocationProviderStatusChecker
) : DeviceLocationRepository {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): GeoCoordinates? {
        if (!statusChecker.hasLocationPermissions() || !statusChecker.isLocationEnabled()) return null

        val location = try {
            withTimeoutOrNull(FRESH_FIX_TIMEOUT_MILLIS.milliseconds) { requestFreshLocation() }
                ?: lastKnownLocation()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null   // e.g. permission revoked between the check and the request
        }

        return location?.let { GeoCoordinates(latitude = it.latitude, longitude = it.longitude) }
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
        const val FRESH_FIX_TIMEOUT_MILLIS = 8_000L
        const val MAX_CACHED_FIX_AGE_MILLIS = 30 * 60 * 1000L
    }

}
```

## home\data\repoimpl\location\PhotonLocationRepoImpl.kt

```kotlin
package com.mhq.fynecast.home.data.repoimpl.location

import com.mhq.fynecast.home.data.api.location.LocationApiService
import com.mhq.fynecast.home.domain.models.CitySuggestionModel
import com.mhq.fynecast.home.domain.repository.LocationRepository
import kotlinx.coroutines.CancellationException

class PhotonLocationRepoImpl(
    private val locationApiService: LocationApiService
) : LocationRepository {

    override suspend fun getCitySuggestions(query: String, lang: String): List<CitySuggestionModel> {
        return try {
            val response = locationApiService.getSuggestions(query = query, lang = lang)
            response.features.map { feature ->
                val props = feature.properties
                val nameParts = listOfNotNull(props.name, props.country).distinct().filter { it.isNotBlank() }
                val coordinates = feature.geometry.coordinates
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

    override suspend fun getCityNameOffMap(lat: Double, lon: Double, lang: String): CitySuggestionModel? {
        return try {
            val response = locationApiService.reverseGeocode(lat = lat, lon = lon, lang = lang)

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
}
```

## home\data\repoimpl\weather\InMemoryCurrentWeatherStore.kt

```kotlin
package com.mhq.fynecast.home.data.repoimpl.weather

import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.domain.repository.CurrentWeatherStore
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

import com.mhq.fynecast.BuildConfig
import com.mhq.fynecast.home.data.api.weather.WeatherApiService
import com.mhq.fynecast.home.data.mapper.toDomain
import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.domain.repository.WeatherRepository

class WeatherRepoImpl(
    private val weatherApiService: WeatherApiService
) : WeatherRepository {
    override suspend fun fetchWeather(query: String, lang: String): WeatherDomainModel =
        weatherApiService.getWeatherData(
            key = BuildConfig.WEATHER_API_KEY,
            city = query,
            languageCode = lang
        ).toDomain()
}
```

## home\domain\mapper\WeatherToFavoriteMapper.kt

```kotlin
package com.mhq.fynecast.home.domain.mapper

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.home.domain.models.WeatherDomainModel

fun WeatherDomainModel.toFavoriteCity() = FavoriteCityDomainModel(
    cityName = cityName,
    countryName = country,
    latitude = latitude,
    longitude = longitude,
    temperatureC = currentTempC,
    temperatureF = currentTempF,
    weatherIconUrl = conditionIcon
)
```

## home\domain\models\CitySuggestionModel.kt

```kotlin
package com.mhq.fynecast.home.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class CitySuggestionModel(
    val fullName: String,
    val latitude: Double,
    val longitude: Double
)
```

## home\domain\models\DailyForecastDomainModel.kt

```kotlin
package com.mhq.fynecast.home.domain.models

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

## home\domain\models\GeoCoordinates.kt

```kotlin
package com.mhq.fynecast.home.domain.models

data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double
)
```

## home\domain\models\HourlyForecastDomainModel.kt

```kotlin
package com.mhq.fynecast.home.domain.models

import java.time.LocalDateTime

data class HourlyForecastDomainModel(
    val time: LocalDateTime,
    val iconUrl: String,
    val tempC: Double,
    val tempF: Double
)
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

## home\domain\models\WeatherDomainModel.kt

```kotlin
package com.mhq.fynecast.home.domain.models

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
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
    val alerts: List<AlertDomainModel>,
    val stats: WeatherStatsDomainModel,
    val dailyForecasts: List<DailyForecastDomainModel>,
    val hourlyForecasts: List<HourlyForecastDomainModel>
)
```

## home\domain\models\WeatherStatsDomainModel.kt

```kotlin
package com.mhq.fynecast.home.domain.models

data class WeatherStatsDomainModel(
    val humidity: Int,
    val windKph: Double,
    val windMph: Double,
    val pressureMb: Double,
    val pressureIn: Double,
    val uvIndex: Double
)
```

## home\domain\repository\CurrentWeatherStore.kt

```kotlin
package com.mhq.fynecast.home.domain.repository

import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import kotlinx.coroutines.flow.StateFlow

interface CurrentWeatherStore {
    val current: StateFlow<WeatherDomainModel?>
    fun publish(weather: WeatherDomainModel)
}
```

## home\domain\repository\DeviceLocationRepository.kt

```kotlin
package com.mhq.fynecast.home.domain.repository

import com.mhq.fynecast.home.domain.models.GeoCoordinates

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

## home\domain\repository\LocationRepository.kt

```kotlin
package com.mhq.fynecast.home.domain.repository

import com.mhq.fynecast.home.domain.models.CitySuggestionModel

interface LocationRepository {
    suspend fun getCitySuggestions(query: String, lang: String): List<CitySuggestionModel>
    suspend fun getCityNameOffMap(lat: Double, lon: Double, lang: String): CitySuggestionModel?
}
```

## home\domain\repository\WeatherRepository.kt

```kotlin
package com.mhq.fynecast.home.domain.repository

import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import kotlinx.coroutines.flow.StateFlow

interface WeatherRepository {
    suspend fun fetchWeather(query: String, lang: String): WeatherDomainModel
}
```

## home\domain\usecases\GetCurrentLocationUseCase.kt

```kotlin
package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.home.domain.models.GeoCoordinates
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

import com.mhq.fynecast.home.domain.models.CitySuggestionModel
import com.mhq.fynecast.home.domain.repository.LocationRepository

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

import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.domain.repository.WeatherRepository

class GetWeatherUseCase(private val weatherRepository: WeatherRepository) {
    suspend operator fun invoke(query: String, lang: String): WeatherDomainModel =
        weatherRepository.fetchWeather(query, lang)
}
```

## home\domain\usecases\LoadCurrentWeatherUseCase.kt

```kotlin
package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.domain.repository.CurrentWeatherStore
import com.mhq.fynecast.home.domain.repository.WeatherRepository

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

## home\domain\usecases\ObserveCurrentCityNameUseCase.kt

```kotlin
package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.home.domain.repository.CurrentWeatherStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCurrentCityNameUseCase(private val currentWeatherStore: CurrentWeatherStore) {
    operator fun invoke(): Flow<String> =
        currentWeatherStore.current.map { it?.cityName ?: "Unknown Location" }
}
```

## home\domain\usecases\ObserveCurrentLocationUseCase.kt

```kotlin
package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.home.domain.repository.CurrentWeatherStore
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

class ObserveFavoriteStatusUseCase(private val favoritesRepository: FavoritesRepository) {
    operator fun invoke(latitude: Double, longitude: Double): Flow<Boolean> =
        favoritesRepository.observeIsFavorite(favoriteId(latitude, longitude))
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

## home\ui\components\WeatherDailyCarousel.kt

```kotlin
package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.home.domain.models.DailyForecastDomainModel

@Composable
fun WeatherDailyCarousel(
    dailyForecasts: List<DailyForecastDomainModel>,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    val locale = currentLocale()
    val weekMin = dailyForecasts.minOfOrNull { if (isMetric) it.minTempC else it.minTempF } ?: 0.0
    val weekMax = dailyForecasts.maxOfOrNull { if (isMetric) it.maxTempC else it.maxTempF } ?: 0.0

    GlassyCard(
        cornerRadius = 28.dp,
        padding = 20.dp,
        containerAlpha = 0.08f,
        borderAlpha = 0.15f,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.daily_forecast),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                dailyForecasts.forEachIndexed { index, item ->
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

                    // Prevent drawing an isolated extra separator line at the final item index boundary
                    if (index < dailyForecasts.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
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
private fun WeatherDailyCarouselPreview() {
    FyneCastTheme() {
        WeatherDailyCarousel(
            dailyForecasts = emptyList(),
            isMetric = true
        )
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
import coil3.compose.AsyncImage
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import kotlin.math.roundToInt

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
        // Left Column: Day Descriptor Bounds
        Column(
            modifier = Modifier.width(70.dp), //85
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = day,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = date,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
            )
        }

        // Center Content Element: Weather state icon representation map
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            AsyncImage(
                model = "https:$weatherIconUrl",
                contentDescription = weatherDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(36.dp)
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

        // Right Content Block: High/Low relative tracking balance parameters
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.width(130.dp) //90
        ) {
            Text(
                text = "${lowTemperature.roundToInt()}°",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.End,
                modifier = Modifier.width(32.dp)
            )

            // Premium ambient bar slider container element mimicking relative scaling layout structures
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
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
                text = "${highTemperature.roundToInt()}°",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.End,
                modifier = Modifier.width(32.dp)
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
private fun WeatherDailyRowItemPreview() {
    FyneCastTheme() {
        WeatherDailyRowItem(
            day = "",
            date = "",
            weekMin = 10.0,
            weekMax = 10.0,
            weatherIconUrl = "",
            weatherDescription = "",
            highTemperature = 10.0,
            lowTemperature = 10.0,
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun WeatherHeroCard(
    icon: String,
    city: String,
    timestamp: String,
    description: String,
    temperature: String,
    isFavorite: Boolean,
    alertsNumber: Int,
    onToggleFavorite: () -> Unit,
    onAlertsBannerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    GlassyCard(
        cornerRadius = 32.dp,
        padding = 24.dp,
        containerAlpha = 0.06f,
        borderAlpha = 0.15f,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = city,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = timestamp.uppercase(),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                LikeButton(
                    isLiked = isFavorite,
                    onLikeChanged = onToggleFavorite
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = temperature,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 76.sp,
                        fontWeight = FontWeight.W200,
                        lineHeight = 76.sp,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        text = description.uppercase(),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "https:$icon",
                        contentDescription = description,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(68.dp)
                    )
                }
            }

            if (alertsNumber > 0) {
                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onAlertsBannerClick() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.warning),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    Text(
                        text = pluralStringResource(
                            R.plurals.active_weather_warnings,
                            alertsNumber,
                            alertsNumber
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
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
private fun WeatherHeroCardPreview() {
    FyneCastTheme() {
        WeatherHeroCard(
            icon = "Hi",
            city = "Hi",
            timestamp = "Hi",
            description = "Hi",
            temperature = "Hi",
            isFavorite = true,
            alertsNumber = 10,
            onToggleFavorite = {},
            onAlertsBannerClick = {}
        )
    }
}
```

## home\ui\components\WeatherHourlyCarousel.kt

```kotlin
package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
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
import com.mhq.fynecast.core.ui.format.DateTimeFormats
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.home.domain.models.HourlyForecastDomainModel
import com.mhq.fynecast.core.ui.globalcomponents.currentLocale
import java.time.LocalDateTime

@Composable
fun WeatherHourlyCarousel(
    hourlyForecasts: List<HourlyForecastDomainModel>,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    val locale = currentLocale()

    GlassyCard(
        cornerRadius = 28.dp,
        padding = 20.dp,
        containerAlpha = 0.08f,
        borderAlpha = 0.15f,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.hourly_forecast),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    count = hourlyForecasts.size,
                    key = { index -> hourlyForecasts[index].time.toString() }
                ) { index ->
                    val item = hourlyForecasts[index]
                    // Highlight the first element or elements matching current hour context
                    val isCurrentHour = index == 0

                    WeatherHourlyItem(
                        header = DateTimeFormats.hour(item.time, locale),
                        weatherIconUrl = item.iconUrl,
                        temperature = if (isMetric) item.tempC else item.tempF,
                        isCurrentHour = isCurrentHour
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
private fun WeatherHourlyCarouselPreview() {
    FyneCastTheme() {
        WeatherHourlyCarousel(
            hourlyForecasts = emptyList(),
            isMetric = true,
        )
    }
}
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import kotlin.String
import kotlin.math.roundToInt

@Composable
fun WeatherHourlyItem(
    header: String,
    weatherIconUrl: String,
    temperature: Double,
    isCurrentHour: Boolean,
    modifier: Modifier = Modifier
) {
    val capsuleBackground = if (isCurrentHour) {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
            )
        )
    }

    val borderStrokeColor = if (isCurrentHour) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    }

    val contentColor = if (isCurrentHour) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .width(68.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(capsuleBackground)
            .border(width = 1.dp, brush = Brush.verticalGradient(listOf(borderStrokeColor, borderStrokeColor.copy(alpha = 0.2f))), shape = RoundedCornerShape(24.dp))
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Text(
            text = header,
            color = contentColor.copy(alpha = if (isCurrentHour) 0.9f else 0.6f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isCurrentHour) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        )

        AsyncImage(
            model = "https:$weatherIconUrl",
            contentDescription = stringResource(R.string.hourly_condition),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .size(38.dp)
        )

        Text(
            text = "${temperature.roundToInt()}°",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor
        )
    }
}


@Preview(showBackground = true)
@Composable
fun WeatherHourlyItemPreview() {
    FyneCastTheme() {
        WeatherHourlyItem(
            header = "12:00",
            weatherIconUrl = "",
            temperature = 10.0,
            isCurrentHour = true,
        )
    }
}
```

## home\ui\components\WeatherSearchBar.kt

```kotlin
package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.mhq.fynecast.home.domain.models.CitySuggestionModel

@Composable
fun WeatherSearchBar(
    suggestions: List<CitySuggestionModel>,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (CitySuggestionModel) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }
    val isListVisible = showSuggestions && suggestions.isNotEmpty() && searchQuery.length > 2
    val focusManager = LocalFocusManager.current

    val onSurfaceColor = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(5f) // Increased to ensure dropdown sits beautifully above all carousel elements
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onQueryChange(it)
                showSuggestions = it.isNotEmpty()
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.enter_a_city_name),
                    color = onSurfaceColor.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_icon),
                    tint = onSurfaceColor.copy(alpha = 0.6f)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            onQueryChange("")
                            showSuggestions = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear_input),
                                tint = onSurfaceColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(onClick = onMapClick) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = stringResource(R.string.pick_from_map),
                            tint = onSurfaceColor.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = onSurfaceColor,
                unfocusedTextColor = onSurfaceColor,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f),
                focusedBorderColor = onSurfaceColor.copy(alpha = 0.25f),
                unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.1f),
                cursorColor = onSurfaceColor
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (isListVisible) {
                        onSuggestionClick(suggestions[0])
                        searchQuery = suggestions[0].fullName
                        showSuggestions = false
                        focusManager.clearFocus()
                    }
                }
            )
        )

        if (isListVisible) {
            // Updated to leverage your premium GlassyCard framework wrapper natively
            GlassyCard(
                cornerRadius = 24.dp,
                padding = 4.dp,
                containerAlpha = 0.15f,
                borderAlpha = 0.25f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    items(suggestions) { suggestion ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = suggestion.fullName,
                                    color = onSurfaceColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = onSurfaceColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchQuery = suggestion.fullName
                                    showSuggestions = false
                                    onSuggestionClick(suggestion)
                                    focusManager.clearFocus()
                                }
                        )
                    }
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

@Preview(showBackground = true)
@Composable
fun WeatherSearchBarPreview() {
    FyneCastTheme() {
        WeatherSearchBar(
            suggestions = emptyList(),
            onQueryChange = {},
            onSuggestionClick = {},
            onMapClick = {}
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
                        letterSpacing = 0.8.sp
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
import com.mhq.fynecast.home.domain.models.WeatherStatsDomainModel
import java.text.NumberFormat

@Composable
fun WeatherStatsGrid(
    stats: WeatherStatsDomainModel,
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

## home\ui\HomeRetryAction.kt

```kotlin
package com.mhq.fynecast.home.ui

enum class HomeRetryAction {
    RequestLocation,
    ReloadPlace
}
```

## home\ui\HomeStateContainer.kt

```kotlin
package com.mhq.fynecast.home.ui

import com.mhq.fynecast.home.domain.models.CitySuggestionModel

data class HomeStateContainer(
    val uiState: HomeUiState = HomeUiState.Loading,
    val selectedCityName: String = "",
    val citySuggestions: List<CitySuggestionModel> = emptyList(),
    val isFavorite: Boolean = false
)
```

## home\ui\HomeUiState.kt

```kotlin
package com.mhq.fynecast.home.ui

import com.mhq.fynecast.home.domain.models.WeatherDomainModel

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val weather: WeatherDomainModel) : HomeUiState
    data class Error(val message: String, val retry: HomeRetryAction) : HomeUiState
}
```

## home\ui\HomeViewModel.kt

```kotlin
package com.mhq.fynecast.home.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.favorites.domain.usecases.RefreshFavoriteSnapshotUseCase
import com.mhq.fynecast.favorites.domain.usecases.ToggleFavoriteUseCase
import com.mhq.fynecast.home.domain.mapper.toFavoriteCity
import com.mhq.fynecast.home.domain.models.GeoCoordinates
import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.domain.usecases.GetCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.GetLastSearchedCityUseCase
import com.mhq.fynecast.home.domain.usecases.GetLocationSuggestionsUseCase
import com.mhq.fynecast.home.domain.usecases.LoadCurrentWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveFavoriteStatusUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveSignedInUserIdUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class HomeViewModel(
    private val loadCurrentWeatherUseCase: LoadCurrentWeatherUseCase,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val getLocationSuggestionsUseCase: GetLocationSuggestionsUseCase,
    private val getLastSearchedCityUseCase: GetLastSearchedCityUseCase,
    private val observeSignedInUserIdUseCase: ObserveSignedInUserIdUseCase,
    private val observeFavoriteStatusUseCase: ObserveFavoriteStatusUseCase,
    private val refreshFavoriteSnapshotUseCase: RefreshFavoriteSnapshotUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updatePreferenceUseCase: UpdatePreferenceUseCase,
    private val defaultCityFallback: String = "Cairo"
) : ViewModel() {

    private val _state = MutableStateFlow(HomeStateContainer())
    val state: StateFlow<HomeStateContainer> = _state.asStateFlow()
    private var loadTarget: LoadTarget = LoadTarget.Startup(useDeviceLocation = false)
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var favoriteJob: Job? = null
    private var lastRefreshTime = 0L
    private val refreshCooldownMillis = 60 * 1000L
    private var loadedLangCode: String? = null
    private var latestLangCode: String? = null

    /** True once any load has been requested;
     * the UI uses it to ask for location only once. */
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
            val longitude: Double
        ) : LoadTarget {
            override val retry = HomeRetryAction.ReloadPlace
        }
    }

    init {
        observeSignedInUserIdUseCase()
            .drop(1) // the first value is whoever is already signed in
            .onEach { resetForNewSession() }
            .launchIn(viewModelScope)
    }

    fun onLocationPermissionResult(isGranted: Boolean, langCode: String) {
        load(LoadTarget.Startup(useDeviceLocation = isGranted), langCode)
    }

    fun onCitySelected(
        suggestionName: String,
        lat: Double,
        lon: Double,
        langCode: String
    ) {
        viewModelScope.launch {
            updatePreferenceUseCase.lastSearchedCity(name = suggestionName, lat = lat, lon = lon)
        }
        load(
            LoadTarget.Place(suggestionName, lat, lon), langCode
        )
    }

    /** Reloads the place that failed. Startup failures retry through the location flow instead. */
    fun onRetryClicked(langCode: String) = load(loadTarget, langCode)

    private fun load(target: LoadTarget, langCode: String) {
        loadTarget = target
        isInitialLaunchPerformed = true

        loadJob?.cancel()                    // the newest request wins
        loadJob = viewModelScope.launch {
            _state.update { it.copy(uiState = HomeUiState.Loading) }
            try {
                val (weather, displayName) = when (target) {
                    is LoadTarget.Place ->
                        loadCurrentWeatherUseCase(
                            "${target.latitude},${target.longitude}",
                            langCode
                        ) to target.name

                    is LoadTarget.Startup -> {
                        val weather = loadCurrentWeatherUseCase(startupQuery(target), langCode)
                        weather to "${weather.cityName}, ${weather.country}"
                    }
                }
                _state.update {
                    it.copy(uiState = HomeUiState.Success(weather), selectedCityName = displayName)
                }
                loadedLangCode = langCode
                onWeatherShown(weather)
                latestLangCode?.takeIf { it != langCode }?.let { onLanguageChanged(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // selectedCityName is untouched: no city was loaded, so none should be claimed.
                _state.update {
                    it.copy(uiState = HomeUiState.Error(e.message ?: "Unknown Error", target.retry))
                }
            }
        }
    }

    /** GPS (when allowed) → last searched city → default city. */
    private suspend fun startupQuery(target: LoadTarget.Startup): String {
        delay(300.milliseconds)
        val coordinates = if (target.useDeviceLocation) currentCoordinatesOrNull() else null
        if (coordinates != null) return "${coordinates.latitude},${coordinates.longitude}"

        val savedCity = getLastSearchedCityUseCase()
        return savedCity?.let { "${it.latitude},${it.longitude}" } ?: defaultCityFallback
    }

    private suspend fun currentCoordinatesOrNull(): GeoCoordinates? =
        try {
            getCurrentLocationUseCase()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    fun refreshCurrentCity(langCode: String) {
        val now = System.currentTimeMillis()
        if (now - lastRefreshTime < refreshCooldownMillis) return
        if (loadJob?.isActive == true) return  // never interrupt a load
        val weather = (_state.value.uiState as? HomeUiState.Success)?.weather ?: return

        loadJob = viewModelScope.launch {
            try {
                val fresh = loadCurrentWeatherUseCase("${weather.latitude},${weather.longitude}", langCode)
                _state.update { it.copy(uiState = HomeUiState.Success(fresh)) }
                loadedLangCode = langCode
                onWeatherShown(fresh)
                lastRefreshTime = System.currentTimeMillis()  // only after success
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
        val weather = (_state.value.uiState as? HomeUiState.Success)?.weather ?: return
        load(LoadTarget.Place(_state.value.selectedCityName, weather.latitude, weather.longitude), langCode)
    }

    private fun resetForNewSession() {
        loadJob?.cancel()
        searchJob?.cancel()
        favoriteJob?.cancel()
        _state.value = HomeStateContainer()
        isInitialLaunchPerformed = false
        lastRefreshTime = 0L
        loadTarget = LoadTarget.Startup(useDeviceLocation = false)
        loadedLangCode = null
    }

    fun onSearchQueryChanged(newQuery: String, langCode: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500.milliseconds)
            val suggestions = runCatching {
                getLocationSuggestionsUseCase(newQuery, langCode)
            }.getOrNull() ?: emptyList()
            _state.update { it.copy(citySuggestions = suggestions) }
        }
    }

    fun onToggleFavorite() {
        val weather = (_state.value.uiState as? HomeUiState.Success)?.weather ?: return
        viewModelScope.launch { toggleFavoriteUseCase(weather.toFavoriteCity()) }
    }

    private fun onWeatherShown(weather: WeatherDomainModel) {
        favoriteJob?.cancel()
        favoriteJob = observeFavoriteStatusUseCase(weather.latitude, weather.longitude)
            .onEach { isFav -> _state.update { it.copy(isFavorite = isFav) } }
            .launchIn(viewModelScope)

        // Keeps a favorite's temperature and icon current whenever its city is shown.
        viewModelScope.launch { refreshFavoriteSnapshotUseCase(weather.toFavoriteCity()) }
    }

    companion object {
        private const val TAG = "HomeViewModel"

        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FyneCastApplication
                HomeViewModel(
                    loadCurrentWeatherUseCase = app.container.loadCurrentWeatherUseCase,
                    getCurrentLocationUseCase = app.container.getCurrentLocationUseCase,
                    getLocationSuggestionsUseCase = app.container.getLocationSuggestionsUseCase,
                    getLastSearchedCityUseCase = app.container.getLastSearchedCityUseCase,
                    observeFavoriteStatusUseCase = app.container.observeFavoriteStatusUseCase,
                    observeSignedInUserIdUseCase = app.container.observeSignedInUserIdUseCase,
                    refreshFavoriteSnapshotUseCase = app.container.refreshFavoriteSnapshotUseCase,
                    toggleFavoriteUseCase = app.container.toggleFavoriteUseCase,
                    updatePreferenceUseCase = app.container.updatePreferenceUseCase
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.home.ui.HomeRetryAction
import com.mhq.fynecast.home.ui.HomeUiState
import com.mhq.fynecast.home.ui.HomeViewModel

@Composable
fun HomeContainer(
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory),
    onNavigateToMap: () -> Unit,
    onAlertsBannerClick: () -> Unit,
    onRequestPermission: (String) -> Unit,
    isMetric: Boolean,
    langCode: String,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {

    val stateContainer by homeViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (!homeViewModel.isInitialLaunchPerformed) {
            onRequestPermission(langCode)
        }
    }

    // 2. Read the nested uiState directly from the container wrapper
    when (val uiState = stateContainer.uiState) {
        is HomeUiState.Loading -> {
            HomeLoading(contentPadding = contentPadding, modifier = modifier)
        }

        is HomeUiState.Success -> {
            HomeContent(
                weatherDomainModel = uiState.weather,
                citySuggestions = stateContainer.citySuggestions,
                selectedCityName = stateContainer.selectedCityName,
                isMetric = isMetric,
                isFavorite = stateContainer.isFavorite,
                onQueryChange = { homeViewModel.onSearchQueryChanged(it, langCode) },
                onSuggestionClick = { suggestion ->
                    homeViewModel.onCitySelected(
                        suggestion.fullName,
                        suggestion.latitude,
                        suggestion.longitude,
                        langCode
                    )
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
                        // One load: the permission result starts it. If the permission is already
                        // decided, the callback fires immediately and the load starts anyway.
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.format.DateTimeFormats
import com.mhq.fynecast.core.ui.globalcomponents.currentLocale
import com.mhq.fynecast.home.domain.models.CitySuggestionModel
import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.ui.components.WeatherDailyCarousel
import com.mhq.fynecast.home.ui.components.WeatherHeroCard
import com.mhq.fynecast.home.ui.components.WeatherHourlyCarousel
import com.mhq.fynecast.home.ui.components.WeatherSearchBar
import com.mhq.fynecast.home.ui.components.WeatherStatsGrid

@Composable
fun HomeContent(
    weatherDomainModel: WeatherDomainModel,
    citySuggestions: List<CitySuggestionModel>,
    selectedCityName: String,
    isMetric: Boolean,
    isFavorite: Boolean,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (CitySuggestionModel) -> Unit,
    onMapIconClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAlertsBannerClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp)
                .statusBarsPadding()
        ) {
            WeatherSearchBar(
                suggestions = citySuggestions,
                onQueryChange = onQueryChange,
                onSuggestionClick = onSuggestionClick,
                onMapClick = onMapIconClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            )
            WeatherHeroCard(
                icon = weatherDomainModel.conditionIcon,
                city = selectedCityName.substringBefore(","),
                timestamp = weatherDomainModel.lastUpdated?.let {
                    DateTimeFormats.lastUpdated(it, currentLocale())
                }.orEmpty(),
                description = weatherDomainModel.conditionText,
                temperature = if (isMetric)
                    "${weatherDomainModel.currentTempC}°"
                else
                    "${weatherDomainModel.currentTempF}°",
                isFavorite = isFavorite,
                alertsNumber = weatherDomainModel.alerts.size,
                onToggleFavorite = onToggleFavorite,
                onAlertsBannerClick = onAlertsBannerClick,
                modifier = Modifier.fillMaxWidth()
            )
            WeatherHourlyCarousel(
                hourlyForecasts = weatherDomainModel.hourlyForecasts,
                isMetric = isMetric
            )
            WeatherDailyCarousel(
                dailyForecasts = weatherDomainModel.dailyForecasts,
                isMetric = isMetric
            )
            WeatherStatsGrid(
                stats = weatherDomainModel.stats,
                isMetric = isMetric
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

import com.mhq.fynecast.home.domain.models.CitySuggestionModel
import com.mhq.fynecast.home.domain.repository.LocationRepository

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
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.core.ui.theme.AbyssalVoid
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen
import com.mhq.fynecast.home.domain.models.CitySuggestionModel

@Composable
fun LocationConfirmCard(
    location: CitySuggestionModel,
    onLocationPicked: (CitySuggestionModel) -> Unit,
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
                text = location.fullName,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(
                modifier = Modifier.height(12.dp)
            )
            Button(
                onClick = { onLocationPicked(location) },
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
    FyneCastTheme(){
        LocationConfirmCard(
            location = CitySuggestionModel(
                fullName = "City Name",
                latitude = 100.0,
                longitude = 100.0
            ),
            onLocationPicked = {}
        )
    }
}
```

## map\ui\MapPickerUiState.kt

```kotlin
package com.mhq.fynecast.map.ui

import com.mhq.fynecast.home.domain.models.CitySuggestionModel

data class MapPickerUiState(
    val selectedLocation: CitySuggestionModel? = null,
    val isSearching: Boolean = false,
    val errorMessage: String? = null
)
```

## map\ui\MapPickerViewModel.kt

```kotlin
package com.mhq.fynecast.map.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.map.domain.usecases.ReverseGeocodeLocationUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapPickerViewModel(
    private val reverseGeocodeLocationUseCase: ReverseGeocodeLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapPickerUiState())
    val uiState: StateFlow<MapPickerUiState> = _uiState.asStateFlow()

    private var geocodeJob: Job? = null

    fun onMapClicked(latitude: Double, longitude: Double, langCode: String) {
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, selectedLocation = null, errorMessage = null) }
            try {
                val resolvedLocation = reverseGeocodeLocationUseCase(latitude, longitude, langCode)
                if (resolvedLocation != null) {
                    _uiState.update { it.copy(selectedLocation = resolvedLocation) }
                } else {
                    _uiState.update {
                        it.copy(errorMessage = "No location data found here. Please pick a more specific location.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Couldn't resolve this location. Please try again.")
                }
            }
            _uiState.update { it.copy(isSearching = false) }
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FyneCastApplication
                MapPickerViewModel(
                    reverseGeocodeLocationUseCase = app.container.reverseGeocodeLocationUseCase
                )
            }
        }
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.AbyssalVoid
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen
import com.mhq.fynecast.home.domain.models.CitySuggestionModel
import com.mhq.fynecast.map.ui.MapPickerViewModel
import com.mhq.fynecast.map.ui.components.ErrorCard
import com.mhq.fynecast.map.ui.components.LoadingCard
import com.mhq.fynecast.map.ui.components.LocationConfirmCard
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

private const val MARKER_ICON_ID = "map_picker_marker"

/**
 * SymbolManager has no built-in default pin (unlike the deprecated Marker API),
 * so we draw a simple dot bitmap using the app's own theme colors and register
 * it with the style. Swap this out for a real drawable asset (via
 * ContextCompat.getDrawable(context, R.drawable.xxx).toBitmap()) whenever one exists.
 */
private fun createMarkerBitmap(fillColor: Int, strokeColor: Int): Bitmap {
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
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

@Composable
fun MapPickerScreen(
    mapPickerViewModel: MapPickerViewModel,
    langCode: String,
    onLocationPicked: (CitySuggestionModel) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by mapPickerViewModel.uiState.collectAsState()

    remember { MapLibre.getInstance(context) }
    val mapView = remember { MapView(context) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        // Wait for style initialization before assigning SymbolManager
                        map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                            style.addImage(
                                MARKER_ICON_ID,
                                createMarkerBitmap(markerFillColor, markerStrokeColor)
                            )

                            val symbolManager = SymbolManager(mapView, map, style)
                            symbolManager.iconAllowOverlap = true
                            symbolManager.textAllowOverlap = true

                            map.setMaxZoomPreference(10.0)
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(30.0444, 31.2357))
                                .zoom(5.0)
                                .build()

                            map.addOnMapClickListener { point ->
                                // Replaced deprecated map.clear()
                                symbolManager.deleteAll()

                                // Replaced deprecated map.addMarker(MarkerOptions())
                                symbolManager.create(
                                    SymbolOptions()
                                        .withLatLng(point)
                                        .withIconImage(MARKER_ICON_ID)
                                )

                                mapPickerViewModel.onMapClicked(
                                    point.latitude,
                                    point.longitude,
                                    langCode
                                )
                                true
                            }
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
            )
        } else {
            uiState.errorMessage?.let { message ->
                ErrorCard(
                    message = message,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } ?: uiState.selectedLocation?.let { location ->
                LocationConfirmCard(
                    location = location,
                    onLocationPicked = onLocationPicked,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
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
import com.mhq.fynecast.settings.domain.models.UserPreferences
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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

    override suspend fun updateLanguage(language: String) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.CURRENT_LANGUAGE] = language }
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
            currentLanguage = preferences[PreferencesKeys.CURRENT_LANGUAGE] ?: "English",
            isDarkMode = preferences[PreferencesKeys.IS_DARK_MODE] ?: true,
            lastSearchedCityName = preferences[PreferencesKeys.LAST_CITY_NAME],
            lastSearchedCityLat = preferences[PreferencesKeys.LAST_CITY_LAT],
            lastSearchedCityLon = preferences[PreferencesKeys.LAST_CITY_LON]
        )
    }
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
    val currentLanguage: String,
    val isDarkMode: Boolean,
    val lastSearchedCityName: String? = null,
    val lastSearchedCityLat: Double? = null,
    val lastSearchedCityLon: Double? = null,
    val notificationPromptShown: Boolean = false
)
```

## settings\domain\repository\UserPreferencesRepository.kt

```kotlin
package com.mhq.fynecast.settings.domain.repository

import com.mhq.fynecast.settings.domain.models.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userPreferencesFlow: Flow<UserPreferences>

    suspend fun getPreferences(): UserPreferences
    suspend fun updateMetricSystem(isMetric: Boolean)
    suspend fun updateNotifications(enabled: Boolean)
    suspend fun updateLanguage(language: String)
    suspend fun updateDarkMode(enabled: Boolean)
    suspend fun updateLastSearchedCity(name: String, lat: Double, lon: Double)
    suspend fun clearLastSearchedCity()
    suspend fun consumeNotificationPrompt(): Boolean
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

## settings\domain\usecases\profile\UpdatePreferenceUseCase.kt

```kotlin
package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class UpdatePreferenceUseCase(
    private val preferencesRepository: UserPreferencesRepository
) {
    suspend fun metricSystem(isMetric: Boolean) =
        preferencesRepository.updateMetricSystem(isMetric)

    suspend fun notifications(enabled: Boolean) =
        preferencesRepository.updateNotifications(enabled)

    suspend fun language(language: String) =
        preferencesRepository.updateLanguage(language)

    suspend fun darkMode(enabled: Boolean) =
        preferencesRepository.updateDarkMode(enabled)

    suspend fun lastSearchedCity(name: String, lat: Double, lon: Double) =
        preferencesRepository.updateLastSearchedCity(name, lat, lon)

    suspend fun clearLastSearchedCity() = preferencesRepository.clearLastSearchedCity()
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
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onLogoutClick,
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                tint = MaterialTheme.colorScheme.error,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
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
import com.mhq.fynecast.settings.domain.models.AppLanguage

@Composable
fun SettingsPanel(
    notificationsEnabled: Boolean,
    isDarkMode: Boolean,
    isMetric: Boolean,
    currentLanguage: String,
    onNotificationsToggle: (Boolean) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onMetricToggle: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
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
                isFirstOptionSelected = currentLanguage == AppLanguage.English.storedName,
                firstOptionLabel = stringResource(R.string.english),
                secondOptionLabel = "العربية",
                onFirstOptionSelected = { onLanguageChange(AppLanguage.English.storedName) },
                onSecondOptionSelected = { onLanguageChange(AppLanguage.Arabic.storedName) }
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
            currentLanguage = "",
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.R
import com.mhq.fynecast.settings.ui.SettingsViewModel

@Composable
fun SettingsContainer(
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory),
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
    val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()

    val isMetric by settingsViewModel.isMetric.collectAsStateWithLifecycle()
    val isDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()

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

    Box(modifier = modifier.fillMaxSize()) {
        SettingsContent(
            isMetric = isMetric,
            onMetricToggle = settingsViewModel::toggleUnitSystem,
            isDarkMode = isDarkMode,
            onDarkModeToggle = settingsViewModel::toggleDarkMode,
            notificationsEnabled = notificationsEnabled,
            onNotificationsToggle = settingsViewModel::toggleNotifications,
            profileName = profileName,
            profileEmail = profileEmail,
            profileImageUri = profileImageUri,
            currentLanguage = currentLanguage,
            onLanguageChange = settingsViewModel::toggleLanguage,
            onEditProfileClick = onEditProfileClick,
            onLogoutClick = { settingsViewModel.logOutUserSession() },
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.settings.ui.components.SettingsLogoutButton
import com.mhq.fynecast.settings.ui.components.SettingsPanel
import com.mhq.fynecast.settings.ui.components.SettingsProfileHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    notificationsEnabled: Boolean,
    isDarkMode: Boolean,
    isMetric: Boolean,
    profileName: String,
    profileEmail: String,
    profileImageUri: String?,
    currentLanguage: String,
    onNotificationsToggle: (Boolean) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onMetricToggle: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onEditProfileClick: () -> Unit,
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
                isDarkMode = isDarkMode,
                onDarkModeToggle = onDarkModeToggle,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                notificationsEnabled = notificationsEnabled,
                onNotificationsToggle = onNotificationsToggle
            )
            SettingsLogoutButton(
                onLogoutClick = onLogoutClick
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
            notificationsEnabled = true,
            isDarkMode = true,
            isMetric = true,
            profileName = "name",
            profileEmail = "email",
            onNotificationsToggle = {},
            onDarkModeToggle = {},
            onMetricToggle = {},
            onEditProfileClick = {},
            onLogoutClick = {},
            contentPadding = PaddingValues(0.dp),
            currentLanguage = "",
            onLanguageChange = {},
            profileImageUri = null
        )
    }
}
```

## settings\ui\SettingsState.kt

```kotlin
package com.mhq.fynecast.settings.ui

data class SettingsState(
    val username: String = "Username",
    val email: String = "username@gmail.com",
    val profileImageUri: String? = null
)
```

## settings\ui\SettingsViewModel.kt

```kotlin
package com.mhq.fynecast.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.alerts.notifications.domain.usecases.ConsumeNotificationPromptUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.SyncNotificationPermissionUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ObserveUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.session.LogoutUserUseCase
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.settings.domain.models.AppLanguage
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val updatePreference: UpdatePreferenceUseCase,
    private val toggleNotifications: ToggleNotificationsUseCase,
    private val handleNotificationPermissionResult: HandleNotificationPermissionResultUseCase,
    private val syncNotificationPermission: SyncNotificationPermissionUseCase,
    private val consumeNotificationPrompt: ConsumeNotificationPromptUseCase,
    private val logoutUserUseCase: LogoutUserUseCase
) : ViewModel() {

    private val _localeEvent = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val localeEvent = _localeEvent.asSharedFlow()
    private val _requestNotificationPermissionEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requestNotificationPermissionEvent = _requestNotificationPermissionEvent.asSharedFlow()
    private val profileFlow = observeUserProfileUseCase()
    private val preferencesFlow = observeUserPreferencesUseCase()
    private val _logoutCompleteChannel = Channel<Unit>(Channel.BUFFERED)
    val logoutComplete = _logoutCompleteChannel.receiveAsFlow()
    private val _logoutFailedChannel = Channel<Unit>(Channel.BUFFERED)
    val logoutFailed = _logoutFailedChannel.receiveAsFlow()
    private var isLoggingOut = false

    val profileName: StateFlow<String> =
        profileFlow.map { it.name }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                "Username"
            )

    val profileEmail: StateFlow<String> =
        profileFlow.map { it.email }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                "username@gmail.com"
            )

    val profileImageUri: StateFlow<String?> =
        profileFlow.map { it.profileImageUri }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    val isMetric: StateFlow<Boolean> =
        preferencesFlow.map { it.isMetric }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                true
            )

    fun toggleUnitSystem(isMetric: Boolean) {
        viewModelScope.launch {
            updatePreference.metricSystem(isMetric)
        }
    }

    // notificationsEnabled StateFlow: initial value true → false
    val notificationsEnabled: StateFlow<Boolean> =
        preferencesFlow.map { it.notificationsEnabled }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Called once the location permission flow finishes, whether it was granted or denied. */
    fun requestNotificationPermissionOnFirstLaunch() {
        viewModelScope.launch {
            if (consumeNotificationPrompt()) _requestNotificationPermissionEvent.tryEmit(Unit)
        }
    }

    fun toggleNotifications(wantsEnabled: Boolean) {
        viewModelScope.launch {
            when (toggleNotifications.invoke(wantsEnabled)) {
                ToggleNotificationsUseCase.Result.PermissionRequired ->
                    _requestNotificationPermissionEvent.tryEmit(Unit)
                else -> Unit
            }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            handleNotificationPermissionResult(granted)
        }
    }

    fun syncNotificationPermissionState() {
        viewModelScope.launch {
            syncNotificationPermission()
        }
    }

    val currentLanguage: StateFlow<String> =
        preferencesFlow.map { it.currentLanguage }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                "English"
            )

    fun toggleLanguage(language: String) {
        viewModelScope.launch { updatePreference.language(language) }
    }

    val isDarkMode: StateFlow<Boolean> =
        preferencesFlow.map { it.isDarkMode }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                true
            )

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { updatePreference.darkMode(enabled) }
    }

    fun logOutUserSession() {
        if (isLoggingOut) return
        isLoggingOut = true
        viewModelScope.launch {
            try {
                logoutUserUseCase().fold(
                    onSuccess = { _logoutCompleteChannel.send(Unit) },
                    onFailure = { _logoutFailedChannel.send(Unit) }
                )
            } finally {
                isLoggingOut = false
            }
        }
    }

    init {
        viewModelScope.launch {
            preferencesFlow
                .map { AppLanguage.fromStoredName(it.currentLanguage).code }
                .distinctUntilChanged()
                .collect { _localeEvent.tryEmit(it) }
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)
                val container = app.container
                SettingsViewModel(
                    observeUserProfileUseCase = container.observeUserProfileUseCase,
                    observeUserPreferencesUseCase = container.observeUserPreferencesUseCase,
                    updatePreference = container.updatePreferenceUseCase,
                    toggleNotifications = container.toggleNotificationsUseCase,
                    handleNotificationPermissionResult = container.handleNotificationPermissionResultUseCase,
                    syncNotificationPermission = container.syncNotificationPermissionUseCase,
                    consumeNotificationPrompt = container.consumeNotificationPromptUseCase,
                    logoutUserUseCase = container.logoutUserUseCase
                )
            }
        }
    }
}
```

