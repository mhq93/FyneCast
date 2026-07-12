package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.home.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCurrentCityNameUseCase(
    private val weatherRepository: WeatherRepository
) {
    operator fun invoke(): Flow<String> =
        weatherRepository.weatherDataCache.map { it?.cityName ?: "Unknown Location" }
}