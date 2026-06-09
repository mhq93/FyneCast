package com.mhq.fynecast.data.network.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DayDto(
    @SerialName("maxtemp_c")
    val maxTempC: Double,
    @SerialName("mintemp_c")
    val minTempC: Double,
    @SerialName("maxtemp_f")
    val maxTempF: Double,
    @SerialName("mintemp_f")
    val minTempF: Double,
    @SerialName("maxwind_kph")
    val maxWindKph: Double,
    @SerialName("maxwind_mph")
    val maxWindMph: Double,
    @SerialName("avghumidity")
    val avgHumidity: Int,
    @SerialName("condition")
    val dayCondition: ConditionDto,
    @SerialName("uv")
    val dayUv: Double
)