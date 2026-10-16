package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.favorites.domain.models.favoriteId
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow

class ObserveFavoriteStatusUseCase(private val favoritesRepository: FavoritesRepository) {
    operator fun invoke(latitude: Double, longitude: Double): Flow<Boolean> =
        favoritesRepository.observeIsFavorite(favoriteId(latitude, longitude))
}