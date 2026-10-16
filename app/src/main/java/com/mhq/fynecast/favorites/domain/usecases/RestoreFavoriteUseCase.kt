package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository

class RestoreFavoriteUseCase(private val favoritesRepository: FavoritesRepository) {
    suspend operator fun invoke(city: FavoriteCityDomainModel) =
        favoritesRepository.saveFavorite(city)
}