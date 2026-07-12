package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository

class ToggleFavoriteUseCase(private val favoritesRepository: FavoritesRepository) {
    suspend operator fun invoke(city: FavoriteCityDomainModel) {
        val isFavorite = favoritesRepository.isFavorite(
            name = city.cityName,
            country = city.countryName
        )

        if (isFavorite) {
            favoritesRepository.deleteFavorite(
                name = city.cityName,
                country = city.countryName
            )
        } else {
            favoritesRepository.saveFavorite(city)
        }
    }
}
