package com.mhq.fynecast.favorites.domain.repository

import androidx.paging.PagingData
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    // Streams the paginated list to the UI
    fun getAllFavorites(): Flow<PagingData<FavoriteCityDomainModel>>

    // Continuous stream tracking if a specific city is favored
    fun observeIsFavorite(name: String, country: String): Flow<Boolean>

    // One-shot check for background operations or click listeners
    suspend fun isFavorite(name: String, country: String): Boolean

    // Persisting a favorite city onto local database
    suspend fun saveFavorite(city: FavoriteCityDomainModel)

    // Single source of deletion using primary keys to avoid instantiating models
    suspend fun deleteFavorite(name: String, country: String)
}
