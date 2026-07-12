package com.mhq.fynecast.home.domain.models

data class HourlyForecastDomainModel(
    val formattedHour: String,
    val iconUrl: String,
    val tempC: Double,
    val tempF: Double
)