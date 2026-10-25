package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.models.favoriteId
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveFavoriteStatusUseCaseTest {

    private val snapped = 30.05 to 31.25     // the point the weather service reported
    private val asked = 30.04 to 31.24       // the point we asked about

    @Test
    fun `finds a favorite saved under the reported point`() = runTest {
        val repo = FakeFavorites(saved = setOf(favoriteId(snapped.first, snapped.second)))

        val id = ObserveFavoriteStatusUseCase(repo)(listOf(snapped, asked)).first()

        assertEquals(favoriteId(snapped.first, snapped.second), id)
    }

    @Test
    fun `finds a favorite saved under the point we asked about`() = runTest {
        val repo = FakeFavorites(saved = setOf(favoriteId(asked.first, asked.second)))

        val id = ObserveFavoriteStatusUseCase(repo)(listOf(snapped, asked)).first()

        assertEquals(favoriteId(asked.first, asked.second), id)
    }

    @Test
    fun `returns null when no point is a favorite`() = runTest {
        val repo = FakeFavorites(saved = emptySet())

        val id = ObserveFavoriteStatusUseCase(repo)(listOf(snapped, asked)).first()

        assertNull(id)
    }

    @Test
    fun `two points in the same 1 km cell count once`() = runTest {
        val repo = FakeFavorites(saved = setOf(favoriteId(30.051, 31.251)))

        val id = ObserveFavoriteStatusUseCase(repo)(listOf(30.051 to 31.251, 30.052 to 31.252)).first()

        assertEquals(favoriteId(30.051, 31.251), id)
    }
}

private class FakeFavorites(private val saved: Set<String>) : FavoritesRepository {
    override fun observeIsFavorite(id: String): Flow<Boolean> = flowOf(id in saved)

    override fun getAllFavorites(): Flow<List<FavoriteCityDomainModel>> = flowOf(emptyList<FavoriteCityDomainModel>())
    override suspend fun toggleFavorite(city: FavoriteCityDomainModel) = Unit
    override suspend fun saveFavorite(city: FavoriteCityDomainModel) = Unit
    override suspend fun deleteFavorite(id: String) = Unit
    override suspend fun refreshSnapshot(city: FavoriteCityDomainModel) = Unit
    override suspend fun updateNames(id: String, cityName: String, countryName: String) = Unit
    override suspend fun getFavoritesList(): List<FavoriteCityDomainModel> = emptyList()
}
