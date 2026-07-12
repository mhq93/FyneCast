package com.mhq.fynecast.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.usecases.ToggleFavoriteUseCase
import com.mhq.fynecast.home.domain.models.GeoCoordinates
import com.mhq.fynecast.home.domain.usecases.FetchWeatherDataUseCase
import com.mhq.fynecast.home.domain.usecases.GetCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.GetLastSearchedCityUseCase
import com.mhq.fynecast.home.domain.usecases.GetLocationSuggestionsUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveFavoriteStatusUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class HomeViewModel(
    private val fetchWeatherDataUseCase: FetchWeatherDataUseCase,
    private val getLocationSuggestionsUseCase: GetLocationSuggestionsUseCase,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val observeFavoriteStatusUseCase: ObserveFavoriteStatusUseCase,
    private val getLastSearchedCityUseCase: GetLastSearchedCityUseCase,
    private val updatePreferenceUseCase: UpdatePreferenceUseCase,
    private val defaultCityFallback: String = "Cairo"
) : ViewModel() {

    private val _state = MutableStateFlow(HomeStateContainer())
    val state: StateFlow<HomeStateContainer> = _state.asStateFlow()

    private var lastRefreshTime = 0L
    private val refreshCooldownMillis = 60 * 1000L

    var isInitialLaunchPerformed = false
        private set

    private var searchJob: Job? = null
    private var favoriteJob: Job? = null

    fun fetchInitialLocationWeather(langCode: String) {
        fetchWeatherData(
            queryCity = null,
            langCode = langCode,
            locationProvider = { getCurrentLocationUseCase() }
        )
    }

    fun fetchWeatherData(
        queryCity: String? = null,
        langCode: String,
        locationProvider: (suspend () -> GeoCoordinates?)? = null
    ) {
        viewModelScope.launch {
            if (queryCity == null && isInitialLaunchPerformed) return@launch
            _state.update { it.copy(uiState = HomeUiState.Loading) }

            try {
                val finalQuery = if (queryCity == null) {
                    delay(300.milliseconds)
                    val coordinates = runCatching { locationProvider?.invoke() }.getOrNull()

                    if (coordinates != null) {
                        "${coordinates.latitude},${coordinates.longitude}"
                    } else {
                        val savedCity = getLastSearchedCityUseCase()
                        savedCity?.let { "${it.latitude},${it.longitude}" } ?: defaultCityFallback
                    }
                } else {
                    queryCity
                }

                val domainWeather = fetchWeatherDataUseCase(finalQuery, langCode)

                val displayCityName = if (queryCity == null) {
                    "${domainWeather.cityName}, ${domainWeather.country}"
                } else {
                    _state.value.selectedCityName
                }

                _state.update {
                    it.copy(
                        uiState = HomeUiState.Success(domainWeather),
                        selectedCityName = displayCityName
                    )
                }
                isInitialLaunchPerformed = true
                trackFavoriteObservation(displayCityName)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        uiState = HomeUiState.Error(e.message ?: "Unknown Error"),
                        selectedCityName = if (queryCity == null) "$defaultCityFallback, Egypt" else it.selectedCityName
                    )
                }
            }
        }
    }

    fun refreshCurrentCity(langCode: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRefreshTime < refreshCooldownMillis) return

        viewModelScope.launch {
            val currentCity = _state.value.selectedCityName
            if (currentCity.isEmpty()) return@launch

            lastRefreshTime = currentTime
            try {
                val domainWeather = fetchWeatherDataUseCase(currentCity, langCode)
                _state.update { it.copy(uiState = HomeUiState.Success(domainWeather)) }
            } catch (e: Exception) {
            }
        }
    }

    fun onLocationPermissionResult(isGranted: Boolean, langCode: String) {
        if (isGranted) {
            fetchInitialLocationWeather(langCode)
        } else {
            fetchWeatherData(queryCity = null, langCode = langCode)
        }
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

    fun onCitySelected(suggestionName: String, lat: Double, lon: Double, langCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(selectedCityName = suggestionName) }
            updatePreferenceUseCase.lastSearchedCity(name = suggestionName, lat = lat, lon = lon)
            fetchWeatherData(queryCity = "$lat,$lon", langCode = langCode)
        }
    }

    fun onToggleFavorite() {
        val currentState = _state.value
        val weatherSuccess = currentState.uiState as? HomeUiState.Success ?: return

        viewModelScope.launch {
            // Same name/country split InsertFavoriteUseCase used to do internally —
            // kept here since toggleFavoriteUseCase needs the fields separately, not a combined string.
            val parts = currentState.selectedCityName.split(",")
            val cleanCityName = parts.firstOrNull()?.trim() ?: weatherSuccess.weather.cityName
            val cleanCountryName = parts.getOrNull(1)?.trim() ?: weatherSuccess.weather.country

            toggleFavoriteUseCase(
                FavoriteCityDomainModel(
                    cityName = cleanCityName,
                    countryName = cleanCountryName,
                    latitude = weatherSuccess.weather.latitude,
                    longitude = weatherSuccess.weather.longitude,
                    temperatureC = weatherSuccess.weather.currentTempC,
                    temperatureF = weatherSuccess.weather.currentTempF,
                    weatherIconUrl = weatherSuccess.weather.conditionIcon
                )
            )
        }
    }

    private fun trackFavoriteObservation(fullName: String) {
        favoriteJob?.cancel()
        favoriteJob = observeFavoriteStatusUseCase(fullName)
            .onEach { isFav ->
                _state.update { it.copy(isFavorite = isFav) }
            }
            .launchIn(viewModelScope)
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FyneCastApplication
                HomeViewModel(
                    fetchWeatherDataUseCase = app.container.fetchWeatherDataUseCase,
                    getLocationSuggestionsUseCase = app.container.getLocationSuggestionsUseCase,
                    getCurrentLocationUseCase = app.container.getCurrentLocationUseCase,
                    toggleFavoriteUseCase = app.container.toggleFavoriteUseCase,
                    observeFavoriteStatusUseCase = app.container.observeFavoriteStatusUseCase,
                    getLastSearchedCityUseCase = app.container.getLastSearchedCityUseCase,
                    updatePreferenceUseCase = app.container.updatePreferenceUseCase
                )
            }
        }
    }
}