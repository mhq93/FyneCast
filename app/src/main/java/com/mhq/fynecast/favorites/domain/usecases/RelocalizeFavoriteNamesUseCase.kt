package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.core.domain.location.LocationRepository
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * One pass over the favorites of the signed-in user: rewrites each saved city and country
 * in [lang] (a city saved in English reads in Arabic after the switch, and back).
 *
 * A favorite whose name cannot be looked up (offline, no match near its coordinates)
 * keeps the name it already has.
 */
class RelocalizeFavoriteNamesUseCase(
    private val favoritesRepository: FavoritesRepository,
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(lang: String) {
        val favorites = favoritesRepository.getFavoritesList()
        if (favorites.isEmpty()) return
        val gate = Semaphore(MAX_PARALLEL_LOOKUPS)
        coroutineScope {
            favorites.map { favorite ->
                async { gate.withPermit { relocalize(favorite, lang) } }
            }.awaitAll()
        }
    }

    private suspend fun relocalize(favorite: FavoriteCityDomainModel, lang: String) {
        try {
            val localized = locationRepository.getLocalizedPlaceName(
                name = "${favorite.cityName}, ${favorite.countryName}",
                lat = favorite.latitude,
                lon = favorite.longitude,
                lang = lang
            ) ?: return

            val city = localized.substringBefore(",").trim()
            // The geocoder leaves the country out when it equals the city (e.g. "Singapore").
            val country = localized.substringAfter(",", "").trim().ifBlank { city }
            if (city.isBlank()) return
            if (city == favorite.cityName && country == favorite.countryName) return

            favoritesRepository.updateNames(favorite.id, city, country)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep the old name.
        }
    }

    private companion object {
        const val MAX_PARALLEL_LOOKUPS = 3
    }
}
