package com.mhq.fynecast.data.repository

import com.mhq.fynecast.BuildConfig
import com.mhq.fynecast.data.network.weather.WeatherApiResponse
import com.mhq.fynecast.data.network.weather.WeatherApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WeatherApiRepository(private val weatherApiService: WeatherApiService) : AppRepository {
    private val _weatherDataCache = MutableStateFlow<WeatherApiResponse?>(null)
    override val weatherDataCache: StateFlow<WeatherApiResponse?> = _weatherDataCache.asStateFlow()

    override suspend fun provideWeatherData(queryCity: String): WeatherApiResponse {
        val response = weatherApiService.getWeatherData(
            key = BuildConfig.WEATHER_API_KEY,
            city = queryCity
        )

        _weatherDataCache.value = response
        return response
    }

    /*
    override suspend fun provideWeatherData(queryCity: String): WeatherApiResponse {
    val response = weatherApiService.getWeatherData(
        key = BuildConfig.WEATHER_API_KEY,
        city = queryCity
    )

    // FORCE REAL ALERTS FOR TESTING:
    val testAlerts = AlertsDto(
        alert = listOf(
            AlertDto(
                headline = "Severe Flood Warning issued for $queryCity",
                severity = "Severe",
                event = "Flood Warning",
                effective = "2026-06-03T12:00:00",
                expires = "2026-06-04T12:00:00",
                description = "Heavy rainfall has caused rivers to crest over banks. Seek high ground immediately and avoid travelling through flooded zones.",
                instruction = "Move items upstairs. Do not drive through moving water layers."
            )
        )
    )

    // Attach fake alerts directly to the cache stream to force layout testing
    val simulatedResponse = response.copy(alerts = testAlerts)

    _weatherDataCache.value = simulatedResponse
    return simulatedResponse
}

    * */
}