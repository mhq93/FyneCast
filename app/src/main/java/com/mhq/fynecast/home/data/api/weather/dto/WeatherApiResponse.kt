package com.mhq.fynecast.home.data.api.weather.dto

import com.mhq.fynecast.alerts.data.dto.AlertsDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherApiResponse(
    @SerialName("location")
    val location: LocationDto,
    @SerialName("current")
    val current: CurrentDto,
    @SerialName("forecast")
    val forecast: ForecastDto,
    @SerialName("alerts")
    val alerts: AlertsDto? = null
)