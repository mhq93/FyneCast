package com.mhq.fynecast.data.network.weather

import com.mhq.fynecast.data.network.weather.dto.AlertsDto
import com.mhq.fynecast.data.network.weather.dto.CurrentDto
import com.mhq.fynecast.data.network.weather.dto.ForecastDto
import com.mhq.fynecast.data.network.weather.dto.LocationDto
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