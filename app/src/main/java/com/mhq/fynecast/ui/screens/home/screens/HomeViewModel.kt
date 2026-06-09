//package com.mhq.fynecast.home
//
//import android.util.Log
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.setValue
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
//import androidx.lifecycle.viewModelScope
//import androidx.lifecycle.viewmodel.initializer
//import androidx.lifecycle.viewmodel.viewModelFactory
//import com.mhq.fynecast.AppRepository
//import com.mhq.fynecast.FyneCastApplication
//import com.mhq.fynecast.home.domain.ToggleFavoriteUseCase
//import com.mhq.fynecast.favorites.database.FavoriteEntity
//import com.mhq.fynecast.favorites.repository.FavoritesRepository
//import com.mhq.fynecast.home.location.autocomplete.dto.CitySuggestion
//import com.mhq.fynecast.home.location.LocationTracker
//import com.mhq.fynecast.home.location.autocomplete.PhotonLocationRepository
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.flatMapLatest
//import kotlinx.coroutines.flow.stateIn
//import kotlinx.coroutines.launch
//import retrofit2.HttpException
//import java.io.IOException
//
//@OptIn(ExperimentalCoroutinesApi::class)
//class HomeViewModel(
//    private val appRepository: AppRepository,
//    private val favoritesRepository: FavoritesRepository,
//    private val photonLocationRepository: PhotonLocationRepository,
//    private val locationTracker: LocationTracker,
//    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
//    ) : ViewModel() {
//    private val _homeUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
//    val homeUiState = _homeUiState.asStateFlow()
//
//    init {
//        fetchWeatherData()
//    }
//
//    fun fetchWeatherData(queryCity: String? = null) {
//        viewModelScope.launch {
//            _homeUiState.value = HomeUiState.Loading
//            val finalQuery = if (queryCity == null) {
//                val location = try {
//                    locationTracker.getCurrentLocation()
//                } catch (e: Exception) {
//                    null
//                }
//                if (location != null) {
//                    "${location.latitude},${location.longitude}"
//                } else {
//                    "Cairo"
//                }
//            } else {
//                queryCity
//            }
//            try {
//                val result = appRepository.provideWeatherData(finalQuery)
//                selectedCityName = "${result.location.cityName}, ${result.location.country}"
//                _homeUiState.value = HomeUiState.Success(result)
//            } catch (e: Exception) {
//                _homeUiState.value = HomeUiState.Error(e.message ?: "Unknown Error")
//            }
//        }
//    }
//
////    fun fetchWeatherData(queryCity: String? = null) {
////        viewModelScope.launch {
////            _homeUiState.value = HomeUiState.Loading
////
////            // 1. Try to get GPS location if no specific city is provided
////            val finalQuery = if (queryCity == null) {
////                try {
////                    delay(500) // Give GMS a moment to stabilize
////                    val location = locationTracker.getCurrentLocation()
////                    if (location != null) {
////                        "${location.latitude},${location.longitude}"
////                    } else {
////                        "Cairo" // Fallback if GPS is null
////                    }
////                } catch (e: SecurityException) {
////                    Log.e("HomeViewModel", "GMS Security Error: ${e.message}")
////                    "Cairo" // Fallback if GMS package error occurs
////                } catch (e: Exception) {
////                    "Cairo"
////                }
////            } else {
////                queryCity
////            }
////
////            // 2. Fetch data using the finalQuery (either GPS coords or City Name)
////            _homeUiState.value = try {
////                val result = appRepository.provideWeatherData(finalQuery)
////                selectedCityName = "${result.location.cityName}, ${result.location.country}"
////                HomeUiState.Success(result)
////            } catch (e: IOException) {
////                HomeUiState.Error("No Internet Connection")
////            } catch (e: HttpException) {
////                HomeUiState.Error("Server Error: ${e.code()}")
////            } catch (e: Exception) {
////                HomeUiState.Error(e.message ?: "Unknown Error")
////            }
////        }
////    }
//
//
////    fun fetchWeatherData(queryCity: String = "Cairo") {
////        viewModelScope.launch {
////            _homeUiState.value = HomeUiState.Loading
////            val location = locationTracker.getCurrentLocation()
////
////            _homeUiState.value = try {
////                val result = appRepository.provideWeatherData(queryCity)
////                selectedCityName = "${result.location.cityName}, ${result.location.country}"
////                HomeUiState.Success(appRepository.provideWeatherData(queryCity))
////            } catch (e: IOException) {
////                HomeUiState.Error(e.message ?: "Network error")
////            } catch (e: HttpException) {
////                HomeUiState.Error(e.message ?: "Server error")
////            }
////        }
////    }
//
//    private var locationAutoSearch: Job? = null
//    var citySuggestions by mutableStateOf<List<CitySuggestion>>(emptyList())
//        private set
//
//    var selectedCityName by mutableStateOf("Select City")
//        //private set
//
//    fun onSearchQueryChanged(newQuery: String) {
//        locationAutoSearch?.cancel()
//        if (newQuery.length > 2) {
//            locationAutoSearch = viewModelScope.launch {
//                delay(500)
//                try {
//                    val suggestions = photonLocationRepository.getCitySuggestions(newQuery)
//                    citySuggestions = suggestions
//                } catch (e: Exception) {
//                    citySuggestions = emptyList()
//                }
//            }
//        }
//    }
//
//    fun onCitySelected(suggestion: CitySuggestion) {
//        citySuggestions = emptyList()
//        selectedCityName = suggestion.fullName
//        val locationQuery = "${suggestion.latitude},${suggestion.longitude}"
//        viewModelScope.launch {
//            _homeUiState.value = HomeUiState.Loading
//            try {
//                val result = appRepository.provideWeatherData(locationQuery)
//                _homeUiState.value = HomeUiState.Success(result)
//            } catch (e: Exception) {
//                _homeUiState.value = HomeUiState.Error(e.message ?: "Unknown Error")
//            }
//        }
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    val isCurrentCityFavorite: StateFlow<Boolean> = homeUiState
//        .flatMapLatest { state ->
//            if (state is HomeUiState.Success) {
//                favoritesRepository.isFavoriteFlow(
//                    state.weatherData.location.cityName,
//                    state.weatherData.location.country
//                )
//            } else {
//                kotlinx.coroutines.flow.flowOf(false)
//            }
//        }.stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = false
//        )
//
//
//    //2
////    fun isCityFavored(name: String, country: String): StateFlow<Boolean> {
////        return favoritesRepository.isFavoriteFlow(name, country)
////            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
////    }
//
////1
//    //    fun isCityFavored(lat: Double, lon: Double): StateFlow<Boolean> {
//    //        return favoritesRepository.isFavoriteFlow(lat, lon)
//    //            .stateIn(
//    //                scope = viewModelScope,
//    //                started = SharingStarted.WhileSubscribed(5000),
//    //                initialValue = false
//    //            )
//    //    }
//
//    fun onToggleFavorite(favoriteEntity: FavoriteEntity) {
//        viewModelScope.launch {
//            toggleFavoriteUseCase(favoriteEntity)
//        }
//    }
//
//
//    companion object {
//        val factory: ViewModelProvider.Factory = viewModelFactory {
//            initializer {
//                val application = (this[APPLICATION_KEY] as FyneCastApplication)
//                val appRepository = application.container.appRepository
//                HomeViewModel(
//                    appRepository = appRepository,
//                    locationTracker = application.container.locationTracker,
//                    photonLocationRepository = application.container.photonLocationRepository,
//                    favoritesRepository = application.container.favoritesRepository,
//                    toggleFavoriteUseCase = application.container.toggleFavoriteUseCase
//                    )
//            }
//        }
//    }
//}
//


package com.mhq.fynecast.ui.screens.home

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
import com.mhq.fynecast.AppRepository
import com.mhq.fynecast.FyneCastApplication
import com.mhq.fynecast.favorites.database.FavoriteEntity
import com.mhq.fynecast.favorites.repository.FavoritesRepository
import com.mhq.fynecast.home.domain.ToggleFavoriteUseCase
import com.mhq.fynecast.home.location.LocationTracker
import com.mhq.fynecast.home.location.autocomplete.PhotonLocationRepository
import com.mhq.fynecast.home.location.autocomplete.dto.CitySuggestion
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val appRepository: AppRepository,
    private val favoritesRepository: FavoritesRepository,
    private val photonLocationRepository: PhotonLocationRepository,
    private val locationTracker: LocationTracker,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _homeUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeUiState = _homeUiState.asStateFlow()

    // Prevents the app from re-triggering GPS every time you switch tabs
    private var isInitialLaunchPerformed = false

    var citySuggestions by mutableStateOf<List<CitySuggestion>>(emptyList())
        private set

    var selectedCityName by mutableStateOf("")

    private var locationAutoSearch: Job? = null

    init {
        // App starts here
        fetchWeatherData(null)
    }

    /**
     * core logic for fetching weather.
     * @param queryCity if null, we attempt GPS. If GPS fails/disabled, we use "Cairo".
     */
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
                    Log.e("HomeViewModel", "GPS Error: ${e.message}")
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
                Log.e("HomeViewModel", "API Error: ${e.message}")
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
                delay(500)
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

    fun onCitySelected(suggestion: CitySuggestion) {
        citySuggestions = emptyList()
        selectedCityName = suggestion.fullName
        val locationQuery = "${suggestion.latitude},${suggestion.longitude}"
        fetchWeatherData(locationQuery)
    }

    /**
     * Reactive state for the Heart Icon.
     * Listens to the current successful city and checks the DB.
     */
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

//    val isCurrentCityFavorite: StateFlow<Boolean> = snapshotFlow { selectedCityName }
//        .map { fullName ->
//            val parts = fullName.split(",")
//            val name = parts.getOrNull(0)?.trim() ?: ""
//            val country = parts.getOrNull(1)?.trim() ?: ""
//            name to country
//        }
//        .distinctUntilChanged()
//        .flatMapLatest { (name, country) ->
//            favoritesRepository.isFavoriteFlow(name, country)
//        }
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
                    photonLocationRepository = container.photonLocationRepository,
                    favoritesRepository = container.favoritesRepository,
                    toggleFavoriteUseCase = container.toggleFavoriteUseCase
                )
            }
        }
    }
}