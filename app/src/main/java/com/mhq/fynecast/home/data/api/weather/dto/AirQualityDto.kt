package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AirQualityDto(
    @SerialName("us-epa-index")
    val usEpaIndex: Int? = null,
    @SerialName("pm2_5")
    val pm25: Double? = null,
    @SerialName("pm10")
    val pm10: Double? = null
)