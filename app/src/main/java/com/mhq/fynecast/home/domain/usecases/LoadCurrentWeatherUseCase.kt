package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.domain.repository.CurrentWeatherStore
import com.mhq.fynecast.home.domain.repository.WeatherRepository

class LoadCurrentWeatherUseCase(
    private val weatherRepository: WeatherRepository,
    private val currentWeatherStore: CurrentWeatherStore
) {
    suspend operator fun invoke(query: String, lang: String): WeatherDomainModel =
        weatherRepository
            .fetchWeather(query, lang)
            .also(currentWeatherStore::publish)
}