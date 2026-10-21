package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import com.mhq.fynecast.home.domain.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Brings every favorite's temperature and icon up to date (B41).
 * Runs at most once per [MIN_INTERVAL_MILLIS] and never twice at the same time.
 */
class RefreshAllFavoritesUseCase(
    private val favoritesRepository: FavoritesRepository,
    private val weatherRepository: WeatherRepository
) {
    private val runLock = Mutex()
    private var lastSuccessfulRunMillis = 0L

    suspend operator fun invoke(lang: String) {
        if (!runLock.tryLock()) return
        try {
            val now = System.currentTimeMillis()
            if (now - lastSuccessfulRunMillis < MIN_INTERVAL_MILLIS) return
            if (refreshAll(lang)) lastSuccessfulRunMillis = System.currentTimeMillis()
        } finally {
            runLock.unlock()
        }
    }

    /** True when at least one favorite was refreshed. */
    private suspend fun refreshAll(lang: String): Boolean {
        val favorites = favoritesRepository.getFavoritesList()
        if (favorites.isEmpty()) return false
        val gate = Semaphore(MAX_PARALLEL_REQUESTS)
        return coroutineScope {
            favorites.map { favorite ->
                async {
                    gate.withPermit {
                        try {
                            val weather = weatherRepository.fetchWeather(
                                "${favorite.latitude},${favorite.longitude}",
                                lang
                            )
                            favoritesRepository.refreshSnapshot(
                                favorite.copy(
                                    temperatureC = weather.currentTempC,
                                    temperatureF = weather.currentTempF,
                                    weatherIconUrl = weather.conditionIcon
                                )
                            )
                            true
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            false
                        }
                    }
                }
            }.awaitAll().any { it }
        }
    }

    private companion object {
        const val MIN_INTERVAL_MILLIS = 10 * 60 * 1000L
        const val MAX_PARALLEL_REQUESTS = 3
    }
}