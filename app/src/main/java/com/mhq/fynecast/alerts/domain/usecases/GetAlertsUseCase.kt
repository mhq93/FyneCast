package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.core.domain.weather.AlertDomainModel
import com.mhq.fynecast.core.domain.weather.CurrentWeatherStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case that streams current weather alerts from the weather store.
 */
class GetAlertsUseCase(private val currentWeatherStore: CurrentWeatherStore) {
    /** Returns a flow of active weather alert lists, or null if unavailable. */
    operator fun invoke(): Flow<List<AlertDomainModel>?> =
        currentWeatherStore.current.map { it?.alerts }
}