package com.mhq.fynecast.alerts.domain.models

data class TrackedAlertDomainModel(
    val id: String,
    val cityName: String,
    val event: String,
    val effectiveTimeMillis: Long,
    val expiresTimeMillis: Long
)