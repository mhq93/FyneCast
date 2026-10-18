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
import com.mhq.fynecast.home.domain.models.GeoCoordinates
import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.domain.usecases.GetCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.GetLastSearchedCityUseCase
import com.mhq.fynecast.home.domain.usecases.GetLocationSuggestionsUseCase
import com.mhq.fynecast.home.domain.usecases.LoadCurrentWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveFavoriteStatusUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveSignedInUserIdUseCase
import com.mhq.fynecast.map.domain.usecases.ReverseGeocodeLocationUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase
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
    private val updatePreferenceUseCase: UpdatePreferenceUseCase,
    private val reverseGeocodeLocationUseCase: ReverseGeocodeLocationUseCase,
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

    /** True once any load has been requested;
     * the UI uses it to ask for location only once. */
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
            val longitude: Double
        ) : LoadTarget {
            override val retry = HomeRetryAction.ReloadPlace
        }
    }

    init {
        observeSignedInUserIdUseCase()
            .drop(1) // the first value is whoever is already signed in
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
            updatePreferenceUseCase.lastSearchedCity(name = suggestionName, lat = lat, lon = lon)
        }
        load(
            LoadTarget.Place(suggestionName, lat, lon), langCode
        )
    }

    /** Reloads the place that failed. Startup failures retry through the location flow instead. */
    fun onRetryClicked(langCode: String) = load(loadTarget, langCode)

    private fun load(target: LoadTarget, langCode: String) {
        loadTarget = target
        isInitialLaunchPerformed = true

        loadJob?.cancel()                    // the newest request wins
        loadJob = viewModelScope.launch {
            _state.update { it.copy(uiState = HomeUiState.Loading) }
            try {
                val (weather, displayName) = withTimeout(LOAD_TIMEOUT_MILLIS.milliseconds) {
                    when (target) {
                        is LoadTarget.Place -> {
                            val weather = loadCurrentWeatherUseCase(
                                "${target.latitude},${target.longitude}",
                                langCode
                            )
                            weather to target.name
                        }
                        is LoadTarget.Startup -> {
                            val spot = startupSpot(target)
                            val weather = loadCurrentWeatherUseCase(spot.query, langCode)
                            val name = spot.name
                                ?: lookupCityName(spot.latitude, spot.longitude, langCode)
                                ?: "${weather.cityName}, ${weather.country}"
                            weather to name
                        }
                    }
                }
                _state.update {
                    it.copy(uiState = HomeUiState.Success(weather), selectedCityName = displayName)
                }
                loadedLangCode = langCode
                onWeatherShown(weather)
                latestLangCode?.takeIf { it != langCode }?.let { onLanguageChanged(it) }
            } catch (e: TimeoutCancellationException) {
                _state.update { it.copy(uiState = HomeUiState.Error("timeout", target.retry)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // selectedCityName is untouched: no city was loaded, so none should be claimed.
                _state.update {
                    it.copy(uiState = HomeUiState.Error(e.message ?: "Unknown Error", target.retry))
                }
            }
        }
    }

    private class StartupSpot(
        val query: String,
        val name: String?,         // the name the user picked, when we have it
        val latitude: Double?,     // where we are asking about, when known
        val longitude: Double?
    )

    /** GPS (when allowed) → last searched city → default city. */
    private suspend fun startupSpot(target: LoadTarget.Startup): StartupSpot {
        delay(300.milliseconds)
        val coordinates = if (target.useDeviceLocation) currentCoordinatesOrNull() else null
        if (coordinates != null) {
            return StartupSpot(
                query = "${coordinates.latitude},${coordinates.longitude}",
                name = null,
                latitude = coordinates.latitude,
                longitude = coordinates.longitude
            )
        }
        val savedCity = getLastSearchedCityUseCase()
        return if (savedCity != null) {
            StartupSpot(
                query = "${savedCity.latitude},${savedCity.longitude}",
                name = savedCity.name,
                latitude = savedCity.latitude,
                longitude = savedCity.longitude
            )
        } else {
            StartupSpot(query = defaultCityFallback, name = null, latitude = null, longitude = null)
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
        if (loadJob?.isActive == true) return  // never interrupt a load
        val weather = (_state.value.uiState as? HomeUiState.Success)?.weather ?: return

        loadJob = viewModelScope.launch {
            try {
                val fresh =
                    loadCurrentWeatherUseCase("${weather.latitude},${weather.longitude}", langCode)
                _state.update { it.copy(uiState = HomeUiState.Success(fresh)) }
                loadedLangCode = langCode
                onWeatherShown(fresh)
                lastRefreshTime = System.currentTimeMillis()  // only after success
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
        load(
            LoadTarget.Place(_state.value.selectedCityName, weather.latitude, weather.longitude),
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
    }

    fun onSearchQueryChanged(newQuery: String, langCode: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500.milliseconds)
            val suggestions = runCatching {
                getLocationSuggestionsUseCase(newQuery, langCode)
            }.getOrNull() ?: emptyList()
            _state.update { it.copy(citySuggestions = suggestions) }
        }
    }

    fun onToggleFavorite() {
        val state = _state.value
        val weather = (state.uiState as? HomeUiState.Success)?.weather ?: return
        // Save under the name the user picked, not the weather service's station name.
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

        // Keeps a favorite's temperature and icon current whenever its city is shown.
        viewModelScope.launch { refreshFavoriteSnapshotUseCase(weather.toFavoriteCity()) }
    }

    /** The name for these coordinates, or null if they are unknown, or the lookup is slow or fails. */
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

    companion object {
        private const val TAG = "HomeViewModel"
        private const val LOAD_TIMEOUT_MILLIS = 30_000L
        private const val NAME_LOOKUP_TIMEOUT_MILLIS = 2_000L

        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FyneCastApplication
                HomeViewModel(
                    loadCurrentWeatherUseCase = app.container.loadCurrentWeatherUseCase,
                    getCurrentLocationUseCase = app.container.getCurrentLocationUseCase,
                    getLocationSuggestionsUseCase = app.container.getLocationSuggestionsUseCase,
                    getLastSearchedCityUseCase = app.container.getLastSearchedCityUseCase,
                    observeFavoriteStatusUseCase = app.container.observeFavoriteStatusUseCase,
                    observeSignedInUserIdUseCase = app.container.observeSignedInUserIdUseCase,
                    refreshFavoriteSnapshotUseCase = app.container.refreshFavoriteSnapshotUseCase,
                    toggleFavoriteUseCase = app.container.toggleFavoriteUseCase,
                    updatePreferenceUseCase = app.container.updatePreferenceUseCase,
                    reverseGeocodeLocationUseCase = app.container.reverseGeocodeLocationUseCase
                )
            }
        }
    }
}