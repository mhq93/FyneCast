package com.mhq.fynecast.favorites.data.datasource

import com.mhq.fynecast.favorites.data.database.FavoriteDao
import com.mhq.fynecast.favorites.data.database.FavoriteEntity
import kotlinx.coroutines.flow.Flow

class FavoritesRoomDataSource(
    private val favoriteDao: FavoriteDao
) : FavoritesLocalDataSource {

    override fun observeIsFavorite(userId: String, key: String): Flow<Boolean> =
        favoriteDao.observeIsFavorite(userId, key)

    override fun observeAll(userId: String): Flow<List<FavoriteEntity>> =
        favoriteDao.getAllFavorites(userId)

    override suspend fun getAllOnce(userId: String): List<FavoriteEntity> =
        favoriteDao.getFavoritesOnce(userId)

    override suspend fun toggle(entity: FavoriteEntity) = favoriteDao.toggle(entity)

    override suspend fun insert(entity: FavoriteEntity) = favoriteDao.insertFavorite(entity)

    override suspend fun delete(userId: String, key: String) =
        favoriteDao.deleteFavorite(userId, key)

    override suspend fun updateSnapshot(
        userId: String,
        key: String,
        tempC: Double,
        tempF: Double,
        icon: String
    ) = favoriteDao.updateSnapshot(userId, key, tempC, tempF, icon)
}
