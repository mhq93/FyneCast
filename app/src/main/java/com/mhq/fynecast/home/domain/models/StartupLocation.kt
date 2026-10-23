package com.mhq.fynecast.home.domain.models

data class StartupLocation(
    val query: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val fromDevice: Boolean
)