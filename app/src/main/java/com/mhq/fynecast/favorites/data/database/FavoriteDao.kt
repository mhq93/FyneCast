package com.mhq.fynecast.favorites.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    // Streams changes continuously (UI updates automatically)
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE cityName = :name AND countryName = :country)")
    fun observeIsFavorite(name: String, country: String): Flow<Boolean>

    // One-shot check (e.g., prior to an action)
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE cityName = :name AND countryName = :country)")
    suspend fun isFavorite(name: String, country: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favoriteEntity: FavoriteEntity)

    // Primary delete method using identifiers to avoid instantiating full entities
    @Query("DELETE FROM favorites WHERE cityName = :name AND countryName = :country")
    suspend fun deleteFavorite(name: String, country: String)

    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): PagingSource<Int, FavoriteEntity>
}
