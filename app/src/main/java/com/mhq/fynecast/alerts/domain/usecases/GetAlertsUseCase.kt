package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.home.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAlertsUseCase(
    private val weatherRepository: WeatherRepository
) {
    operator fun invoke(): Flow<List<AlertDomainModel>> =
        weatherRepository.weatherDataCache
            .map { weather -> weather?.alerts ?: emptyList() }
}