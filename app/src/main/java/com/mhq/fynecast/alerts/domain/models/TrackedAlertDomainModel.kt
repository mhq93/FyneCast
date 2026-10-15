package com.mhq.fynecast.alerts.domain.models

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
    val location: TrackedLocation
        get() = TrackedLocation(cityName, latitude, longitude)
}