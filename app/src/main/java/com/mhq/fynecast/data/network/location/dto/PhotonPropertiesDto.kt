package com.mhq.fynecast.data.network.location.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhotonPropertiesDto(
    val name: String? = null,
    val city: String? = null,    // Ensure this is String, not a class
    val town: String? = null,    // Ensure this is String, not a class
    val state: String? = null,
    val country: String? = null,
    val type: String? = null,
    @SerialName("osm_value")
    val osmValue: String? = null
)

//@Serializable
//data class PhotonProperties(
//    val name: String? = null,
//    val city: String? = null,
//    val town: String? = null,
//    val type: String? = null,
//    val village: String? = null,
//    val locality: String? = null, // Add this
//    val district: String? = null, // Add this
//    val state: String? = null,
//    val country: String? = null
//)
