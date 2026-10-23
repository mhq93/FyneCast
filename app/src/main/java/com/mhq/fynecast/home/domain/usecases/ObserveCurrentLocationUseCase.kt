package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.core.domain.location.TrackedLocation
import com.mhq.fynecast.core.domain.weather.CurrentWeatherStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCurrentLocationUseCase(private val currentWeatherStore: CurrentWeatherStore) {
    operator fun invoke(): Flow<TrackedLocation?> =
        currentWeatherStore.current.map { weatherDomainModel ->
            weatherDomainModel?.let { TrackedLocation(it.cityName, it.latitude, it.longitude) }
        }
}