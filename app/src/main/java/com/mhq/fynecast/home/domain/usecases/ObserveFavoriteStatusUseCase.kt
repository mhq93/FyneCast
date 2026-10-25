package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.favorites.domain.models.favoriteId
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

class ObserveFavoriteStatusUseCase(private val favoritesRepository: FavoritesRepository) {

    /**
     * The id of the saved favorite that stands for the city on screen, or null when it is not one.
     *
     * [places] are every coordinate pair that may identify the city: the weather service snaps a
     * query to its own point, so the point it reports can differ from the one we asked about.
     * A favorite counts if it was saved under any of them.
     */
    operator fun invoke(places: List<Pair<Double, Double>>): Flow<String?> {
        val ids = places.map { (latitude, longitude) -> favoriteId(latitude, longitude) }.distinct()
        if (ids.isEmpty()) return flowOf(null)
        return combine(ids.map { favoritesRepository.observeIsFavorite(it) }) { isSaved ->
            ids.zip(isSaved.toList()).firstOrNull { (_, saved) -> saved }?.first
        }
    }
}
