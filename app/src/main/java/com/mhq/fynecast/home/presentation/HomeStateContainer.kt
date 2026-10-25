package com.mhq.fynecast.home.presentation

import com.mhq.fynecast.home.presentation.model.CitySuggestionPresentationModel

data class HomeStateContainer(
    val uiState: HomeUiState = HomeUiState.Loading,
    val selectedCityName: String = "",
    val citySuggestions: List<CitySuggestionPresentationModel> = emptyList(),
    val isFavorite: Boolean = false,
    /** True while a new city loads and the previous weather is still on screen. */
    val isRefreshing: Boolean = false
)
