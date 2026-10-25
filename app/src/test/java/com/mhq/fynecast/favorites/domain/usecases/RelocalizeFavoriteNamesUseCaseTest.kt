package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.core.domain.location.LocationRepository
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelocalizeFavoriteNamesUseCaseTest {

    private val cairo = FavoriteCityDomainModel(
        cityName = "Cairo",
        countryName = "Egypt",
        latitude = 30.04,
        longitude = 31.24,
        temperatureC = 24.0,
        temperatureF = 75.2,
        weatherIconUrl = ""
    )

    @Test
    fun `rewrites the saved names in the new language`() = runTest {
        val favorites = FakeFavoritesRepository(listOf(cairo))
        val locations = FakeLocationRepository(localized = "القاهرة, مصر")

        RelocalizeFavoriteNamesUseCase(favorites, locations)("ar")

        assertEquals(listOf(Triple(cairo.id, "القاهرة", "مصر")), favorites.nameUpdates)
    }

    @Test
    fun `keeps the old name when the lookup finds nothing`() = runTest {
        val favorites = FakeFavoritesRepository(listOf(cairo))
        val locations = FakeLocationRepository(localized = null)

        RelocalizeFavoriteNamesUseCase(favorites, locations)("ar")

        assertTrue(favorites.nameUpdates.isEmpty())
    }

    @Test
    fun `writes nothing when the name is already in that language`() = runTest {
        val favorites = FakeFavoritesRepository(listOf(cairo))
        val locations = FakeLocationRepository(localized = "Cairo, Egypt")

        RelocalizeFavoriteNamesUseCase(favorites, locations)("en")

        assertTrue(favorites.nameUpdates.isEmpty())
    }

    @Test
    fun `uses the place as the country when the geocoder returns one name`() = runTest {
        val singapore = cairo.copy(cityName = "Singapore", countryName = "Singapore")
        val favorites = FakeFavoritesRepository(listOf(singapore))
        val locations = FakeLocationRepository(localized = "سنغافورة")

        RelocalizeFavoriteNamesUseCase(favorites, locations)("ar")

        assertEquals(listOf(Triple(singapore.id, "سنغافورة", "سنغافورة")), favorites.nameUpdates)
    }

    @Test
    fun `looks up each language only once`() = runTest {
        val favorites = FakeFavoritesRepository(listOf(cairo))
        val locations = FakeLocationRepository(localized = "القاهرة, مصر")
        val useCase = RelocalizeFavoriteNamesUseCase(favorites, locations)

        useCase("ar")
        useCase("ar")

        assertEquals(1, locations.lookups)
    }

    @Test
    fun `looks up again when the language changes`() = runTest {
        val favorites = FakeFavoritesRepository(listOf(cairo))
        val locations = FakeLocationRepository(localized = "Cairo, Egypt")
        val useCase = RelocalizeFavoriteNamesUseCase(favorites, locations)

        useCase("ar")
        useCase("en")

        assertEquals(2, locations.lookups)
    }
}

private class FakeFavoritesRepository(
    private val favorites: List<FavoriteCityDomainModel>
) : FavoritesRepository {
    val nameUpdates = mutableListOf<Triple<String, String, String>>()

    override suspend fun getFavoritesList() = favorites
    override suspend fun updateNames(id: String, cityName: String, countryName: String) {
        nameUpdates += Triple(id, cityName, countryName)
    }

    override fun getAllFavorites(): Flow<List<FavoriteCityDomainModel>> = flowOf(favorites)
    override fun observeIsFavorite(id: String): Flow<Boolean> = flowOf(false)
    override suspend fun toggleFavorite(city: FavoriteCityDomainModel) = Unit
    override suspend fun saveFavorite(city: FavoriteCityDomainModel) = Unit
    override suspend fun deleteFavorite(id: String) = Unit
    override suspend fun refreshSnapshot(city: FavoriteCityDomainModel) = Unit
}

private class FakeLocationRepository(
    private val localized: String?
) : LocationRepository {
    var lookups = 0

    override suspend fun getLocalizedPlaceName(
        name: String,
        lat: Double,
        lon: Double,
        lang: String
    ): String? {
        lookups++
        return localized
    }

    override suspend fun getCitySuggestions(query: String, lang: String): List<CitySuggestionModel> =
        emptyList()

    override suspend fun getCityNameOffMap(lat: Double, lon: Double, lang: String): CitySuggestionModel? =
        null
}
