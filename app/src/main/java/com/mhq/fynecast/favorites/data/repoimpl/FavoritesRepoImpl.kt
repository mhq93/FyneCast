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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesRepoImpl(
    private val favoriteDao: FavoriteDao,
    private val currentUserProvider: CurrentUserProvider
) : FavoritesRepository {

    override fun observeIsFavorite(id: String): Flow<Boolean> =
        currentUserProvider.observeUserId().flatMapLatest { userId ->
            if (userId == null)
                flowOf(false)
            else
                favoriteDao.observeIsFavorite(userId, id)
        }

    override suspend fun toggleFavorite(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        favoriteDao.toggle(city.toEntity(userId))
    }

    override suspend fun saveFavorite(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        favoriteDao.insertFavorite(city.toEntity(userId))
    }

    override suspend fun deleteFavorite(id: String) {
        val userId = currentUserProvider.currentUserId() ?: return
        favoriteDao.deleteFavorite(userId, id)
    }

    override suspend fun refreshSnapshot(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        favoriteDao.updateSnapshot(
            userId,
            city.id,
            city.temperatureC,
            city.temperatureF,
            city.weatherIconUrl
        )
    }

    override suspend fun getFavoritesList(): List<FavoriteCityDomainModel> {
        val userId = currentUserProvider.currentUserId() ?: return emptyList()
        return favoriteDao.getFavoritesOnce(userId).map { it.toDomain() }
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