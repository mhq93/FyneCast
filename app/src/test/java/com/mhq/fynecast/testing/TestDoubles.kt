package com.mhq.fynecast.testing

import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.core.domain.location.TrackedLocation
import com.mhq.fynecast.core.domain.time.TimeProvider
import com.mhq.fynecast.core.domain.weather.AlertDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherStatsDomainModel
import com.mhq.fynecast.settings.domain.models.AppLanguage
import com.mhq.fynecast.settings.domain.models.UserPreferences
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Shared, ordered record of what the fakes were asked to do. */
class EventLog {
    val events = mutableListOf<String>()
    fun add(event: String) { events += event }
}

class FakeTimeProvider(var now: Long = 0L) : TimeProvider {
    override fun nowMillis(): Long = now
}

class FakeNotificationPermissionChecker(var granted: Boolean = true) : NotificationPermissionChecker {
    override fun isGranted(): Boolean = granted
}

class FakeAlertsRepository(private val log: EventLog = EventLog()) : AlertsRepository {
    val tracked = mutableListOf<TrackedAlertDomainModel>()
    var failOnDeleteAll = false

    override fun getTrackedAlertIds(): Flow<List<String>> = flow { emit(tracked.map { it.id }) }

    override suspend fun getTrackedAlerts(): List<TrackedAlertDomainModel> = tracked.toList()

    override suspend fun saveTrackedAlert(alert: AlertDomainModel, location: TrackedLocation) {
        tracked.removeAll { it.id == alert.id }
        tracked += TrackedAlertDomainModel(
            id = alert.id,
            cityName = location.trackedLocationName,
            latitude = location.trackedLocationLatitude,
            longitude = location.trackedLocationLongitude,
            headline = alert.headline,
            event = alert.event,
            effectiveTimeMillis = alert.effectiveMillis,
            expiresTimeMillis = alert.expiresMillis ?: 0L
        )
    }

    override suspend fun deleteTrackedAlert(alertId: String) {
        tracked.removeAll { it.id == alertId }
    }

    override suspend fun deleteAllTrackedAlerts() {
        log.add("repo:deleteAll")
        if (failOnDeleteAll) throw IllegalStateException("delete failed")
        tracked.clear()
    }
}

class FakeAlertTrackingController(private val log: EventLog = EventLog()) : AlertTrackingController {
    val shownNotifications = mutableListOf<List<TrackedAlertDomainModel>>()
    val scheduledWorkers = mutableListOf<String>()
    val cancelledWorkers = mutableListOf<String>()
    val scheduledLiftedChecks = mutableListOf<TrackedLocation>()
    val cancelledLiftedChecks = mutableListOf<TrackedLocation>()

    override fun showTrackingNotification(alerts: List<TrackedAlertDomainModel>) {
        shownNotifications += alerts
    }

    override fun scheduleAlertTracking(alertId: String) { scheduledWorkers += alertId }

    override fun cancelTrackingWorker(alertId: String) {
        log.add("controller:cancelWorker:$alertId")
        cancelledWorkers += alertId
    }

    override fun scheduleLiftedCheck(trackedLocation: TrackedLocation) { scheduledLiftedChecks += trackedLocation }

    override fun cancelLiftedCheck(trackedLocation: TrackedLocation) { cancelledLiftedChecks += trackedLocation }
}

class FakeUserPreferencesRepository(
    initial: UserPreferences = preferences(),
    private val log: EventLog = EventLog()
) : UserPreferencesRepository {
    var current: UserPreferences = initial

    override val userPreferencesFlow: Flow<UserPreferences> = flow { emit(current) }

    override suspend fun getPreferences(): UserPreferences = current
    override suspend fun updateMetricSystem(isMetric: Boolean) { current = current.copy(isMetric = isMetric) }
    override suspend fun updateNotifications(enabled: Boolean) {
        log.add("prefs:notifications=$enabled")
        current = current.copy(notificationsEnabled = enabled)
    }
    override suspend fun updateLanguage(language: AppLanguage) { current = current.copy(currentLanguage = language) }
    override suspend fun updateDarkMode(enabled: Boolean) { current = current.copy(isDarkMode = enabled) }
    override suspend fun updateLastSearchedCity(name: String, lat: Double, lon: Double) {
        current = current.copy(lastSearchedCityName = name, lastSearchedCityLat = lat, lastSearchedCityLon = lon)
    }
    override suspend fun clearLastSearchedCity() {
        current = current.copy(lastSearchedCityName = null, lastSearchedCityLat = null, lastSearchedCityLon = null)
    }
    override suspend fun consumeNotificationPrompt(): Boolean = false
}

// ---- Test data builders ----

fun preferences(notificationsEnabled: Boolean = true) = UserPreferences(
    isMetric = true,
    notificationsEnabled = notificationsEnabled,
    currentLanguage = AppLanguage.English,
    isDarkMode = null
)

fun alert(id: String = "alert-1", expiresMillis: Long? = 10_000L) = AlertDomainModel(
    id = id,
    headline = "Heavy rain",
    severity = "Moderate",
    event = "Rain",
    effectiveMillis = 0L,
    expiresMillis = expiresMillis,
    description = "",
    instruction = ""
)

fun location(name: String = "Cairo", lat: Double = 30.0, lon: Double = 31.0) =
    TrackedLocation(name, lat, lon)

fun tracked(
    id: String,
    lat: Double = 30.0,
    lon: Double = 31.0,
    expiresMillis: Long = 10_000L
) = TrackedAlertDomainModel(
    id = id,
    cityName = "Cairo",
    latitude = lat,
    longitude = lon,
    headline = "Heavy rain",
    event = "Rain",
    effectiveTimeMillis = 0L,
    expiresTimeMillis = expiresMillis
)

fun weather(
    lat: Double = 30.0,
    lon: Double = 31.0,
    alerts: List<AlertDomainModel> = emptyList()
) = WeatherDomainModel(
    cityName = "Cairo",
    country = "Egypt",
    latitude = lat,
    longitude = lon,
    currentTempC = 25.0,
    currentTempF = 77.0,
    conditionText = "Sunny",
    conditionIcon = "",
    lastUpdated = null,
    timeZoneId = null,
    alerts = alerts,
    stats = WeatherStatsDomainModel(
        humidity = 40,
        windKph = 10.0,
        windMph = 6.2,
        pressureMb = 1013.0,
        pressureIn = 29.9,
        uvIndex = 5.0
    ),
    dailyForecasts = emptyList(),
    hourlyForecasts = emptyList()
)
