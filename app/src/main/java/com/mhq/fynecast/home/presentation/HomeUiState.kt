package com.mhq.fynecast.home.presentation

import com.mhq.fynecast.home.presentation.model.WeatherPresentationModel

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val weather: WeatherPresentationModel) : HomeUiState
    data class Error(val retry: HomeRetryAction) : HomeUiState
}
