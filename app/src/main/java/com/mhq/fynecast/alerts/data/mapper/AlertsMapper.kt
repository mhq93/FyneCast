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