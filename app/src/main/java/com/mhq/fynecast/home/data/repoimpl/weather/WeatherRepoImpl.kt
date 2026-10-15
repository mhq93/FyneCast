package com.mhq.fynecast.home.data.repoimpl.weather

import com.mhq.fynecast.BuildConfig
import com.mhq.fynecast.home.data.api.weather.WeatherApiService
import com.mhq.fynecast.home.data.mapper.toDomain
import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.domain.repository.WeatherRepository

class WeatherRepoImpl(
    private val weatherApiService: WeatherApiService
) : WeatherRepository {
    override suspend fun fetchWeather(query: String, lang: String): WeatherDomainModel =
        weatherApiService.getWeatherData(
            key = BuildConfig.WEATHER_API_KEY,
            city = query,
            languageCode = lang
        ).toDomain()
}