package com.mhq.fynecast.home.data.api.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationDto(
    @SerialName("name")
    val cityName: String,
    @SerialName("region")
    val cityRegion: String,
    @SerialName("country")
    val country: String,
    @SerialName("lat")
    val cityLatitude: Double,
    @SerialName("lon")
    val cityLongitude: Double,
    @SerialName("localtime")
    val cityLocaltime: String
)