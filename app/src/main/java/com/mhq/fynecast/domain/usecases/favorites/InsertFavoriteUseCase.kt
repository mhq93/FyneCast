package com.mhq.fynecast.domain.usecases.favorites

import com.mhq.fynecast.data.database.FavoriteEntity
import com.mhq.fynecast.data.repository.FavoritesRepository

class InsertFavoriteUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(favoriteEntity: FavoriteEntity) {
        repository.insertFavorite(favoriteEntity)
    }
}