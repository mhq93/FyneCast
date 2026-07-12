package com.mhq.fynecast.home.domain.models

data class DailyForecastDomainModel(
    val dayName: String,       // Pre-formatted day identifier string (e.g., "Mon")
    val monthDayStr: String,   // Pre-formatted calendar day representation string (e.g., "Jun 18")
    val iconUrl: String,       // Decoupled graphic asset target path string
    val description: String,   // Localized weather synopsis text description
    val maxTempC: Double,
    val maxTempF: Double,
    val minTempC: Double,
    val minTempF: Double
)