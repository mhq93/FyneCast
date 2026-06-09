package com.mhq.fynecast.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.mhq.fynecast.data.database.FavoriteDao
import com.mhq.fynecast.data.database.FavoriteEntity
import kotlinx.coroutines.flow.Flow

class FavoritesRepoImpl(private val favoriteDao: FavoriteDao) : FavoritesRepository {

//    override suspend fun isFavoriteOnce(lat: Double, lon: Double): Boolean {
//        //return favoriteDao.isFavoriteOnce(lat, lon)
//        return favoriteDao.isFavoriteOnce("%.3f".format(lat).toDouble(), "%.3f".format(lon).toDouble())
//    }
//
//    override fun isFavoriteFlow(lat: Double, lon: Double): Flow<Boolean> {
//        //return favoriteDao.isFavoriteFlow(lat, lon)
//        return favoriteDao.isFavoriteFlow("%.3f".format(lat).toDouble(), "%.3f".format(lon).toDouble())
//    }

//    override suspend fun toggleFavorite(favoriteEntity: FavoriteEntity) {
//        val exists = favoriteDao.isFavoriteOnce(favoriteEntity.latitude, favoriteEntity.longitude)
//        if (exists) {
//            favoriteDao.deleteFavoriteByCoordinates(favoriteEntity.latitude, favoriteEntity.longitude)
//        } else {
//            favoriteDao.insertFavorite(favoriteEntity)
//        }
//    }

    override fun isFavoriteFlow(name: String, country: String): Flow<Boolean> {
        return favoriteDao.isFavoriteFlow(name, country)
    }

    override suspend fun isFavoriteOnce(name: String, country: String): Boolean {
        return favoriteDao.isFavoriteOnce(name, country)
    }

    override suspend fun deleteFavoriteByName(name: String, country: String) {
        favoriteDao.deleteByName(name, country)
    }

    override suspend fun toggleFavorite(favoriteEntity: FavoriteEntity) {
        val exists = favoriteDao.isFavoriteOnce(favoriteEntity.cityName, favoriteEntity.countryName)
        if (exists) {
            favoriteDao.deleteByName(
                favoriteEntity.cityName,
                favoriteEntity.countryName
            )
        } else {
            favoriteDao.insertFavorite(favoriteEntity)
        }
    }

    override suspend fun insertFavorite(favoriteEntity: FavoriteEntity) {
        favoriteDao.insertFavorite(favoriteEntity = favoriteEntity)
    }

    override suspend fun deleteFavorite(favoriteEntity: FavoriteEntity) {
        favoriteDao.deleteFavorite(favoriteEntity)
    }

//    override suspend fun deleteFavoriteByCoordinates(lat: Double, lon: Double) {
//        favoriteDao.deleteFavoriteByCoordinates(lat, lon)
//    }

    override fun fetchFavorites(): Flow<PagingData<FavoriteEntity>> {
        return Pager(
            config = PagingConfig(pageSize = 10),
            pagingSourceFactory = { favoriteDao.getAllFavorites() }
        ).flow
    }
}