package com.mhq.fynecast.home.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.home.ui.HomeUiState
import com.mhq.fynecast.home.ui.HomeViewModel

@Composable
fun HomeContainer(
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory),
    onNavigateToMap: () -> Unit,
    onAlertsBannerClick: () -> Unit,
    onRequestPermission: (String) -> Unit,
    isMetric: Boolean,
    langCode: String,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {

    val stateContainer by homeViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (!homeViewModel.isInitialLaunchPerformed) {
            onRequestPermission(langCode)
        }
    }

    // 2. Read the nested uiState directly from the container wrapper
    when (val uiState = stateContainer.uiState) {
        is HomeUiState.Loading -> {
            HomeLoading(contentPadding = contentPadding, modifier = modifier)
        }

        is HomeUiState.Success -> {
            HomeContent(
                weatherDomainModel = uiState.weather,
                citySuggestions = stateContainer.citySuggestions,
                selectedCityName = stateContainer.selectedCityName,
                isMetric = isMetric,
                isFavorite = stateContainer.isFavorite,
                onQueryChange = { homeViewModel.onSearchQueryChanged(it, langCode) },
                onSuggestionClick = { suggestion ->
                    homeViewModel.onCitySelected(
                        suggestion.fullName,
                        suggestion.latitude,
                        suggestion.longitude,
                        langCode
                    )
                },
                onMapIconClick = onNavigateToMap,
                onAlertsBannerClick = onAlertsBannerClick,
                onToggleFavorite = { homeViewModel.onToggleFavorite() },
                contentPadding = contentPadding,
                modifier = modifier
            )
        }

        is HomeUiState.Error -> {
            HomeError(
                retryAction = {
                    onRequestPermission(langCode)
                    homeViewModel.fetchWeatherData(langCode = langCode)
                },
                contentPadding = contentPadding,
                modifier = modifier
            )
        }
    }
}