package com.mhq.fynecast.ui.screens.home.screens

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.data.repository.AppRepository
import com.mhq.fynecast.FyneCastApplication
import com.mhq.fynecast.data.database.FavoriteEntity
import com.mhq.fynecast.data.repository.FavoritesRepository
import com.mhq.fynecast.domain.usecases.favorites.ToggleFavoriteUseCase
import com.mhq.fynecast.data.network.location.LocationTracker
import com.mhq.fynecast.data.repository.PhotonLocationRepository
import com.mhq.fynecast.domain.models.CitySuggestionModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val appRepository: AppRepository,
    private val locationTracker: LocationTracker,
    private val favoritesRepository: FavoritesRepository,
    private val photonLocationRepository: PhotonLocationRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _homeUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeUiState = _homeUiState.asStateFlow()

    // Prevents the app from re-triggering GPS every time you switch tabs
    private var isInitialLaunchPerformed = false

    var citySuggestions by mutableStateOf<List<CitySuggestionModel>>(emptyList())
        private set

    var selectedCityName by mutableStateOf("")

    private var locationAutoSearch: Job? = null

    init {
        // App starts here
        fetchWeatherData(null)
    }

    fun fetchWeatherData(queryCity: String? = null) {
        viewModelScope.launch {
            // Guard: If we are switching tabs and already have data, don't re-fetch GPS
            if (queryCity == null && isInitialLaunchPerformed) return@launch

            _homeUiState.value = HomeUiState.Loading

            val finalQuery = if (queryCity == null) {
                val gpsLocation = try {
                    // Small delay to ensure Google Play Services is ready
                    delay(300)
                    locationTracker.getCurrentLocation()
                } catch (e: Exception) {
                    null
                }
                // If GPS is disabled or null, Cairo is the hard fallback
                gpsLocation?.let { "${it.latitude},${it.longitude}" } ?: "Cairo"
            } else {
                queryCity
            }

            try {
                val result = appRepository.provideWeatherData(finalQuery)
                selectedCityName = "${result.location.cityName}, ${result.location.country}"
                _homeUiState.value = HomeUiState.Success(result)

                // Successfully loaded the first time, lock the automatic refresh
                isInitialLaunchPerformed = true
            } catch (e: Exception) {
                _homeUiState.value = HomeUiState.Error(e.message ?: "Unknown Error")
                // Ensure we don't stay on a "Loading" state if the fallback fails
                if (queryCity == null) selectedCityName = "Cairo, Egypt"
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        locationAutoSearch?.cancel()
        if (newQuery.length > 2) {
            locationAutoSearch = viewModelScope.launch {
                delay(500.milliseconds)
                try {
                    val suggestions = photonLocationRepository.getCitySuggestions(newQuery)
                    citySuggestions = suggestions
                } catch (e: Exception) {
                    citySuggestions = emptyList()
                }
            }
        } else {
            citySuggestions = emptyList()
        }
    }

    fun onCitySelected(suggestion: CitySuggestionModel) {
        citySuggestions = emptyList()
        selectedCityName = suggestion.fullName
        val locationQuery = "${suggestion.latitude},${suggestion.longitude}"
        fetchWeatherData(locationQuery)
    }

    val isCurrentCityFavorite: StateFlow<Boolean> = snapshotFlow { selectedCityName }
        .map { fullName ->
            val name = fullName.split(",").firstOrNull()?.trim() ?: ""
            val country = fullName.split(",").getOrNull(1)?.trim() ?: ""
            name to country
        }
        .distinctUntilChanged()
        .flatMapLatest { (name, country) ->
            favoritesRepository.isFavoriteFlow(name, country)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onToggleFavorite(favoriteEntity: FavoriteEntity) {
        viewModelScope.launch {
            toggleFavoriteUseCase(favoriteEntity)
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as FyneCastApplication)
                val container = application.container
                HomeViewModel(
                    appRepository = container.appRepository,
                    locationTracker = container.locationTracker,
                    favoritesRepository = container.favoritesRepository,
                    photonLocationRepository = container.photonLocationRepository,
                    toggleFavoriteUseCase = container.toggleFavoriteUseCase
                )
            }
        }
    }
}