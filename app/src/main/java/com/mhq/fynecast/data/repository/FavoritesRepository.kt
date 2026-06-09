package com.mhq.fynecast.data.repository

import androidx.paging.PagingData
import com.mhq.fynecast.data.database.FavoriteEntity
import kotlinx.coroutines.flow.Flow

//interface FavoritesRepository {
//    fun fetchFavorites(): Flow<PagingData<FavoriteEntity>>
//    suspend fun insertFavorite(favoriteEntity: FavoriteEntity)
//    suspend fun deleteFavorite(favoriteEntity: FavoriteEntity)
//    suspend fun deleteFavoriteByCoordinates(lat: Double, lon: Double)
//    fun isFavoriteFlow(lat: Double, lon: Double): Flow<Boolean>
//    suspend fun isFavoriteOnce(lat: Double, lon: Double): Boolean
//    suspend fun toggleFavorite(favoriteEntity: FavoriteEntity)
//}

interface FavoritesRepository {
    // 1. Core List Operations
    fun fetchFavorites(): Flow<PagingData<FavoriteEntity>>
    suspend fun insertFavorite(favoriteEntity: FavoriteEntity)
    suspend fun deleteFavorite(favoriteEntity: FavoriteEntity)
    // 2. Name-Based Operations (Primary Key Logic)
    fun isFavoriteFlow(name: String, country: String): Flow<Boolean>
    suspend fun isFavoriteOnce(name: String, country: String): Boolean
    suspend fun deleteFavoriteByName(name: String, country: String)
    // 3. Logic Wrapper
    suspend fun toggleFavorite(favoriteEntity: FavoriteEntity)
}
