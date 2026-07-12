package com.mhq.fynecast.alerts.data.mapper

import com.mhq.fynecast.alerts.data.database.TrackedAlertEntity
import com.mhq.fynecast.alerts.data.dto.AlertDto
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import java.time.OffsetDateTime

private fun parseToEpochMillis(isoString: String): Long =
    runCatching { OffsetDateTime.parse(isoString).toInstant().toEpochMilli() }
        .getOrDefault(0L)

fun AlertDto.calculateId(): String {
    val effectiveMillis = parseToEpochMillis(this.effective)
    val contentHash = "$headline|$description|$instruction".hashCode()
    return "${this.event}_${effectiveMillis}_$contentHash"
}

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

fun AlertDomainModel.toEntity(cityName: String): TrackedAlertEntity {
    return TrackedAlertEntity(
        id = this.id,
        event = this.event,
        cityName = cityName,
        effectiveTimeMillis = this.effectiveMillis,
        expiresTimeMillis = this.expiresMillis
    )
}

fun TrackedAlertEntity.toDomain(): TrackedAlertDomainModel {
    return TrackedAlertDomainModel(
        id = this.id,
        event = this.event,
        cityName = this.cityName,
        effectiveTimeMillis = this.effectiveTimeMillis,
        expiresTimeMillis = this.expiresTimeMillis
    )
}