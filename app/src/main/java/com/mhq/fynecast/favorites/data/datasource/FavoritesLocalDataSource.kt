package com.mhq.fynecast.favorites.data.datasource

import com.mhq.fynecast.favorites.data.database.FavoriteEntity
import kotlinx.coroutines.flow.Flow

/** Where favorites are stored on the phone. Works with database entities, never domain models. */
interface FavoritesLocalDataSource {
    fun observeIsFavorite(userId: String, key: String): Flow<Boolean>
    fun observeAll(userId: String): Flow<List<FavoriteEntity>>
    suspend fun getAllOnce(userId: String): List<FavoriteEntity>

    /** Adds the favorite if it is missing and removes it if present, as one atomic step. */
    suspend fun toggle(entity: FavoriteEntity)
    suspend fun insert(entity: FavoriteEntity)
    suspend fun delete(userId: String, key: String)
    suspend fun updateSnapshot(
        userId: String,
        key: String,
        tempC: Double,
        tempF: Double,
        icon: String
    )
}
