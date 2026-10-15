package com.mhq.fynecast.home.data.repoimpl.weather

import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.domain.repository.CurrentWeatherStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryCurrentWeatherStore : CurrentWeatherStore {
    private val _current = MutableStateFlow<WeatherDomainModel?>(null)
    override val current: StateFlow<WeatherDomainModel?> = _current.asStateFlow()
    override fun publish(weather: WeatherDomainModel) { _current.value = weather }
}