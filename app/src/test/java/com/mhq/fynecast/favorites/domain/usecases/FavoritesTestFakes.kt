package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.core.domain.infra.AppLanguageProvider
import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.core.domain.location.LocationRepository
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal class FakeFavoritesRepository(
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

/** Answers a place-name lookup from a language-to-name map; a missing language finds nothing. */
internal class FakeLocationRepository(
    private val namesByLanguage: Map<String, String>
) : LocationRepository {
    var lookups = 0

    override suspend fun getLocalizedPlaceName(
        name: String,
        lat: Double,
        lon: Double,
        lang: String
    ): String? {
        lookups++
        return namesByLanguage[lang]
    }

    override suspend fun getCitySuggestions(query: String, lang: String): List<CitySuggestionModel> =
        emptyList()

    override suspend fun getCityNameOffMap(lat: Double, lon: Double, lang: String): CitySuggestionModel? =
        null
}

internal class FakeAppLanguageProvider(
    val language: MutableStateFlow<String>
) : AppLanguageProvider {
    override fun observeLanguageCode(): Flow<String> = language
}

internal class FakeCurrentUserProvider(
    val user: MutableStateFlow<String?>
) : CurrentUserProvider {
    override fun observeUserId(): Flow<String?> = user
    override fun currentUserId(): String? = user.value
}
