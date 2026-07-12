package com.mhq.fynecast.home.data.api.location.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhotonPropertiesDto(
    val name: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val municipality: String? = null,
    val county: String? = null,
    val state: String? = null,
    val country: String? = null,
    val type: String? = null,
    @SerialName("osm_value")
    val osmValue: String? = null
)