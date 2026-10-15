package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.home.domain.repository.CurrentWeatherStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAlertsUseCase(private val currentWeatherStore: CurrentWeatherStore) {
    operator fun invoke(): Flow<List<AlertDomainModel>?> =
        currentWeatherStore.current.map { it?.alerts }
}