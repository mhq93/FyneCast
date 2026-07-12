package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import com.mhq.fynecast.home.domain.models.WeatherDomainModel

class InsertFavoriteUseCase(
    private val favoritesRepository: FavoritesRepository
) {
    suspend operator fun invoke(fullName: String, weather: WeatherDomainModel) {
        val parts = fullName.split(",")
        val cleanCityName = parts.firstOrNull()?.trim() ?: weather.cityName
        val cleanCountryName = parts.getOrNull(1)?.trim() ?: weather.country

        // Map directly to your clean DOMAIN model, not the database entity
        val favoriteCityDomainModel = FavoriteCityDomainModel(
            cityName = cleanCityName,
            countryName = cleanCountryName,
            latitude = weather.latitude,
            longitude = weather.longitude,
            temperatureC = weather.currentTempC,
            temperatureF = weather.currentTempF,
            weatherIconUrl = weather.conditionIcon
        )

        // Pass the pure domain model down to the repository layer
        favoritesRepository.saveFavorite(favoriteCityDomainModel)
    }

    suspend operator fun invoke(favoriteCityDomainModel: FavoriteCityDomainModel) {
        favoritesRepository.saveFavorite(favoriteCityDomainModel)
    }
}