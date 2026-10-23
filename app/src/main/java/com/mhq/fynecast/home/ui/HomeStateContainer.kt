package com.mhq.fynecast.home.ui

import com.mhq.fynecast.core.domain.location.CitySuggestionModel

data class HomeStateContainer(
    val uiState: HomeUiState = HomeUiState.Loading,
    val selectedCityName: String = "",
    val citySuggestions: List<CitySuggestionModel> = emptyList(),
    val isFavorite: Boolean = false,
    /** True while a new city loads and the previous weather is still on screen. */
    val isRefreshing: Boolean = false
)