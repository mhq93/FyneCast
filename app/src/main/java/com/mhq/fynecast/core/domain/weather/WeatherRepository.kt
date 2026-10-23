package com.mhq.fynecast.core.domain.weather

interface WeatherRepository {
    suspend fun fetchWeather(query: String, lang: String): WeatherDomainModel
}