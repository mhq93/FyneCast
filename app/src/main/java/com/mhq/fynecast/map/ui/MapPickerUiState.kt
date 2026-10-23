package com.mhq.fynecast.map.ui

import com.mhq.fynecast.core.ui.text.UiText
import com.mhq.fynecast.core.domain.location.CitySuggestionModel

/**
 * Where the user last tapped
 * Set immediately on tap so the marker survives rotation
 * and appears before the place name has been resolved.
 * */
data class MapPickerUiState(
    val selectedLocation: CitySuggestionModel? = null,
    val markerLatitude: Double? = null,
    val markerLongitude: Double? = null,
    val isSearching: Boolean = false,
    val errorMessage: UiText? = null
)