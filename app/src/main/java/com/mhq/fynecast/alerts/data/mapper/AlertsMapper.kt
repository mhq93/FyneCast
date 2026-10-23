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