package com.mhq.fynecast.alerts.domain.models

data class AlertDomainModel(
    val id: String,
    val headline: String,
    val severity: String,
    val event: String,
    val effectiveMillis: Long,
    val expiresMillis: Long,
    val description: String,
    val instruction: String
)