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