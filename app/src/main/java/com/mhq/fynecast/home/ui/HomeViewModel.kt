package com.mhq.fynecast.home.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.favorites.domain.usecases.RefreshFavoriteSnapshotUseCase
import com.mhq.fynecast.favorites.domain.usecases.ToggleFavoriteUseCase
import com.mhq.fynecast.home.domain.mapper.toFavoriteCity
import com.mhq.fynecast.core.domain.location.GeoCoordinates
import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.home.domain.usecases.GetCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.GetLastSearchedCityUseCase
import com.mhq.fynecast.home.domain.usecases.GetLocationSuggestionsUseCase
import com.mhq.fynecast.home.domain.usecases.LoadCurrentWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.LocalizePlaceNameUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveFavoriteStatusUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveSignedInUserIdUseCase
import com.mhq.fynecast.map.domain.usecases.ReverseGeocodeLocationUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SaveLastSearchedCityUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

class HomeViewModel(
    private val loadCurrentWeatherUseCase: LoadCurrentWeatherUseCase,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val getLocationSuggestionsUseCase: GetLocationSuggestionsUseCase,
    private val getLastSearchedCityUseCase: GetLastSearchedCityUseCase,
    private val observeSignedInUserIdUseCase: ObserveSignedInUserIdUseCase,
    private val observeFavoriteStatusUseCase: ObserveFavoriteStatusUseCase,
    private val refreshFavoriteSnapshotUseCase: RefreshFavoriteSnapshotUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val saveLastSearchedCityUseCase: SaveLastSearchedCityUseCase,
    private val reverseGeocodeLocationUseCase: ReverseGeocodeLocationUseCase,
    private val localizePlaceNameUseCase: LocalizePlaceNameUseCase,
    private val defaultCityFallback: String = "Cairo"
) : ViewModel() {

    private val _state = MutableStateFlow(HomeStateContainer())
    val state: StateFlow<HomeStateContainer> = _state.asStateFlow()
    private var loadTarget: LoadTarget = LoadTarget.Startup(useDeviceLocation = false)
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var favoriteJob: Job? = null
    private var lastRefreshTime = 0L
    private val refreshCooldownMillis = 60 * 1000L
    private var loadedLangCode: String? = null
    private var latestLangCode: String? = null

    /** The place we queried (not the weather service's snapped station) & how its name was made. */
    private var shownSpot: ShownSpot? = null

    /** True once any load has been requested; the UI uses it to ask for location only once. */
    var isInitialLaunchPerformed = false
        private set

    private sealed interface LoadTarget {
        val retry: HomeRetryAction

        data class Startup(val useDeviceLocation: Boolean) : LoadTarget {
            override val retry = HomeRetryAction.RequestLocation
        }

        data class Place(
            val name: String,
            val latitude: Double,
            val longitude: Double,
            val relocalizeName: Boolean = false,
            val nameFromDevice: Boolean = false
        ) : LoadTarget {
            override val retry = HomeRetryAction.ReloadPlace
        }
    }

    private class ShownSpot(
        val latitude: Double,
        val longitude: Double,
        val fromDevice: Boolean
    )

    private class StartupSpot(
        val query: String,
        val name: String?,
        val latitude: Double?,
        val longitude: Double?,
        val fromDevice: Boolean
    )

    private class LoadedCity(
        val weather: WeatherDomainModel,
        val name: String,
        val spot: ShownSpot
    )

    init {
        observeSignedInUserIdUseCase()
            .drop(1)
            .onEach { resetForNewSession() }
            .launchIn(viewModelScope)
    }

    fun onLocationPermissionResult(isGranted: Boolean, langCode: String) {
        load(LoadTarget.Startup(useDeviceLocation = isGranted), langCode)
    }

    fun onCitySelected(
        suggestionName: String,
        lat: Double,
        lon: Double,
        langCode: String
    ) {
        viewModelScope.launch {
            saveLastSearchedCityUseCase(
                name = suggestionName,
                lat = lat,
                lon = lon
            )
        }
        load(
            /**
             * Search results only come in English, German or French;
             * other languages get the name rewritten in the app language once the weather is in.
             * */
            LoadTarget.Place(
                suggestionName,
                lat,
                lon,
                relocalizeName = langCode != "en"
            ),
            langCode
        )
    }

    /** Reloads the place that failed. Startup failures retry through the location flow instead. */
    fun onRetryClicked(langCode: String) = load(loadTarget, langCode)

    private fun load(target: LoadTarget, langCode: String) {
        loadTarget = target
        isInitialLaunchPerformed = true

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                if (it.uiState is HomeUiState.Success) it.copy(isRefreshing = true)
                else it.copy(uiState = HomeUiState.Loading)
            }
            try {
                val loaded = withTimeout(LOAD_TIMEOUT_MILLIS.milliseconds) {
                    fetchCity(target, langCode)
                }
                _state.update {
                    it.copy(
                        uiState = HomeUiState.Success(loaded.weather),
                        selectedCityName = loaded.name,
                        isRefreshing = false
                    )
                }
                shownSpot = loaded.spot
                loadedLangCode = langCode
                onWeatherShown(loaded.weather)
                if (target is LoadTarget.Place && target.relocalizeName && !target.nameFromDevice) {
                    viewModelScope.launch {
                        saveLastSearchedCityUseCase(
                            name = loaded.name,
                            lat = target.latitude,
                            lon = target.longitude
                        )
                    }
                }
                latestLangCode?.takeIf { it != langCode }?.let { onLanguageChanged(it) }
            } catch (e: TimeoutCancellationException) {
                _state.update {
                    it.copy(
                        uiState = HomeUiState.Error(target.retry),
                        isRefreshing = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("HomeViewModel", "Load failed", e)
                _state.update {
                    it.copy(
                        uiState = HomeUiState.Error(target.retry),
                        isRefreshing = false
                    )
                }
            }
        }
    }

    private suspend fun fetchCity(target: LoadTarget, langCode: String): LoadedCity =
        when (target) {
            is LoadTarget.Place -> {
                val weather = loadCurrentWeatherUseCase(
                    "${target.latitude},${target.longitude}",
                    langCode
                )
                val name = when {
                    !target.relocalizeName -> target.name
                    target.nameFromDevice ->
                        lookupCityName(target.latitude, target.longitude, langCode) ?: target.name

                    else ->
                        localizePlaceName(target.name, target.latitude, target.longitude, langCode)
                            ?: target.name
                }
                LoadedCity(
                    weather,
                    name,
                    ShownSpot(target.latitude, target.longitude, target.nameFromDevice)
                )
            }

            is LoadTarget.Startup -> {
                val spot = startupSpot(target)
                val weather = loadCurrentWeatherUseCase(spot.query, langCode)
                val deviceName = if (spot.fromDevice) {
                    lookupCityName(spot.latitude, spot.longitude, langCode)
                } else {
                    null
                }
                val name = spot.name
                    ?: deviceName
                    ?: "${weather.cityName}, ${weather.country}"
                LoadedCity(
                    weather,
                    name,
                    ShownSpot(
                        latitude = spot.latitude ?: weather.latitude,
                        longitude = spot.longitude ?: weather.longitude,
                        fromDevice = spot.fromDevice
                    )
                )
            }
        }

    /** GPS (when allowed) → last searched city → default city. */
    private suspend fun startupSpot(target: LoadTarget.Startup): StartupSpot {
        delay(300.milliseconds)
        val coordinates = if (target.useDeviceLocation) currentCoordinatesOrNull() else null
        if (coordinates != null) {
            return StartupSpot(
                query = "${coordinates.latitude},${coordinates.longitude}",
                name = null,
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
                fromDevice = true
            )
        }
        val savedCity = getLastSearchedCityUseCase()
        return if (savedCity != null) {
            StartupSpot(
                query = "${savedCity.latitude},${savedCity.longitude}",
                name = savedCity.name,
                latitude = savedCity.latitude,
                longitude = savedCity.longitude,
                fromDevice = false
            )
        } else {
            StartupSpot(
                query = defaultCityFallback,
                name = null,
                latitude = null,
                longitude = null,
                fromDevice = false
            )
        }
    }

    private suspend fun currentCoordinatesOrNull(): GeoCoordinates? =
        try {
            getCurrentLocationUseCase()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    fun refreshCurrentCity(langCode: String) {
        val now = System.currentTimeMillis()
        if (now - lastRefreshTime < refreshCooldownMillis) return
        if (loadJob?.isActive == true) return
        val weather = (_state.value.uiState as? HomeUiState.Success)?.weather ?: return

        loadJob = viewModelScope.launch {
            try {
                val fresh =
                    loadCurrentWeatherUseCase("${weather.latitude},${weather.longitude}", langCode)
                _state.update { it.copy(uiState = HomeUiState.Success(fresh)) }
                loadedLangCode = langCode
                onWeatherShown(fresh)
                lastRefreshTime = System.currentTimeMillis()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh weather", e)
            }
        }
    }

    fun onLanguageChanged(langCode: String) {
        latestLangCode = langCode
        if (langCode == loadedLangCode) return
        val weather = (_state.value.uiState as? HomeUiState.Success)?.weather ?: return
        val spot = shownSpot
        load(
            LoadTarget.Place(
                name = _state.value.selectedCityName,
                latitude = spot?.latitude ?: weather.latitude,
                longitude = spot?.longitude ?: weather.longitude,
                relocalizeName = true,
                nameFromDevice = spot?.fromDevice ?: false
            ),
            langCode
        )
    }

    private fun resetForNewSession() {
        loadJob?.cancel()
        searchJob?.cancel()
        favoriteJob?.cancel()
        _state.value = HomeStateContainer()
        isInitialLaunchPerformed = false
        lastRefreshTime = 0L
        loadTarget = LoadTarget.Startup(useDeviceLocation = false)
        loadedLangCode = null
        shownSpot = null
    }

    fun onSearchQueryChanged(newQuery: String, langCode: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500.milliseconds)
            val suggestions = try {
                getLocationSuggestionsUseCase(newQuery, langCode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            _state.update { it.copy(citySuggestions = suggestions) }
        }
    }

    fun onToggleFavorite() {
        val state = _state.value
        val weather = (state.uiState as? HomeUiState.Success)?.weather ?: return
        val pickedName = state.selectedCityName
        val favorite = weather.toFavoriteCity().let { city ->
            if (pickedName.isBlank()) {
                city
            } else {
                city.copy(
                    cityName = pickedName.substringBefore(",").trim(),
                    countryName = pickedName.substringAfter(",", "").trim()
                        .ifBlank { city.countryName }
                )
            }
        }
        viewModelScope.launch { toggleFavoriteUseCase(favorite) }
    }

    private fun onWeatherShown(weather: WeatherDomainModel) {
        favoriteJob?.cancel()
        favoriteJob = observeFavoriteStatusUseCase(weather.latitude, weather.longitude)
            .onEach { isFav -> _state.update { it.copy(isFavorite = isFav) } }
            .launchIn(viewModelScope)

        viewModelScope.launch { refreshFavoriteSnapshotUseCase(weather.toFavoriteCity()) }
    }

    /** The name for these coordinates in the app language, or null if unknown, slow or failed. */
    private suspend fun lookupCityName(
        latitude: Double?,
        longitude: Double?,
        langCode: String
    ): String? {
        if (latitude == null || longitude == null) return null
        return withTimeoutOrNull(NAME_LOOKUP_TIMEOUT_MILLIS.milliseconds) {
            reverseGeocodeLocationUseCase(latitude, longitude, langCode)?.fullName
        }
    }

    /** [name] rewritten in the app language for the place near these coordinates, or null. */
    private suspend fun localizePlaceName(
        name: String,
        latitude: Double,
        longitude: Double,
        langCode: String
    ): String? = withTimeoutOrNull(NAME_LOOKUP_TIMEOUT_MILLIS.milliseconds) {
        localizePlaceNameUseCase(name, latitude, longitude, langCode)
    }

    companion object {
        private const val TAG = "HomeViewModel"
        private const val LOAD_TIMEOUT_MILLIS = 30_000L
        private const val NAME_LOOKUP_TIMEOUT_MILLIS = 4_000L
    }
}