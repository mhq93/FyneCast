package com.mhq.fynecast.favorites.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavoriteDao {

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND locationKey = :key)")
    abstract fun observeIsFavorite(userId: String, key: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND locationKey = :key)")
    abstract suspend fun isFavorite(userId: String, key: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND locationKey = :key")
    abstract suspend fun deleteFavorite(userId: String, key: String)

    @Query(
        """UPDATE favorites SET temperatureC = :tempC, temperatureF = :tempF, weatherIconUrl = :icon
              WHERE userId = :userId AND locationKey = :key"""
    )
    abstract suspend fun updateSnapshot(
        userId: String,
        key: String,
        tempC: Double,
        tempF: Double,
        icon: String
    )

    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY cityName ASC, countryName ASC")
    abstract fun getAllFavorites(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE userId = :userId")
    abstract suspend fun getFavoritesOnce(userId: String): List<FavoriteEntity>

    @Transaction
    open suspend fun toggle(entity: FavoriteEntity) {
        if (isFavorite(entity.userId, entity.locationKey))
            deleteFavorite(entity.userId, entity.locationKey)
        else
            insertFavorite(entity)
    }
}