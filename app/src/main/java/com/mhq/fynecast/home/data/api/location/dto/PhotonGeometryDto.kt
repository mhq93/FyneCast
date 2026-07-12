package com.mhq.fynecast.home.data.api.location.dto

import kotlinx.serialization.Serializable

@Serializable
data class PhotonGeometryDto(
    val coordinates: List<Double>
)