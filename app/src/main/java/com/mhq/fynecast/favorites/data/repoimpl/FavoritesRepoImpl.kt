package com.mhq.fynecast.favorites.data.repoimpl

import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import com.mhq.fynecast.favorites.data.datasource.FavoritesLocalDataSource
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
    private val localDataSource: FavoritesLocalDataSource,
    private val currentUserProvider: CurrentUserProvider
) : FavoritesRepository {

    override fun observeIsFavorite(id: String): Flow<Boolean> =
        currentUserProvider.observeUserId().flatMapLatest { userId ->
            if (userId == null)
                flowOf(false)
            else
                localDataSource.observeIsFavorite(userId, id)
        }

    override suspend fun toggleFavorite(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        localDataSource.toggle(city.toEntity(userId))
    }

    override suspend fun saveFavorite(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        localDataSource.insert(city.toEntity(userId))
    }

    override suspend fun deleteFavorite(id: String) {
        val userId = currentUserProvider.currentUserId() ?: return
        localDataSource.delete(userId, id)
    }

    override suspend fun refreshSnapshot(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        localDataSource.updateSnapshot(
            userId,
            city.id,
            city.temperatureC,
            city.temperatureF,
            city.weatherIconUrl
        )
    }

    override suspend fun updateNames(id: String, cityName: String, countryName: String) {
        val userId = currentUserProvider.currentUserId() ?: return
        localDataSource.updateNames(userId, id, cityName, countryName)
    }

    override suspend fun getFavoritesList(): List<FavoriteCityDomainModel> {
        val userId = currentUserProvider.currentUserId() ?: return emptyList()
        return localDataSource.getAllOnce(userId).map { it.toDomain() }
    }

    override fun getAllFavorites(): Flow<List<FavoriteCityDomainModel>> =
        currentUserProvider.observeUserId().flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                localDataSource.observeAll(userId)
                    .map { entities -> entities.map { it.toDomain() } }
            }
        }
}
