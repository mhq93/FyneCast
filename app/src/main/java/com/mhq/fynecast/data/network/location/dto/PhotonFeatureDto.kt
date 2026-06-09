package com.mhq.fynecast.data.network.location.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhotonFeatureDto(
    @SerialName("properties")
    val properties: PhotonPropertiesDto,
    @SerialName("geometry")
    val geometry: PhotonGeometryDto
)