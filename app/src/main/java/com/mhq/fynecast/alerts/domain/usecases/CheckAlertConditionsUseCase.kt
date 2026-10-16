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