package com.mhq.fynecast.data.repository

import com.mhq.fynecast.data.network.weather.WeatherApiResponse
import kotlinx.coroutines.flow.StateFlow

interface AppRepository {
    val weatherDataCache: StateFlow<WeatherApiResponse?>
    suspend fun provideWeatherData(queryCity: String): WeatherApiResponse
}