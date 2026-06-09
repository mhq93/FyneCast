package com.mhq.fynecast.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class CitySuggestionModel(
    val fullName: String,
    val latitude: Double,
    val longitude: Double
)