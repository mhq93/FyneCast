package com.mhq.fynecast.home.domain.models

data class DailyForecastDomainModel(
    val dayName: String,
    val monthDayStr: String,
    val iconUrl: String,
    val description: String,
    val maxTempC: Double,
    val maxTempF: Double,
    val minTempC: Double,
    val minTempF: Double
)