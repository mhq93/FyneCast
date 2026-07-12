package com.mhq.fynecast.favorites.data.repoimpl

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.mhq.fynecast.favorites.data.database.FavoriteDao
import com.mhq.fynecast.favorites.data.mapper.toDomain
import com.mhq.fynecast.favorites.data.mapper.toEntity
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepoImpl(
    private val favoriteDao: FavoriteDao
) : FavoritesRepository {

    override fun observeIsFavorite(name: String, country: String): Flow<Boolean> {
        return favoriteDao.observeIsFavorite(name, country)
    }

    override suspend fun isFavorite(name: String, country: String): Boolean {
        return favoriteDao.isFavorite(name, country)
    }

    override suspend fun saveFavorite(city: FavoriteCityDomainModel) {
        favoriteDao.insertFavorite(city.toEntity())
    }

    override suspend fun deleteFavorite(name: String, country: String) {
        favoriteDao.deleteFavorite(name, country)
    }

    override fun getAllFavorites(): Flow<PagingData<FavoriteCityDomainModel>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { favoriteDao.getAllFavorites() }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }
}