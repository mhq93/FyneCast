package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow

class ObserveFavoriteStatusUseCase(
    private val favoritesRepository: FavoritesRepository
) {
    operator fun invoke(fullName: String): Flow<Boolean> {
        val parts = fullName.split(",")
        val cityName = parts.firstOrNull()?.trim() ?: fullName
        val countryName = parts.getOrNull(1)?.trim() ?: ""

        return favoritesRepository.observeIsFavorite(
            name = cityName,
            country = countryName
        )
    }
}