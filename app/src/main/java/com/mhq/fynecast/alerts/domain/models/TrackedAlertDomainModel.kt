package com.mhq.fynecast.alerts.domain.models

import com.mhq.fynecast.core.domain.location.TrackedLocation

/**
 * Domain model representing a weather alert that is currently being tracked for a specific location.
 */
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
    /** Derives a [com.mhq.fynecast.core.domain.location.TrackedLocation] instance from the alert's location properties. */
    val location: TrackedLocation
        get() = TrackedLocation(
            cityName,
            latitude,
            longitude
        )
}