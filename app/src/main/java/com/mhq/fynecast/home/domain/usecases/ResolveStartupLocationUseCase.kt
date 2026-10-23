package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.home.domain.models.StartupLocation
import kotlinx.coroutines.CancellationException

/** Where to load weather on startup: GPS (when allowed) → last searched city → default city. */
class ResolveStartupLocationUseCase(
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val getLastSearchedCityUseCase: GetLastSearchedCityUseCase,
    private val defaultCity: String = "Cairo"
) {
    suspend operator fun invoke(useDeviceLocation: Boolean): StartupLocation {
        val coordinates = if (useDeviceLocation) {
            try {
                getCurrentLocationUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        } else null

        if (coordinates != null) {
            return StartupLocation(
                query = "${coordinates.latitude},${coordinates.longitude}",
                name = null,
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
                fromDevice = true
            )
        }
        val savedCity = getLastSearchedCityUseCase()
        return if (savedCity != null) {
            StartupLocation(
                query = "${savedCity.latitude},${savedCity.longitude}",
                name = savedCity.name,
                latitude = savedCity.latitude,
                longitude = savedCity.longitude,
                fromDevice = false
            )
        } else {
            StartupLocation(
                query = defaultCity,
                name = null,
                latitude = null,
                longitude = null,
                fromDevice = false
            )
        }
    }
}