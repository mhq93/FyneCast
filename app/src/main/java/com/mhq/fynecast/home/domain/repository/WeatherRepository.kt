package com.mhq.fynecast.home.domain.repository

import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import kotlinx.coroutines.flow.StateFlow

interface WeatherRepository {
    val weatherDataCache: StateFlow<WeatherDomainModel?>
    suspend fun provideWeatherData(queryCity: String, lang: String): WeatherDomainModel
}