package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.domain.repository.WeatherRepository

class GetWeatherUseCase(private val weatherRepository: WeatherRepository) {
    suspend operator fun invoke(query: String, lang: String): WeatherDomainModel =
        weatherRepository.fetchWeather(query, lang)
}