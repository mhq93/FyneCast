package com.mhq.fynecast.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

//@Dao
//interface FavoriteDao {
//    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE latitude = :lat AND longitude = :lon)")
//    fun isFavorite(lat: Double, lon: Double): Flow<Boolean>
//    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE latitude = :lat AND longitude = :lon)")
//    suspend fun isFavoriteOnce(lat: Double, lon: Double): Boolean
//    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE latitude = :lat AND longitude = :lon)")
//    fun isFavoriteFlow(lat: Double, lon: Double): Flow<Boolean>
//    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
//    suspend fun insertFavorite(favoriteEntity: FavoriteEntity)
//    @Delete
//    suspend fun deleteFavorite(favoriteEntity: FavoriteEntity)
//    @Query("DELETE FROM favorites WHERE latitude = :lat AND longitude = :lon")
//    suspend fun deleteFavoriteByCoordinates(lat: Double, lon: Double)
//    @Query("SELECT * FROM favorites")
//    fun getAllFavorites(): PagingSource<Int, FavoriteEntity>
//}

@Dao
interface FavoriteDao {
    // 1. Check if favorite exists using the Primary Keys (Strings)
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE cityName = :name AND countryName = :country)")
    fun isFavoriteFlow(name: String, country: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE cityName = :name AND countryName = :country)")
    suspend fun isFavoriteOnce(name: String, country: String): Boolean

    // 2. Delete using the Primary Keys
    @Query("DELETE FROM favorites WHERE cityName = :name AND countryName = :country")
    suspend fun deleteByName(name: String, country: String)

    // 3. Standard Room Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favoriteEntity: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favoriteEntity: FavoriteEntity)

    // 4. List retrieval for Paging
    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): PagingSource<Int, FavoriteEntity>
}
