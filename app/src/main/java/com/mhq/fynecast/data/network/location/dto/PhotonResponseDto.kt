package com.mhq.fynecast.data.network.location.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhotonResponseDto(
    @SerialName("features")
    val features: List<PhotonFeatureDto>
)