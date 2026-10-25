package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
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
        val locations = FakeLocationRepository(mapOf("ar" to "القاهرة, مصر"))

        RelocalizeFavoriteNamesUseCase(favorites, locations)("ar")

        assertEquals(listOf(Triple(cairo.id, "القاهرة", "مصر")), favorites.nameUpdates)
    }

    @Test
    fun `keeps the old name when the lookup finds nothing`() = runTest {
        val favorites = FakeFavoritesRepository(listOf(cairo))
        val locations = FakeLocationRepository(emptyMap())

        RelocalizeFavoriteNamesUseCase(favorites, locations)("ar")

        assertTrue(favorites.nameUpdates.isEmpty())
    }

    @Test
    fun `writes nothing when the name is already in that language`() = runTest {
        val favorites = FakeFavoritesRepository(listOf(cairo))
        val locations = FakeLocationRepository(mapOf("en" to "Cairo, Egypt"))

        RelocalizeFavoriteNamesUseCase(favorites, locations)("en")

        assertTrue(favorites.nameUpdates.isEmpty())
    }

    @Test
    fun `uses the place as the country when the geocoder returns one name`() = runTest {
        val singapore = cairo.copy(cityName = "Singapore", countryName = "Singapore")
        val favorites = FakeFavoritesRepository(listOf(singapore))
        val locations = FakeLocationRepository(mapOf("ar" to "سنغافورة"))

        RelocalizeFavoriteNamesUseCase(favorites, locations)("ar")

        assertEquals(listOf(Triple(singapore.id, "سنغافورة", "سنغافورة")), favorites.nameUpdates)
    }
}
