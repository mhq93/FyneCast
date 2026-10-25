package com.mhq.fynecast.favorites.domain.repository

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    /** Observes the signed-in user's favorites, sorted by city then country. */
    fun getAllFavorites(): Flow<List<FavoriteCityDomainModel>>
    /** Observes whether the city is a favorite to perform CRUD operations accordingly. */
    fun observeIsFavorite(id: String): Flow<Boolean>
    /** Adds the city if it isn't a favorite and removes it if it is, as one atomic step. */
    suspend fun toggleFavorite(city: FavoriteCityDomainModel)
    /** used to undo a removal. */
    suspend fun saveFavorite(city: FavoriteCityDomainModel)
    /** used to delete a favorite. */
    suspend fun deleteFavorite(id: String)
    /** updates temperature and icon. */
    suspend fun refreshSnapshot(city: FavoriteCityDomainModel)
    /** rewrites the saved city and country names, e.g. after the app language changed. */
    suspend fun updateNames(id: String, cityName: String, countryName: String)
    /** one-off read, not observed. */
    suspend fun getFavoritesList(): List<FavoriteCityDomainModel>
}
