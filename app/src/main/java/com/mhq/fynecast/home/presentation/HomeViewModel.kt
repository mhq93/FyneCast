package com.mhq.fynecast.home.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.core.domain.location.ObserveSelectedCityUseCase
import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshFavoriteSnapshotUseCase
import com.mhq.fynecast.favorites.domain.usecases.ToggleFavoriteUseCase
import com.mhq.fynecast.home.domain.mapper.toFavoriteCity
import com.mhq.fynecast.home.domain.usecases.GetLocationSuggestionsUseCase
import com.mhq.fynecast.home.domain.usecases.LoadCurrentWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.LocalizePlaceNameUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveFavoriteStatusUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveSignedInUserIdUseCase
import com.mhq.fynecast.home.domain.usecases.ResolveStartupLocationUseCase
import com.mhq.fynecast.home.presentation.mapper.toPresentation
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
    private val resolveStartupLocationUseCase : ResolveStartupLocationUseCase,
    private val observeSelectedCityUseCase: ObserveSelectedCityUseCase,
    private val getLocationSuggestionsUseCase: GetLocationSuggestionsUseCase,
    private val observeSignedInUserIdUseCase: ObserveSignedInUserIdUseCase,
    private val observeFavoriteStatusUseCase: ObserveFavoriteStatusUseCase,
    private val refreshFavoriteSnapshotUseCase: RefreshFavoriteSnapshotUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteFavoriteUseCase: DeleteFavoriteUseCase,
    private val saveLastSearchedCityUseCase: SaveLastSearchedCityUseCase,
    private val reverseGeocodeLocationUseCase: ReverseGeocodeLocationUseCase,
    private val localizePlaceNameUseCase: LocalizePlaceNameUseCase,
    private val defaultCityFallback: String = "Cairo"
) : ViewModel() {

    private val _state = MutableStateFlow(HomeStateContainer())
    val state: StateFlow<HomeStateContainer> = _state.asStateFlow()

    /**
     * The domain weather behind the Success state, kept privately: the screen only ever sees
     * presentation models. It is null whenever the state is not Success.
     */
    private var domainWeather: WeatherDomainModel? = null

    /** The domain suggestions behind the search dropdown; the screen sends back an id. */
    private var domainSuggestions: List<CitySuggestionModel> = emptyList()

    private var loadTarget: LoadTarget = LoadTarget.Startup(useDeviceLocation = false)
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var favoriteJob: Job? = null

    /** The id the saved favorite for the city on screen has, or null when it is not a favorite. */
    private var savedFavoriteId: String? = null
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

        observeSelectedCityUseCase()
            .onEach { city ->
                selectCity(city.name, city.latitude, city.longitude, latestLangCode ?: "en")
            }
            .launchIn(viewModelScope)
    }

    fun onLocationPermissionResult(isGranted: Boolean, langCode: String) {
        load(LoadTarget.Startup(useDeviceLocation = isGranted), langCode)
    }

    /** The user picked a row of the search dropdown. */
    fun onSuggestionSelected(suggestionId: String, langCode: String) {
        val suggestion = domainSuggestions.firstOrNull { it.toPresentation().id == suggestionId }
            ?: return
        selectCity(suggestion.fullName, suggestion.latitude, suggestion.longitude, langCode)
    }

    private fun selectCity(
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
                domainWeather = loaded.weather
                _state.update {
                    it.copy(
                        uiState = HomeUiState.Success(loaded.weather.toPresentation()),
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
                showError(target.retry)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("HomeViewModel", "Load failed", e)
                showError(target.retry)
            }
        }
    }

    private fun showError(retry: HomeRetryAction) {
        domainWeather = null
        _state.update {
            it.copy(
                uiState = HomeUiState.Error(retry),
                isRefreshing = false
            )
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
                delay(300.milliseconds)
                val spot = resolveStartupLocationUseCase(target.useDeviceLocation)
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

    fun onRefreshRequested(langCode: String) {
        val now = System.currentTimeMillis()
        if (now - lastRefreshTime < refreshCooldownMillis) return
        if (loadJob?.isActive == true) return
        val weather = domainWeather ?: return

        loadJob = viewModelScope.launch {
            try {
                val fresh =
                    loadCurrentWeatherUseCase("${weather.latitude},${weather.longitude}", langCode)
                domainWeather = fresh
                _state.update { it.copy(uiState = HomeUiState.Success(fresh.toPresentation())) }
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
        val weather = domainWeather ?: return
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
        savedFavoriteId = null
        _state.value = HomeStateContainer()
        domainWeather = null
        domainSuggestions = emptyList()
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
            domainSuggestions = suggestions
            _state.update { it.copy(citySuggestions = suggestions.map { s -> s.toPresentation() }) }
        }
    }

    fun onToggleFavorite() {
        val weather = domainWeather ?: return
        val savedId = savedFavoriteId
        viewModelScope.launch {
            if (savedId != null) {
                // Remove the favorite that is actually saved, whichever coordinates it was saved under.
                deleteFavoriteUseCase(savedId)
            } else {
                toggleFavoriteUseCase(weather.toFavoriteCity(_state.value.selectedCityName))
            }
        }
    }

    private fun onWeatherShown(weather: WeatherDomainModel) {
        favoriteJob?.cancel()
        // The point the weather service reports and the point we asked about can differ,
        // so a favorite saved under either one is this city.
        val places = buildList {
            add(weather.latitude to weather.longitude)
            shownSpot?.let { add(it.latitude to it.longitude) }
        }
        favoriteJob = observeFavoriteStatusUseCase(places)
            .onEach { savedId ->
                savedFavoriteId = savedId
                _state.update { it.copy(isFavorite = savedId != null) }
            }
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
