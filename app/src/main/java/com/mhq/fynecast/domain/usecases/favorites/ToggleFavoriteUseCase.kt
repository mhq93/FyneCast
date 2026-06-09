package com.mhq.fynecast.domain.usecases.favorites

import com.mhq.fynecast.data.database.FavoriteEntity
import com.mhq.fynecast.data.repository.FavoritesRepository

class ToggleFavoriteUseCase(private val repository: FavoritesRepository) {
    suspend operator fun invoke(favoriteEntity: FavoriteEntity) {
        val isFav = repository.isFavoriteOnce(
            //favoriteEntity.latitude,
            //favoriteEntity.longitude
            favoriteEntity.cityName,
            favoriteEntity.countryName
        )
        if (isFav) {
            repository.deleteFavoriteByName(
                favoriteEntity.cityName,
                favoriteEntity.countryName
            )
            //repository.deleteFavoriteByCoordinates(
            //    favoriteEntity.latitude,
            //    favoriteEntity.longitude
            //)
        } else {
            repository.insertFavorite(favoriteEntity)
        }
    }
}
