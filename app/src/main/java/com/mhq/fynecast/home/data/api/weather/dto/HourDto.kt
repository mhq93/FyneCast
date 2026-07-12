package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HourDto(
    @SerialName("time")
    val hourTime: String,
    @SerialName("temp_c")
    val hourTempC: Double,
    @SerialName("temp_f")
    val hourTempF: Double,
    @SerialName("condition")
    val hourCondition: ConditionDto,
    @SerialName("is_day")
    val isDay: Int,
    @SerialName("humidity")
    val hourHumidity: Int,
    @SerialName("wind_kph")
    val hourWindKph: Double
)