package com.mhq.fynecast.home.presentation.model

import java.time.LocalDateTime

/** What the Home screen needs to show the weather. The UI never sees the domain model. */
data class WeatherPresentationModel(
    val latitude: Double,
    val longitude: Double,
    val currentTempC: Double,
    val currentTempF: Double,
    val conditionText: String,
    val conditionIcon: String,
    val lastUpdated: LocalDateTime?,
    val timeZoneId: String?,
    /** The banner only shows how many alerts there are; the Alerts screen shows the alerts themselves. */
    val alertsCount: Int,
    val stats: WeatherStatsPresentationModel,
    val dailyForecasts: List<DailyForecastPresentationModel>,
    val hourlyForecasts: List<HourlyForecastPresentationModel>
)
