package com.mhq.fynecast.home.presentation.model

data class WeatherStatsPresentationModel(
    val humidity: Int,
    val windKph: Double,
    val windMph: Double,
    val pressureMb: Double,
    val pressureIn: Double,
    val uvIndex: Double
)
