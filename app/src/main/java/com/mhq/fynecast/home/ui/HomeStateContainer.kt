package com.mhq.fynecast.home.ui

import com.mhq.fynecast.home.domain.models.CitySuggestionModel

data class HomeStateContainer(
    val uiState: HomeUiState = HomeUiState.Loading,
    val selectedCityName: String = "",
    val citySuggestions: List<CitySuggestionModel> = emptyList(),
    val isFavorite: Boolean = false
)