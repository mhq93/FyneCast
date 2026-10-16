package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository

class DeleteFavoriteUseCase(private val favoritesRepository: FavoritesRepository) {
    suspend operator fun invoke(id: String) = favoritesRepository.deleteFavorite(id)
}