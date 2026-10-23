package com.mhq.fynecast.core.domain.weather

data class WeatherStatsDomainModel(
    val humidity: Int,
    val windKph: Double,
    val windMph: Double,
    val pressureMb: Double,
    val pressureIn: Double,
    val uvIndex: Double
)