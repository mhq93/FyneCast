package com.mhq.fynecast.home.domain.models

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel

data class WeatherDomainModel(
    val cityName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val currentTempC: Double,
    val currentTempF: Double,
    val conditionText: String,
    val conditionIcon: String,
    val alerts: List<AlertDomainModel>,
    val stats: WeatherStatsDomainModel,
    val dailyForecasts: List<DailyForecastDomainModel>,
    val hourlyForecasts: List<HourlyForecastDomainModel>
)