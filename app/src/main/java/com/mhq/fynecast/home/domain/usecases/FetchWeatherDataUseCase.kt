package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.domain.repository.WeatherRepository

class FetchWeatherDataUseCase(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(query: String, lang: String): WeatherDomainModel {
        val weatherDomainModel = weatherRepository.provideWeatherData(query, lang)

        // Textbook Domain Mapping: Protects downstream boundaries from API JSON drift
        return WeatherDomainModel(
            cityName = weatherDomainModel.cityName,
            country = weatherDomainModel.country,
            latitude = weatherDomainModel.latitude,
            longitude = weatherDomainModel.longitude,
            currentTempC = weatherDomainModel.currentTempC,
            currentTempF = weatherDomainModel.currentTempF,
            conditionText = weatherDomainModel.conditionText,
            conditionIcon = weatherDomainModel.conditionIcon,
            alerts = weatherDomainModel.alerts,
            stats = weatherDomainModel.stats,
            dailyForecasts = weatherDomainModel.dailyForecasts,
            hourlyForecasts = weatherDomainModel.hourlyForecasts,
        )
    }
}