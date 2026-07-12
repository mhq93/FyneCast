package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository

class DeleteFavoriteUseCase(private val favoritesRepository: FavoritesRepository) {
    suspend operator fun invoke(cityName: String, countryName: String) {
        favoritesRepository.deleteFavorite(
            name = cityName,
            country = countryName
        )
    }
}