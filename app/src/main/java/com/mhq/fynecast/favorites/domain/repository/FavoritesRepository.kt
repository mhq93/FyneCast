package com.mhq.fynecast.favorites.domain.repository

import androidx.paging.PagingData
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getAllFavorites(): Flow<PagingData<FavoriteCityDomainModel>>
    fun observeIsFavorite(id: String): Flow<Boolean>
    /** Adds the city if it isn't a favorite and removes it if it is, as one atomic step. */
    suspend fun toggleFavorite(city: FavoriteCityDomainModel)
    suspend fun saveFavorite(city: FavoriteCityDomainModel)            // used to undo a removal
    suspend fun deleteFavorite(id: String)
    suspend fun refreshSnapshot(city: FavoriteCityDomainModel)         // updates temperature and icon
}