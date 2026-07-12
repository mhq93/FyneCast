package com.mhq.fynecast.map.ui

import com.mhq.fynecast.home.domain.models.CitySuggestionModel

data class MapPickerUiState(
    val selectedLocation: CitySuggestionModel? = null,
    val isSearching: Boolean = false,
    val errorMessage: String? = null
)