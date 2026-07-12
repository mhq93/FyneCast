package com.mhq.fynecast.home.data.repoimpl.weather

import android.util.Log
import com.mhq.fynecast.BuildConfig
import com.mhq.fynecast.home.data.api.weather.WeatherApiService
import com.mhq.fynecast.home.data.mapper.toDomain
import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WeatherRepoImpl(
    private val weatherApiService: WeatherApiService
) : WeatherRepository {

    private val _weatherDataCache = MutableStateFlow<WeatherDomainModel?>(null)
    override val weatherDataCache: StateFlow<WeatherDomainModel?> = _weatherDataCache.asStateFlow()

    override suspend fun provideWeatherData(queryCity: String, lang: String): WeatherDomainModel {
        val apiResponse = weatherApiService.getWeatherData(
            key = BuildConfig.WEATHER_API_KEY,
            city = queryCity,
            languageCode = lang
        )

        val domainResult = apiResponse.toDomain()
        _weatherDataCache.value = domainResult
        return domainResult
    }
}