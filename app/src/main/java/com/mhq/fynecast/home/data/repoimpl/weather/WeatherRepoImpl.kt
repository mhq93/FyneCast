package com.mhq.fynecast.home.data.repoimpl.weather

import com.mhq.fynecast.home.data.api.weather.WeatherApiService
import com.mhq.fynecast.home.data.mapper.toDomain
import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherRepository

class WeatherRepoImpl(
    private val weatherApiService: WeatherApiService,
    private val apiKey: String
) : WeatherRepository {
    override suspend fun fetchWeather(query: String, lang: String): WeatherDomainModel =
        weatherApiService.getWeatherData(
            key = apiKey,
            city = query,
            languageCode = lang
        ).toDomain()
}