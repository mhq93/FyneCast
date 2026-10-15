package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.home.domain.repository.CurrentWeatherStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCurrentCityNameUseCase(private val currentWeatherStore: CurrentWeatherStore) {
    operator fun invoke(): Flow<String> =
        currentWeatherStore.current.map { it?.cityName ?: "Unknown Location" }
}