package com.mhq.fynecast.ui.screens.home.screens

import com.mhq.fynecast.data.network.weather.WeatherApiResponse

sealed interface HomeUiState{
    data class Success(val weatherData: WeatherApiResponse) : HomeUiState
    data object Loading: HomeUiState
    data class Error(val message: String) : HomeUiState
}