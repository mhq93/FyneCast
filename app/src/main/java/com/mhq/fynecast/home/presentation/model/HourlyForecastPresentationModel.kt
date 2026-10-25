package com.mhq.fynecast.home.presentation.model

import java.time.LocalDateTime

data class HourlyForecastPresentationModel(
    val time: LocalDateTime,
    val iconUrl: String,
    val tempC: Double,
    val tempF: Double
)
