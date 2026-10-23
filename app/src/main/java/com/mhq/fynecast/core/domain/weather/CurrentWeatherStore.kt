package com.mhq.fynecast.core.domain.weather

import kotlinx.coroutines.flow.StateFlow

interface CurrentWeatherStore {
    val current: StateFlow<WeatherDomainModel?>
    fun publish(weather: WeatherDomainModel)
}