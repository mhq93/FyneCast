package com.mhq.fynecast.alerts.domain.models

/**
 * Represents a geographical location being monitored for weather alerts.
 */
data class TrackedLocation(
    val trackedLocationName: String,
    val trackedLocationLatitude: Double,
    val trackedLocationLongitude: Double
) {
    /** The same string every time resolves to the same API location every time. */
    val trackedLocationQuery: String
        get() = "$trackedLocationLatitude,$trackedLocationLongitude"
}