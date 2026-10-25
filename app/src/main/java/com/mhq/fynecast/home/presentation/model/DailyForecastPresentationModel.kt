package com.mhq.fynecast.home.presentation.model

import java.time.LocalDate

data class DailyForecastPresentationModel(
    val date: LocalDate,
    val iconUrl: String,
    val description: String,
    val maxTempC: Double,
    val maxTempF: Double,
    val minTempC: Double,
    val minTempF: Double
)
