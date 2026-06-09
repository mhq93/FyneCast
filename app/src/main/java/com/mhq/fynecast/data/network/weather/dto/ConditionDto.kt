package com.mhq.fynecast.data.network.weather.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConditionDto(
    @SerialName("text")
    val description: String,
    @SerialName("icon")
    val icon: String
)
