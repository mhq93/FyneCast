package com.mhq.fynecast.home.domain.repository

import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import kotlinx.coroutines.flow.StateFlow

interface CurrentWeatherStore {
    val current: StateFlow<WeatherDomainModel?>
    fun publish(weather: WeatherDomainModel)
}