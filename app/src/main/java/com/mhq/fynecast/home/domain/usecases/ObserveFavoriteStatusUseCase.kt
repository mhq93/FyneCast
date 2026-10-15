package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import com.mhq.fynecast.util.CityNameParser
import kotlinx.coroutines.flow.Flow

class ObserveFavoriteStatusUseCase(
    private val favoritesRepository: FavoritesRepository
) {
    operator fun invoke(fullName: String): Flow<Boolean> {
        val parsed = CityNameParser.parse(fullName)

        return favoritesRepository.observeIsFavorite(
            name = parsed.cityName,
            country = parsed.countryName
        )
    }
}