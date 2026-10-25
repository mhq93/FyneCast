package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class KeepFavoriteNamesLocalizedUseCaseTest {

    private val cairo = FavoriteCityDomainModel(
        cityName = "Cairo",
        countryName = "Egypt",
        latitude = 30.04,
        longitude = 31.24,
        temperatureC = 24.0,
        temperatureF = 75.2,
        weatherIconUrl = ""
    )

    private class Setup(
        val language: MutableStateFlow<String>,
        val user: MutableStateFlow<String?>,
        val favorites: FakeFavoritesRepository,
        val locations: FakeLocationRepository,
        val keepLocalized: KeepFavoriteNamesLocalizedUseCase
    )

    private fun setup(language: String, user: String?, favorite: FavoriteCityDomainModel): Setup {
        val languageFlow = MutableStateFlow(language)
        val userFlow = MutableStateFlow(user)
        val favorites = FakeFavoritesRepository(listOf(favorite))
        val locations = FakeLocationRepository(
            mapOf("ar" to "القاهرة, مصر", "en" to "Cairo, Egypt")
        )
        return Setup(
            language = languageFlow,
            user = userFlow,
            favorites = favorites,
            locations = locations,
            keepLocalized = KeepFavoriteNamesLocalizedUseCase(
                FakeAppLanguageProvider(languageFlow),
                FakeCurrentUserProvider(userFlow),
                RelocalizeFavoriteNamesUseCase(favorites, locations)
            )
        )
    }

    /** Starts the never-ending use case in the background and lets it reach its first wait. */
    private fun TestScope.start(setup: Setup) {
        backgroundScope.launch { setup.keepLocalized() }
        runCurrent()
    }

    @Test
    fun `does nothing at app start`() = runTest {
        val s = setup(language = "ar", user = "u1", favorite = cairo)

        start(s)

        assertEquals(0, s.locations.lookups)
    }

    @Test
    fun `rewrites the names when the language changes`() = runTest {
        val s = setup(language = "en", user = "u1", favorite = cairo)
        start(s)

        s.language.value = "ar"
        runCurrent()

        assertEquals(listOf(Triple(cairo.id, "القاهرة", "مصر")), s.favorites.nameUpdates)
    }

    @Test
    fun `runs again for every later language change`() = runTest {
        val s = setup(language = "en", user = "u1", favorite = cairo)
        start(s)

        s.language.value = "ar"
        runCurrent()
        s.language.value = "en"
        runCurrent()

        assertEquals(2, s.locations.lookups)
    }

    @Test
    fun `does nothing while nobody is signed in`() = runTest {
        val s = setup(language = "en", user = null, favorite = cairo)
        start(s)

        s.language.value = "ar"
        runCurrent()

        assertEquals(0, s.locations.lookups)
    }

    @Test
    fun `runs when a user signs in`() = runTest {
        val s = setup(language = "ar", user = null, favorite = cairo)
        start(s)

        s.user.value = "u1"
        runCurrent()

        assertEquals(1, s.locations.lookups)
    }
}
