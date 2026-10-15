package com.mhq.fynecast.favorites.data.repoimpl

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import com.mhq.fynecast.favorites.data.database.FavoriteDao
import com.mhq.fynecast.favorites.data.mapper.toDomain
import com.mhq.fynecast.favorites.data.mapper.toEntity
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FavoritesRepoImpl(
    private val favoriteDao: FavoriteDao,
    private val currentUserProvider: CurrentUserProvider
) : FavoritesRepository {

    override fun observeIsFavorite(name: String, country: String): Flow<Boolean> =
        currentUserProvider.observeUserId().flatMapLatest { userId ->
            if (userId == null) flowOf(false)
            else favoriteDao.observeIsFavorite(userId, name, country)
        }


    override suspend fun isFavorite(name: String, country: String): Boolean {
        val userId = currentUserProvider.currentUserId() ?: return false
        return favoriteDao.isFavorite(userId, name, country)
    }

    override suspend fun saveFavorite(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        favoriteDao.insertFavorite(city.toEntity(userId))
    }

    override suspend fun deleteFavorite(name: String, country: String) {
        val userId = currentUserProvider.currentUserId() ?: return
        favoriteDao.deleteFavorite(userId, name, country)
    }

    override fun getAllFavorites(): Flow<PagingData<FavoriteCityDomainModel>> =
        currentUserProvider.observeUserId().flatMapLatest { userId ->
            if (userId == null) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    pagingSourceFactory = { favoriteDao.getAllFavorites(userId) }
                ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
            }
        }
}