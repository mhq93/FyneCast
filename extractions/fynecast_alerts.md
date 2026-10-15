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

fun AlertDto.toDomain(): AlertDomainModel {
    val effectiveMillis = parseToEpochMillis(this.effective)
    val expiresMillis = parseToEpochMillis(this.expires)

    return AlertDomainModel(
        id = this.calculateId(),
        headline = this.headline,
        severity = this.severity,
        event = this.event,
        description = this.description,
        instruction = this.instruction,
        effectiveMillis = effectiveMillis,
        expiresMillis = expiresMillis
    )
}

fun AlertDomainModel.toEntity(location: TrackedLocation) =
    TrackedAlertEntity(
        id = id,
        cityName = location.trackedLocationName,
        latitude = location.trackedLocationLatitude,
        longitude = location.trackedLocationLongitude,
        event = event,
        effectiveTimeMillis = effectiveMillis,
        expiresTimeMillis = expiresMillis
    )

fun TrackedAlertEntity.toDomain() =
    TrackedAlertDomainModel(
        id = id,
        cityName = cityName,
        latitude = latitude,
        longitude = longitude,
        event = event,
        effectiveTimeMillis = effectiveTimeMillis,
        expiresTimeMillis = expiresTimeMillis
    )

private fun parseToEpochMillis(isoString: String): Long =
    runCatching {
        OffsetDateTime
            .parse(isoString)
            .toInstant()
            .toEpochMilli()
    }
        .getOrDefault(0L)
```

## data\repoimpl\AlertServiceControllerImpl.kt

```kotlin
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

## domain\models\AlertDomainModel.kt

```kotlin
package com.mhq.fynecast.alerts.domain.models

data class AlertDomainModel(
    val id: String,
    val headline: String,
    val severity: String,
    val event: String,
    val effectiveMillis: Long,
    val expiresMillis: Long,
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

## domain\repository\AlertServiceController.kt

```kotlin
package com.mhq.fynecast.alerts.domain.repository

import com.mhq.fynecast.alerts.domain.models.TrackedLocation

interface AlertServiceController {
    fun startTracking(alertId: String, event: String, headline: String)
    fun scheduleAlertTracking(alertId: String)
    fun scheduleLiftedCheck(trackedLocation: TrackedLocation)
    fun cancelLiftedCheck(trackedLocation: TrackedLocation)
    fun removeAlertFromTracking(alertId: String)
    fun cancelTrackingWorker(alertId: String)
    fun stopTracking()
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

## domain\usecases\CancelAllAlertTrackingUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertServiceController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class CancelAllAlertTrackingUseCase(
    private val alertsRepository: AlertsRepository,
    private val alertServiceController: AlertServiceController
) {
    suspend operator fun invoke() = withContext(NonCancellable) {
        val trackedAlerts = alertsRepository.getTrackedAlerts()

        trackedAlerts.forEach {
            alertServiceController.cancelTrackingWorker(it.id)
        }

        trackedAlerts
            .map { it.location }
            .distinct()
            .forEach {
                alertServiceController.cancelLiftedCheck(it)
            }

        alertsRepository.deleteAllTrackedAlerts()
        alertServiceController.stopTracking()
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

## domain\usecases\RestoreAlertTrackingUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertServiceController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.core.domain.time.TimeProvider

class RestoreAlertTrackingUseCase(
    private val alertsRepository: AlertsRepository,
    private val serviceController: AlertServiceController,
    private val untrackAlertUseCase: UntrackAlertUseCase,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke() {
        val now = timeProvider.nowMillis()

        val (expired, live) =
            alertsRepository.getTrackedAlerts()
                .partition { now >= it.expiresTimeMillis }

        expired.forEach {
            untrackAlertUseCase(it.id)
        }

        live.forEach {
            serviceController.startTracking(it.id, it.event, it.event)
            serviceController.scheduleAlertTracking(it.id)
        }

        live.map { it.location }.distinct().forEach {
            serviceController.scheduleLiftedCheck(it)
        }
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
    suspend operator fun invoke(
        alert: AlertDomainModel,
        trackedLocation: TrackedLocation
    ) {
        if (alert.id in alertsRepository.getTrackedAlertIds().first())
            untrackAlertUseCase(alert.id)
        else
            trackAlertUseCase(alert, trackedLocation)
    }
}
```

## domain\usecases\TrackAlertUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.alerts.domain.repository.AlertServiceController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.core.domain.time.TimeProvider

class TrackAlertUseCase(
    private val alertsRepository: AlertsRepository,
    private val serviceController: AlertServiceController,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(alert: AlertDomainModel, trackedLocation: TrackedLocation): Boolean {
        if (alert.expiresMillis <= timeProvider.nowMillis()) return false

        alertsRepository.saveTrackedAlert(alert, trackedLocation)
        serviceController.startTracking(alert.id, alert.event, alert.headline)
        serviceController.scheduleAlertTracking(alert.id)
        serviceController.scheduleLiftedCheck(trackedLocation)
        return true
    }
}
```

## domain\usecases\UntrackAlertUseCase.kt

```kotlin
package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertServiceController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository

class UntrackAlertUseCase(
    private val alertsRepository: AlertsRepository,
    private val serviceController: AlertServiceController
) {
    suspend operator fun invoke(alertId: String) {
        val removed = alertsRepository.getTrackedAlerts().firstOrNull { it.id == alertId }
        alertsRepository.deleteTrackedAlert(alertId)

        val remainingAlerts = alertsRepository.getTrackedAlerts()

        if (remainingAlerts.isEmpty())
            serviceController.stopTracking()
        else
            serviceController.removeAlertFromTracking(alertId)

        if (
            removed != null && remainingAlerts.none {
                it.latitude == removed.latitude && it.longitude == removed.longitude
            }
        ) {
            serviceController.cancelLiftedCheck(removed.location)
        }

        serviceController.cancelTrackingWorker(alertId)
    }
}
```

## notifications\AlertsForegroundService.kt

```kotlin
package com.mhq.fynecast.alerts.notifications

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.mhq.fynecast.alerts.notifications.data.infra.AlertsNotificationManager
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AlertsForegroundService : LifecycleService() {

    private val appContainer by lazy { (applicationContext as FyneCastApplication).container }
    private val notificationManager by lazy { AlertsNotificationManager(this) }
    private val activeAlerts = ConcurrentHashMap<String, Pair<String, String>>()

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        postSummary()

        lifecycleScope.launch {
            appContainer.observeUserPreferencesUseCase()
                .map { it.notificationsEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (!enabled) {
                        activeAlerts.clear()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent == null) return cleanStartCheck()

        when (intent.action) {
            ACTION_START -> {
                val alertId = intent.getStringExtra(KEY_ALERT_ID) ?: return cleanStartCheck()
                val event = intent.getStringExtra(KEY_EVENT) ?: return cleanStartCheck()
                val headline = intent.getStringExtra(KEY_HEADLINE) ?: return cleanStartCheck()

                if (!appContainer.notificationPermissionChecker.isGranted()) {
                    return cleanStartCheck()
                }

                activeAlerts[alertId] = event to headline
                postSummary()
            }

            ACTION_REMOVE_ALERT -> {
                val alertId = intent.getStringExtra(KEY_ALERT_ID)
                if (alertId != null) activeAlerts.remove(alertId)

                if (activeAlerts.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    postSummary()
                }
            }

            ACTION_CANCEL_ALL -> lifecycleScope.launch(Dispatchers.IO) {
                appContainer.cancelAllAlertTrackingUseCase()
            }
        }
        return START_NOT_STICKY
    }

    private fun postSummary() {
        val notification = notificationManager.buildTrackingNotification(activeAlerts.values.toList())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun cleanStartCheck(): Int {
        if (activeAlerts.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        isRunning = false; super.onDestroy()
    }

    companion object {
        @Volatile
        var isRunning = false
            private set

        const val ACTION_START = "com.mhq.fynecast.action.ALERT_START"
        const val ACTION_STOP = "com.mhq.fynecast.action.ALERT_STOP"
        const val ACTION_REMOVE_ALERT = "com.mhq.fynecast.action.ACTION_REMOVE_ALERT"
        const val ACTION_CANCEL_ALL = "com.mhq.fynecast.action.ALERT_CANCEL_ALL"
        const val KEY_ALERT_ID = "alert_id"
        const val KEY_EVENT = "event"
        const val KEY_HEADLINE = "headline"
        const val FOREGROUND_NOTIFICATION_ID = 1001

        fun removeAlert(context: Context, alertId: String) {
            if (!isRunning) return

            val intent = Intent(
                context,
                AlertsForegroundService::class.java
            ).apply {
                action = ACTION_REMOVE_ALERT
                putExtra(KEY_ALERT_ID, alertId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun start(
            context: Context,
            alertId: String,
            event: String,
            headline: String
        ) {
            val intent = Intent(
                context,
                AlertsForegroundService::class.java
            ).apply {
                action = ACTION_START
                putExtra(KEY_ALERT_ID, alertId)
                putExtra(KEY_EVENT, event)
                putExtra(KEY_HEADLINE, headline)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(
                context,
                AlertsForegroundService::class.java
            ).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
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

## notifications\data\infra\AlertsNotificationManager.kt

```kotlin
package com.mhq.fynecast.alerts.notifications.data.infra

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.mhq.fynecast.R
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.notifications.AlertsForegroundService
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.core.ui.MainActivity

class AlertsNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    fun buildTrackingNotification(activeAlerts: List<Pair<String, String>>): Notification {
        val title = if (activeAlerts.size == 1) {
            "⚠️ Live Tracking: ${activeAlerts.first().first}"
        } else {
            "⚠️ Tracking ${activeAlerts.size} Weather Alerts"
        }

        val body = activeAlerts.joinToString("\n") { "• ${it.second}" }

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val clickPendingIntent = PendingIntent.getActivity(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelAllIntent = Intent(
            context,
            AlertsForegroundService::class.java
        ).apply {
            action = AlertsForegroundService.ACTION_CANCEL_ALL
        }

        val cancelAllPendingIntent = PendingIntent.getService(
            context,
            0,
            cancelAllIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, FyneCastApplication.CHANNEL_ALERT_TRACKING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(if (activeAlerts.size == 1) activeAlerts.first().second else "Expand to view tracked alerts")
            .setContentIntent(clickPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .addAction(
                R.drawable.ic_cancel,
                "Cancel Tracking",
                cancelAllPendingIntent
            )
            .build()
    }

    fun showAlarmNotification(
        clearedAlerts: List<TrackedAlertDomainModel>,
        notificationId: Int
    ) {
        val title = if (clearedAlerts.size == 1) {
            "${clearedAlerts.first().event} has been lifted"
        } else {
            "${clearedAlerts.size} weather alerts have been lifted"
        }

        val body = if (clearedAlerts.size == 1) {
            "The ${clearedAlerts.first().event} warning for ${clearedAlerts.first().cityName} is no longer active"
        } else {
            clearedAlerts.joinToString(", ") { it.event } + " warnings have been cleared"
        }

        val notification = NotificationCompat
            .Builder(context, FyneCastApplication.CHANNEL_ALERT_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("✅ $title")
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(notificationId, notification)
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

## notifications\WeatherAlertTrackingWorker.kt

```kotlin
package com.mhq.fynecast.alerts.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertStatusUseCase
import com.mhq.fynecast.alerts.notifications.data.infra.AlertsNotificationManager
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
import com.mhq.fynecast.alerts.notifications.data.infra.AlertsNotificationManager
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
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentLocationUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlertsViewModel(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val toggleAlertsUseCase: ToggleAlertsUseCase,
    private val getTrackedAlertIdsUseCase: GetTrackedAlertIdsUseCase,
    private val observeCurrentLocationUseCase: ObserveCurrentLocationUseCase
) : ViewModel() {

    val uiState: StateFlow<AlertsUiState> = getAlertsUseCase()
        .map {
            alerts -> if (alerts == null) AlertsUiState.Loading else AlertsUiState.Success(alerts)
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

    fun onAlertTrackToggled(alert: AlertDomainModel) {
        val location = currentLocation.value ?: return
        viewModelScope.launch { toggleAlertsUseCase(alert, location) }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)
                AlertsViewModel(
                    getAlertsUseCase = app.container.getAlertsUseCase,
                    toggleAlertsUseCase = app.container.toggleAlertsUseCase,
                    getTrackedAlertIdsUseCase = app.container.getTrackedAlertIdsUseCase,
                    observeCurrentLocationUseCase = app.container.observeCurrentLocationUseCase
                )
            }
        }
    }
}
```

## ui\components\AlertCard.kt

```kotlin
package com.mhq.fynecast.alerts.ui.components

import android.content.res.Configuration
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
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
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
    // 1. Establish premium, adaptive colors based on severity
    val severityColor = remember(alert.severity) {
        when (alert.severity.lowercase()) {
            "extreme" -> Color(0xFFFF3B30) // Premium Deep Apple Red, was MaterialTheme.colorScheme.error
            "severe" -> Tangerine
            "moderate" -> VividAmber
            else -> Color(0xFF007AFF) // Editorial Royal Blue, was MaterialTheme.colorScheme.tertiary
        }
    }

    // Gentle container background tinted by severity to reduce stark contrast
    val cardBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val borderGradient = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f),
            severityColor.copy(alpha = 0.05f)
        )
    )

    Card(
        shape = RoundedCornerShape(24.dp), // More modern, aggressive corner rounding
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
                .background(cardBackground)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.03f), Color.Transparent)))
                // Seamlessly embedded premium micro-border
                .then(Modifier.background(borderGradient, RoundedCornerShape(24.dp)))
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
                            .background(severityColor, RoundedCornerShape(50))
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

                // Minimalistic pill badge replacing the old corner ribbon layout
                Box(
                    modifier = Modifier
                        .background(severityColor.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
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

            // Premium Tracking Interactive layer
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.alerts.ui.AlertsUiState
import com.mhq.fynecast.alerts.ui.AlertsViewModel

@Composable
fun AlertsContainer(
    alertsViewModel: AlertsViewModel = viewModel(factory = AlertsViewModel.factory),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {

    val uiState by alertsViewModel.uiState.collectAsStateWithLifecycle()
    val trackedAlertIds by alertsViewModel.trackedAlertIds.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is AlertsUiState.Loading -> {
            Box(
                modifier = modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is AlertsUiState.Success -> {
            if (state.alerts.isEmpty()) {
                AlertsBlank(
                    contentPadding = contentPadding,
                    modifier = modifier
                )
            } else {
                AlertsContent(
                    alerts = state.alerts,
                    trackedAlertIds = trackedAlertIds,
                    onTrackToggle = { alertsViewModel.onAlertTrackToggled(it) },
                    contentPadding = contentPadding,
                    modifier = modifier
                )
            }
        }
    }
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

