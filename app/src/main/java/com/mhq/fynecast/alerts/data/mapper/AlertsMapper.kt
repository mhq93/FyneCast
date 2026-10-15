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
        headline = headline,
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
        headline = headline,
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