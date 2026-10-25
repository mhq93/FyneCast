package com.mhq.fynecast.home.data.datasource.weather

import com.mhq.fynecast.home.data.api.weather.dto.WeatherApiResponse

/** Where weather comes from over the network. Returns raw API data, never domain models. */
interface WeatherRemoteDataSource {
    suspend fun fetchWeather(query: String, lang: String): WeatherApiResponse
}