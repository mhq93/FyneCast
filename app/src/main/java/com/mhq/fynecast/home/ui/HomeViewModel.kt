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
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val defaultCityFallback: String = "Cairo"
) : ViewModel() {

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

    private val _state = MutableStateFlow(HomeStateContainer())
    val state: StateFlow<HomeStateContainer> = _state.asStateFlow()

    private var lastRefreshTime = 0L
    private val refreshCooldownMillis = 60 * 1000L

    /** True once any load has been requested; the UI uses it to ask for location only once. */
    var isInitialLaunchPerformed = false
        private set

    private var loadTarget: LoadTarget = LoadTarget.Startup(useDeviceLocation = false)
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var favoriteJob: Job? = null

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
                val (weather, displayName) = when (target) {
                    is LoadTarget.Place ->
                        loadCurrentWeatherUseCase(
                            "${target.latitude},${target.longitude}",
                            langCode
                        ) to target.name

                    is LoadTarget.Startup -> {
                        val weather = loadCurrentWeatherUseCase(startupQuery(target), langCode)
                        weather to "${weather.cityName}, ${weather.country}"
                    }
                }

                _state.update {
                    it.copy(uiState = HomeUiState.Success(weather), selectedCityName = displayName)
                }
                onWeatherShown(weather)
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

    /** GPS (when allowed) → last searched city → default city. */
    private suspend fun startupQuery(target: LoadTarget.Startup): String {
        delay(300.milliseconds)
        val coordinates = if (target.useDeviceLocation) currentCoordinatesOrNull() else null
        if (coordinates != null) return "${coordinates.latitude},${coordinates.longitude}"

        val savedCity = getLastSearchedCityUseCase()
        return savedCity?.let { "${it.latitude},${it.longitude}" } ?: defaultCityFallback
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
                onWeatherShown(fresh)
                lastRefreshTime =
                    System.currentTimeMillis()  // only after success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh weather", e)
            }
        }
    }

    private fun resetForNewSession() {
        loadJob?.cancel()
        searchJob?.cancel()
        favoriteJob?.cancel()
        _state.value = HomeStateContainer()
        isInitialLaunchPerformed = false
        lastRefreshTime = 0L
        loadTarget = LoadTarget.Startup(useDeviceLocation = false)
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
        val weather = (_state.value.uiState as? HomeUiState.Success)?.weather ?: return
        viewModelScope.launch { toggleFavoriteUseCase(weather.toFavoriteCity()) }
    }

    private fun onWeatherShown(weather: WeatherDomainModel) {
        favoriteJob?.cancel()
        favoriteJob = observeFavoriteStatusUseCase(weather.latitude, weather.longitude)
            .onEach { isFav -> _state.update { it.copy(isFavorite = isFav) } }
            .launchIn(viewModelScope)

        // Keeps a favorite's temperature and icon current whenever its city is shown.
        viewModelScope.launch { refreshFavoriteSnapshotUseCase(weather.toFavoriteCity()) }
    }

    companion object {
        private const val TAG = "HomeViewModel"

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
                    updatePreferenceUseCase = app.container.updatePreferenceUseCase
                )
            }
        }
    }
}