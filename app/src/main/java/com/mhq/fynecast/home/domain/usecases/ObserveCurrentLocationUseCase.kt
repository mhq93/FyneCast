package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.home.domain.repository.CurrentWeatherStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCurrentLocationUseCase(private val currentWeatherStore: CurrentWeatherStore) {
    operator fun invoke(): Flow<TrackedLocation?> =
        currentWeatherStore.current.map { weatherDomainModel ->
            weatherDomainModel?.let { TrackedLocation(it.cityName, it.latitude, it.longitude) }
        }
}