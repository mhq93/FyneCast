package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastDayDto(
    @SerialName("date")
    val date: String,
    @SerialName("day")
    val day: DayDto,
    @SerialName("hour")
    val hour: List<HourDto>
)