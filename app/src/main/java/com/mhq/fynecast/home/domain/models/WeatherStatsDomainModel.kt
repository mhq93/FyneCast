package com.mhq.fynecast.home.domain.models

data class WeatherStatsDomainModel(
    val humidity: Int,
    val windKph: Double,
    val windMph: Double,
    val pressureMb: Double,
    val pressureIn: Double,
    val uvIndex: Double
)