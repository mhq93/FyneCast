package com.mhq.fynecast.home.data.api.location.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhotonResponseDto(
    @SerialName("features")
    val features: List<PhotonFeatureDto>
)