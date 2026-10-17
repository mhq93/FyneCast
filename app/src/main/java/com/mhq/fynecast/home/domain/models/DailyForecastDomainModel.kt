package com.mhq.fynecast.home.domain.models

import java.time.LocalDate

data class DailyForecastDomainModel(
    val date: LocalDate,
    val iconUrl: String,
    val description: String,
    val maxTempC: Double,
    val maxTempF: Double,
    val minTempC: Double,
    val minTempF: Double
)