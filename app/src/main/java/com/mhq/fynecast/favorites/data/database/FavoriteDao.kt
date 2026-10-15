package com.mhq.fynecast.favorites.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND cityName = :name AND countryName = :country)")
    fun observeIsFavorite(userId: String, name: String, country: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND cityName = :name AND countryName = :country)")
    suspend fun isFavorite(userId: String, name: String, country: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favoriteEntity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND cityName = :name AND countryName = :country")
    suspend fun deleteFavorite(userId: String, name: String, country: String)

    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY cityName ASC, countryName ASC")
    fun getAllFavorites(userId: String): PagingSource<Int, FavoriteEntity>
}