package com.mhq.fynecast.home.data.datasource.weather

import com.mhq.fynecast.home.data.api.weather.WeatherApiService
import com.mhq.fynecast.home.data.api.weather.dto.WeatherApiResponse

class WeatherRemoteDataSourceImpl(
    private val weatherApiService: WeatherApiService,
    private val apiKey: String
) : WeatherRemoteDataSource {

    override suspend fun fetchWeather(query: String, lang: String): WeatherApiResponse =
        weatherApiService.getWeatherData(
            key = apiKey,
            city = query,
            languageCode = lang
        )
}