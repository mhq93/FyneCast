package com.mhq.fynecast.core.domain.weather

import java.time.LocalDateTime

data class HourlyForecastDomainModel(
    val time: LocalDateTime,
    val iconUrl: String,
    val tempC: Double,
    val tempF: Double
)