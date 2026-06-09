package com.mhq.fynecast.data.network.location.dto

import kotlinx.serialization.Serializable

@Serializable
data class PhotonGeometryDto(
    val coordinates: List<Double>
)