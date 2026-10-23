package com.mhq.fynecast.core.domain.weather

import java.time.LocalDateTime

data class WeatherDomainModel(
    val cityName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val currentTempC: Double,
    val currentTempF: Double,
    val conditionText: String,
    val conditionIcon: String,
    val lastUpdated: LocalDateTime?,
    val timeZoneId: String?,
    val alerts: List<AlertDomainModel>,
    val stats: WeatherStatsDomainModel,
    val dailyForecasts: List<DailyForecastDomainModel>,
    val hourlyForecasts: List<HourlyForecastDomainModel>
)