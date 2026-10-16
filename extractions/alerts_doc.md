# Files under `app/src/main/java/com/mhq/fynecast/alerts`

## data\database\AlertDao.kt

```kotlin
package com.mhq.fynecast.alerts.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts")
    suspend fun getAllTrackedAlerts(): List<TrackedAlertEntity>
    @Query("SELECT id FROM alerts")
    fun getTrackedAlertIds(): Flow<List<String>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackedAlert(alert: TrackedAlertEntity)
    @Query("DELETE FROM alerts WHERE id = :alertId")
    suspend fun deleteTrackedAlertById(alertId: String)
    @Query("DELETE FROM alerts")
    suspend fun deleteAllTrackedAlerts()
}
```

## data\database\AlertsDatabase.kt

```kotlin
package com.mhq.fynecast.alerts.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TrackedAlertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AlertsDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
}
```

## data\database\TrackedAlertEntity.kt

```kotlin
package com.mhq.fynecast.alerts.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

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

## data\dto\AlertDto.kt

```kotlin
package com.mhq.fynecast.alerts.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

## data\dto\AlertsDto.kt

```kotlin
package com.mhq.fynecast.alerts.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlertsDto(
    @SerialName("alert")
    val alerts: List<AlertDto> = emptyList()
)
```

## data\mapper\AlertsMapper.kt

```kotlin
package com.mhq.fynecast.alerts.data.mapper

import com.mhq.fynecast.alerts.data.database.TrackedAlertEntity
import com.mhq.fynecast.alerts.data.dto.AlertDto
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import java.time.OffsetDateTime

fun AlertDto.calculateId(): String = "${event}_${effective}_$areas"

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

// A date with no offset is treated as unknown rather than guessed in the wrong time zone.
private fun parseToEpochMillis(isoString: String): Long? =
    runCatching {
        OffsetDateTime
            .parse(isoString)
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
```

## data\repoimpl\AlertsRepoImpl.kt

```kotlin
package com.mhq.fynecast.alerts.data.repoimpl

import com.mhq.fynecast.alerts.data.database.AlertDao
import com.mhq.fynecast.alerts.data.mapper.toDomain
import com.mhq.fynecast.alerts.data.mapper.toEntity
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository

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

## data\repoimpl\AlertTrackingControllerImpl.kt

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

## domain\models\AlertDomainModel.kt

```kotlin
package com.mhq.fynecast.alerts.domain.models

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

## domain\models\TrackedAlertDomainModel.kt

```kotlin
package com.mhq.fynecast.alerts.domain.models

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
    val location: TrackedLocation
        get() = TrackedLocation(cityName, latitude, longitude)
}
```

## domain\models\TrackedLocation.kt

```kotlin
package com.mhq.fynecast.alerts.domain.models

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

## domain\repository\AlertsRepository.kt

```kotlin
package com.mhq.fynecast.alerts.domain.repository

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import kotlinx.coroutines.flow.Flow

interface AlertsRepository {
    fun getTrackedAlertIds(): Flow<List<String>>
    suspend fun getTrackedAlerts(): List<TrackedAlertDomainModel>
    suspend fun saveTrackedAlert(alert: AlertDomainModel, location: TrackedLocation)
    suspend fun deleteTrackedAlert(alertId: String)
    suspend fun deleteAllTrackedAlerts()
}
```

## domain\repository\AlertTrackingController.kt

```kotlin
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
```

## domain\usecases\CancelAllAlertTrackingUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class CancelAllAlertTrackingUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertTrackingController: AlertTrackingController
) {
    suspend operator fun invoke() = withContext(NonCancellable) {
        val tracked = alertsRepository.getTrackedAlerts()
        tracked.forEach { alertTrackingController.cancelTrackingWorker(it.id) }
        tracked.map { it.location }.distinct().forEach { alertTrackingController.cancelLiftedCheck(it) }
        alertsRepository.deleteAllTrackedAlerts()
        alertTrackingController.showTrackingNotification(emptyList())
    }
}
```

## domain\usecases\CheckAlertConditionsUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.core.domain.time.TimeProvider
import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import kotlin.math.abs

class CheckAlertConditionsUseCase(
    private val alertsRepository: AlertsRepository,
    private val timeProvider: TimeProvider
) {
    /**
     * [weatherSnapshot] null, or for a different place than [trackedLocation], means "unknown":
     * only expiry counts. Only a matching location's alert list can declare an alert lifted.
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

    private fun WeatherDomainModel.isNear(location: TrackedLocation) =
        abs(latitude - location.trackedLocationLatitude) < TOLERANCE_DEGREES &&
                abs(longitude - location.trackedLocationLongitude) < TOLERANCE_DEGREES

    private companion object {
        const val TOLERANCE_DEGREES = 0.1
    }
}
```

## domain\usecases\GetAlertsUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.home.domain.repository.CurrentWeatherStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAlertsUseCase(private val currentWeatherStore: CurrentWeatherStore) {
    operator fun invoke(): Flow<List<AlertDomainModel>?> =
        currentWeatherStore.current.map { it?.alerts }
}
```

## domain\usecases\GetTrackedAlertIdsUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.Flow

class GetTrackedAlertIdsUseCase(private val alertsRepository: AlertsRepository) {
    operator fun invoke(): Flow<List<String>> =
        alertsRepository.getTrackedAlertIds()
}
```

## domain\usecases\GetTrackedAlertStatusUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.core.domain.time.TimeProvider

class GetTrackedAlertStatusUseCase(
    private val alertsRepository: AlertsRepository,
    private val timeProvider: TimeProvider
) {
    sealed interface Status {
        data object NotTracked : Status
        data object Active : Status
        data class Expired(val alert: TrackedAlertDomainModel) : Status
    }

    suspend operator fun invoke(alertId: String): Status {
        val alert = alertsRepository.getTrackedAlerts().firstOrNull { it.id == alertId }
            ?: return Status.NotTracked

        return if (timeProvider.nowMillis() >= alert.expiresTimeMillis)
            Status.Expired(alert)
        else Status.Active
    }
}
```

## domain\usecases\RefreshTrackingNotificationUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository

class RefreshTrackingNotificationUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertTrackingController: AlertTrackingController
) {
    suspend operator fun invoke() =
        alertTrackingController.showTrackingNotification(alertsRepository.getTrackedAlerts())
}
```

## domain\usecases\RestoreAlertTrackingUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.core.domain.time.TimeProvider

class RestoreAlertTrackingUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertTrackingController: AlertTrackingController,
    private val untrackAlertUseCase: UntrackAlertUseCase,
    private val timeProvider: TimeProvider
) {
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

## domain\usecases\ToggleAlertsUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.first

class ToggleAlertsUseCase(
    private val alertsRepository: AlertsRepository,
    private val trackAlertUseCase: TrackAlertUseCase,
    private val untrackAlertUseCase: UntrackAlertUseCase
) {
    sealed interface Result {
        data object Tracked : Result
        data object Untracked : Result
        data object AlreadyExpired : Result
        data object ExpiryUnknown : Result
        data object NotificationsRequired : Result
    }

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

## domain\usecases\TrackAlertUseCase.kt

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

class TrackAlertUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertTrackingController: AlertTrackingController,
    private val timeProvider: TimeProvider,
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase
) {
    sealed interface Result {
        data object Tracked : Result
        data object AlreadyExpired : Result
        data object ExpiryUnknown : Result
        data object NotificationsRequired : Result
    }


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

## domain\usecases\UntrackAlertUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository

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

## notifications\AlertTrackingReceiver.kt

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

## notifications\BootReceiver.kt

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

## notifications\data\infra\AndroidNotificationPermissionChecker.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.data.infra

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker

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

## notifications\domain\infra\NotificationPermissionChecker.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.domain.infra

interface NotificationPermissionChecker {
    fun isGranted(): Boolean
}
```

## notifications\domain\usecases\ConsumeNotificationPromptUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

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

## notifications\domain\usecases\HandleNotificationPermissionResultUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase

class HandleNotificationPermissionResultUseCase(
    private val updatePreferenceUseCase: UpdatePreferenceUseCase
) {
    suspend operator fun invoke(granted: Boolean) {
        updatePreferenceUseCase.notifications(granted)
    }
}
```

## notifications\domain\usecases\SyncNotificationPermissionUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.alerts.domain.usecases.CancelAllAlertTrackingUseCase
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase
import kotlinx.coroutines.flow.first

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

## notifications\domain\usecases\ToggleNotificationsUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.alerts.domain.usecases.CancelAllAlertTrackingUseCase
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase

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

## notifications\ui\AlertsNotificationManager.kt

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

class AlertsNotificationManager(baseContext: Context) {

    private val context: Context = baseContext.withAppLocale()
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun showAlarmNotification(
        clearedAlerts: List<TrackedAlertDomainModel>,
        notificationId: Int
    ) {
        val title = if (clearedAlerts.size == 1) {
            context.getString(R.string.alert_lifted_title_single, clearedAlerts.first().event)
        } else {
            context.getString(R.string.alert_lifted_title_multiple, clearedAlerts.size)
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

    fun showTrackingNotification(alerts: List<TrackedAlertDomainModel>) {
        if (alerts.isEmpty()) {
            cancelTrackingNotification()
            return
        }
        val title = if (alerts.size == 1)
            context.getString(R.string.alert_tracking_title_single, alerts.first().event)
        else
            context.getString(R.string.alert_tracking_title_multiple, alerts.size)

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
                    R.drawable.ic_cancel, context.getString(R.string.cancel_tracking),
                    receiverIntent(AlertTrackingReceiver.ACTION_CANCEL_ALL, requestCode = 2)
                )
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()

        notificationManager.notify(TRACKING_NOTIFICATION_ID, notification)
    }

    fun cancelTrackingNotification() = notificationManager.cancel(TRACKING_NOTIFICATION_ID)

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

## notifications\WeatherAlertTrackingWorker.kt

```kotlin
package com.mhq.fynecast.alerts.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertStatusUseCase
import com.mhq.fynecast.alerts.notifications.ui.AlertsNotificationManager
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.CancellationException

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

## notifications\WeatherRefreshWorker.kt

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

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
            val lang = if (container.observeUserPreferencesUseCase()
                    .first().currentLanguage == "Arabic"
            ) "ar" else "en"

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

## ui\AlertsEvent.kt

```kotlin
package com.mhq.fynecast.alerts.ui

sealed interface AlertsEvent {
    data object AlertAlreadyExpired : AlertsEvent
    data object AlertExpiryUnknown : AlertsEvent
    data class AlertTrackingStarted(val eventName: String) : AlertsEvent
    data object RequestNotificationPermission : AlertsEvent
    data object NotificationPermissionDenied : AlertsEvent
}
```

## ui\AlertsUiState.kt

```kotlin
package com.mhq.fynecast.alerts.ui

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel

sealed interface AlertsUiState {
    object Loading : AlertsUiState
    data class Success(val alerts: List<AlertDomainModel>) : AlertsUiState
}
```

## ui\AlertsViewModel.kt

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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlertsViewModel(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val getTrackedAlertIdsUseCase: GetTrackedAlertIdsUseCase,
    private val observeCurrentLocationUseCase: ObserveCurrentLocationUseCase,
    private val toggleAlertsUseCase: ToggleAlertsUseCase,
    private val toggleNotificationsUseCase: ToggleNotificationsUseCase,
    private val handleNotificationPermissionResultUseCase: HandleNotificationPermissionResultUseCase
) : ViewModel() {

    val uiState: StateFlow<AlertsUiState> = getAlertsUseCase()
        .map { alerts ->
            if (alerts == null) AlertsUiState.Loading else AlertsUiState.Success(alerts)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            AlertsUiState.Loading
        )

    val trackedAlertIds: StateFlow<List<String>> = getTrackedAlertIdsUseCase()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

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

    fun onAlertTrackToggled(alert: AlertDomainModel) {
        viewModelScope.launch { toggle(alert) }
    }

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

    fun onNotificationsPromptDismissed() {
        _showNotificationsPrompt.value = false
        pendingAlert = null
    }

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

    private suspend fun trackPendingAlert() {
        val alert = pendingAlert ?: return
        pendingAlert = null
        toggle(alert)
    }

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

## ui\components\AlertCard.kt

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.R
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.core.ui.theme.AppleRed
import com.mhq.fynecast.core.ui.theme.EditorialRoyalBlue
import com.mhq.fynecast.core.ui.theme.Tangerine
import com.mhq.fynecast.core.ui.theme.VividAmber

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
            "extreme" -> AppleRed // Was MaterialTheme.colorScheme.error
            "severe" -> Tangerine
            "moderate" -> VividAmber
            else -> EditorialRoyalBlue // Was MaterialTheme.colorScheme.tertiary
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
                    // Soft pulsing anchor dot instead of a stark structural ribbon
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color = severityColor, shape = RoundedCornerShape(50))
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

//import android.content.res.Configuration
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.animateContentSize
//import androidx.compose.animation.core.tween
//import androidx.compose.animation.expandVertically
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.animation.shrinkVertically
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ExpandLess
//import androidx.compose.material.icons.filled.ExpandMore
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clipToBounds
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import com.mhq.fynecast.R
//import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
//import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
//import com.mhq.fynecast.core.ui.theme.FyneCastTheme
//import com.mhq.fynecast.core.ui.theme.Tangerine
//import com.mhq.fynecast.core.ui.theme.VividAmber
//
//@Composable
//fun AlertCard(
//    alert: AlertDomainModel,
//    isTracked: Boolean,
//    isExpanded: Boolean,
//    onTrackToggle: () -> Unit,
//    onExpandToggle: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    val bottomShape = remember {
//        RoundedCornerShape(
//            bottomStart = 16.dp,
//            bottomEnd = 16.dp
//        )
//    }
//
//    val ribbonColor = remember(alert.severity) {
//        alert.severity.lowercase()
//    }.let { severity ->
//        when (severity) {
//            "extreme" -> MaterialTheme.colorScheme.error
//            "severe" -> Tangerine
//            "moderate" -> VividAmber
//            else -> MaterialTheme.colorScheme.tertiary
//        }
//    }
//
//    GlassyCard(
//        cornerRadius = 16.dp,
//        padding = 0.dp,
//        modifier = modifier
//            .fillMaxWidth()
//            .clipToBounds()
//            .animateContentSize(animationSpec = tween(durationMillis = 300))
//    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .clickable { onExpandToggle() }
//        ) {
//            Column(
//                modifier = Modifier.padding(
//                    start = 16.dp,
//                    top = 16.dp,
//                    bottom = 12.dp,
//                    end = 80.dp
//                ),
//                verticalArrangement = Arrangement.Center
//            ) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(4.dp)
//                ) {
//                    Text(
//                        text = remember(alert.event) { alert.event.uppercase() },
//                        style = MaterialTheme.typography.labelMedium,
//                        color = ribbonColor,
//                        fontWeight = FontWeight.Bold
//                    )
//                    Icon(
//                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
//                        contentDescription = null,
//                        tint = ribbonColor,
//                        modifier = Modifier.size(16.dp)
//                    )
//                }
//
//                Spacer(
//                    modifier = Modifier.height(6.dp)
//                )
//
//                Text(
//                    text = alert.headline,
//                    style = MaterialTheme.typography.titleMedium,
//                    color = MaterialTheme.colorScheme.onSurface,
//                    fontWeight = FontWeight.Bold,
//                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
//                    overflow = TextOverflow.Ellipsis
//                )
//
//                AnimatedVisibility(
//                    visible = isExpanded,
//                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(
//                        animationSpec = tween(
//                            300
//                        )
//                    ),
//                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(
//                        animationSpec = tween(
//                            300
//                        )
//                    )
//                ) {
//                    Column {
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Text(
//                            text = alert.description,
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
//                        )
//                        if (alert.instruction.isNotEmpty()) {
//                            Spacer(modifier = Modifier.height(8.dp))
//                            Text(
//                                text = stringResource(R.string.instructions),
//                                style = MaterialTheme.typography.labelSmall,
//                                color = MaterialTheme.colorScheme.primary,
//                                fontWeight = FontWeight.Bold
//                            )
//                            Text(
//                                text = alert.instruction,
//                                style = MaterialTheme.typography.bodySmall,
//                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
//                            )
//                        }
//                    }
//                }
//            }
//
//            AlertRibbon(
//                severity = alert.severity.uppercase(),
//                ribbonColor = ribbonColor,
//                modifier = Modifier.align(Alignment.TopEnd)
//            )
//        }
//
//        AlertTrackingButton(
//            isTracked = isTracked,
//            onTrackToggle = onTrackToggle,
//            shape = bottomShape
//        )
//    }
//}
//
//@Preview(
//    name = "Light Mode",
//    showBackground = true,
//    backgroundColor = 0xFFE0F7FA
//)
//
//@Preview(
//    name = "Dark Mode",
//    uiMode = Configuration.UI_MODE_NIGHT_YES,
//    showBackground = true,
//    backgroundColor = 0xFF000000
//)
//
//@Preview(showBackground = true)
//@Composable
//fun AlertCardPreview() {
//    FyneCastTheme() {
//        AlertCard(
//            alert = AlertDomainModel(
//                id = "",
//                headline = "",
//                severity = "",
//                event = "",
//                effectiveMillis = 1L,
//                expiresMillis = 1L,
//                description = "",
//                instruction = ""
//            ),
//            isTracked = true,
//            onTrackToggle = {},
//            isExpanded = true,
//            onExpandToggle = {}
//        )
//    }
//}
```

## ui\components\AlertRibbon.kt

```kotlin
package com.mhq.fynecast.alerts.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AlertRibbon(
    severity: String,
    ribbonColor: Color,
    modifier: Modifier = Modifier,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val rotation = if (isRtl) -45f else 45f
    val horizontalOffset = if (isRtl) (-38).dp else 38.dp

    Box(
        modifier = modifier
            .offset(x = horizontalOffset, y = 14.dp)
            .rotate(rotation)
            .background(ribbonColor)
            .width(140.dp)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = severity,
            color = if (ribbonColor == Color(0xFFFBC02D)) Color.Black else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            maxLines = 1
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
private fun AlertRibbonPreview() {
    FyneCastTheme(){
        AlertRibbon(
            severity = "SEVERE",
            ribbonColor = Color.Yellow
        )
    }
}
```

## ui\components\AlertTrackingButton.kt

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

@Composable
fun AlertTrackingButton(
    isTracked: Boolean,
    severityColor: Color,
    onTrackToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Elegant color animations to prevent jarring structural flickering
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

//import android.content.res.Configuration
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.NotificationsActive
//import androidx.compose.material.icons.filled.NotificationsOff
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import com.mhq.fynecast.R
//import com.mhq.fynecast.core.ui.theme.FyneCastTheme
//
//@Composable
//fun AlertTrackingButton(
//    shape: RoundedCornerShape,
//    isTracked: Boolean,
//    onTrackToggle: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//
//    val containerColor =
//        if (isTracked)
//            MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
//        else
//            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
//
//    val contentColor =
//        if (isTracked)
//            MaterialTheme.colorScheme.error
//        else
//            MaterialTheme.colorScheme.onPrimary
//
//    val borderColor =
//        if (isTracked)
//            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
//        else
//            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
//
//    Row(
//        horizontalArrangement = Arrangement.Center,
//        verticalAlignment = Alignment.CenterVertically,
//        modifier = modifier
//            .fillMaxWidth()
//            .clip(shape)
//            .background(containerColor)
//            .border(width = 1.dp, color = borderColor, shape = shape)
//            .clickable { onTrackToggle() }
//            .padding(vertical = 12.dp, horizontal = 16.dp)
//    ) {
//        Icon(
//            imageVector =
//                if (isTracked)
//                    Icons.Default.NotificationsOff
//                else
//                    Icons.Default.NotificationsActive,
//            contentDescription = null,
//            tint = contentColor
//        )
//        Spacer(modifier = Modifier.width(8.dp))
//        Text(
//            text =
//                if (isTracked)
//                    stringResource(R.string.cancel_tracking)
//                else
//                    stringResource(R.string.track_this_alert),
//            fontWeight = FontWeight.Bold,
//            style = MaterialTheme.typography.labelLarge,
//            color = contentColor
//        )
//    }
//}
//
//@Preview(
//    name = "Light Mode",
//    showBackground = true,
//    backgroundColor = 0xFFE0F7FA
//)
//
//@Preview(
//    name = "Dark Mode",
//    uiMode = Configuration.UI_MODE_NIGHT_YES,
//    showBackground = true,
//    backgroundColor = 0xFF000000
//)
//
//@Preview
//@Composable
//private fun AlertTrackingButtonPreview() {
//    FyneCastTheme() {
//        AlertTrackingButton(
//            isTracked = true,
//            onTrackToggle = {},
//            shape = RoundedCornerShape(16.dp)
//        )
//    }
//}
```

## ui\components\NotificationsDialog.kt

```kotlin
package com.mhq.fynecast.alerts.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mhq.fynecast.R

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

## ui\screens\AlertsBlank.kt

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
fun AlertsBlankPreview() {
    FyneCastTheme() {
        AlertsBlank()
    }
}
```

## ui\screens\AlertsContainer.kt

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
import com.mhq.fynecast.alerts.ui.AlertsEvent
import com.mhq.fynecast.alerts.ui.AlertsUiState
import com.mhq.fynecast.alerts.ui.AlertsViewModel
import com.mhq.fynecast.alerts.ui.components.NotificationsDialog
import kotlinx.coroutines.launch

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
                    // Denied with no rationale available means "don't ask again": only settings can fix it.
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

## ui\screens\AlertsContent.kt

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.ui.components.AlertCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

private val ExpandedAlertIdsSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() }
)

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

@Preview(showBackground = true)
@Composable
fun AlertsContentPreview() {
    FyneCastTheme() {
        AlertsContent(
            alerts = emptyList(),
            trackedAlertIds = emptyList(),
            onTrackToggle = {}
        )
    }
}
```

