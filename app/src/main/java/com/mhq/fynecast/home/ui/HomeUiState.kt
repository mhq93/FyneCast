package com.mhq.fynecast.home.ui

import com.mhq.fynecast.home.domain.models.WeatherDomainModel

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val weather: WeatherDomainModel) : HomeUiState
    data class Error(val message: String) : HomeUiState
}