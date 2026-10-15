package com.mhq.fynecast.home.domain.repository

import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import kotlinx.coroutines.flow.StateFlow

interface WeatherRepository {
    suspend fun fetchWeather(query: String, lang: String): WeatherDomainModel
}