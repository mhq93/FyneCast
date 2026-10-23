package com.mhq.fynecast.home.data.mapper

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.home.data.api.weather.dto.AlertDto
import java.time.OffsetDateTime

/**
 * Calculates a unique string identifier for an alert based on its event type, effective timestamp, and affected areas.
 */
fun AlertDto.calculateId(): String = "${event}_${effective}_$areas"

/**
 * Maps an [AlertDto] from the weather API response into the domain model [AlertDomainModel].
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