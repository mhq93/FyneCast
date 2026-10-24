package com.mhq.fynecast.map.presentation

import com.mhq.fynecast.core.ui.text.UiText
import com.mhq.fynecast.map.presentation.model.MapPlacePresentationModel

/**
 * What the map picker screen shows.
 *
 * The marker coordinates are set immediately on tap so the marker survives rotation
 * and appears before the place name has been resolved.
 */
data class MapPickerUiState(
    val selectedPlace: MapPlacePresentationModel? = null,
    val markerLatitude: Double? = null,
    val markerLongitude: Double? = null,
    val isSearching: Boolean = false,
    val errorMessage: UiText? = null
)
