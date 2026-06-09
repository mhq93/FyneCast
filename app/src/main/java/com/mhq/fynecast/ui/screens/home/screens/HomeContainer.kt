package com.mhq.fynecast.ui.screens.home.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.data.database.FavoriteEntity

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeContainer(
    viewModel: HomeViewModel,
    onNavigateToMap: () -> Unit,
    onRequestPermission: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
    isMetric: Boolean
) {
    val homeUiState by viewModel.homeUiState.collectAsState()

    when (val state = homeUiState) {

        is HomeUiState.Loading -> {
            HomeLoading(
                contentPadding = contentPadding,
                modifier = modifier
            )
        }

        is HomeUiState.Success -> {
            val isFavorite by viewModel.isCurrentCityFavorite.collectAsState()

            // FIX: Swap HomeScreen out for your clean, stateless layout component
            HomeContent(
                responseData = state.weatherData,
                citySuggestions = viewModel.citySuggestions,
                selectedCityName = viewModel.selectedCityName,
                isFavorite = isFavorite,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onSuggestionClick = { viewModel.onCitySelected(it) },
                onMapIconClick = onNavigateToMap,
                onToggleFavorite = {
                    val favoriteEntity = FavoriteEntity(
                        cityName = viewModel.selectedCityName.split(",").first().trim(),
                        countryName = state.weatherData.location.country,
                        latitude = state.weatherData.location.cityLatitude,
                        longitude = state.weatherData.location.cityLongitude,
                        favCityTemp = state.weatherData.current.tempC,
                        favCityWeatherIcon = state.weatherData.current.condition.icon
                    )
                    viewModel.onToggleFavorite(favoriteEntity)
                },
                contentPadding = contentPadding,
                modifier = modifier,
                isMetric = isMetric
            )
        }

        is HomeUiState.Error -> {
            HomeError(
                retryAction = {
                    onRequestPermission()
                    viewModel.fetchWeatherData("Cairo")
                },
                contentPadding = contentPadding,
                modifier = modifier
            )
        }
    }
}