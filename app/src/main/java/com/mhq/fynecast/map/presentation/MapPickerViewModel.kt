package com.mhq.fynecast.map.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.R
import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.core.domain.location.SelectCityUseCase
import com.mhq.fynecast.core.domain.location.SelectedCity
import com.mhq.fynecast.core.ui.text.UiText
import com.mhq.fynecast.map.domain.usecases.ReverseGeocodeLocationUseCase
import com.mhq.fynecast.map.presentation.mapper.toPresentation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapPickerViewModel(
    private val selectCityUseCase: SelectCityUseCase,
    private val reverseGeocodeLocationUseCase: ReverseGeocodeLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapPickerUiState())
    val uiState: StateFlow<MapPickerUiState> = _uiState.asStateFlow()

    private var geocodeJob: Job? = null

    /** The domain city for the last tap, kept privately: the screen only sees the presentation model. */
    private var resolvedCity: CitySuggestionModel? = null

    fun onMapClicked(latitude: Double, longitude: Double, langCode: String) {
        geocodeJob?.cancel()
        resolvedCity = null
        geocodeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearching = true,
                    selectedPlace = null,
                    errorMessage = null,
                    markerLatitude = latitude,
                    markerLongitude = longitude
                )
            }
            try {
                val resolved = reverseGeocodeLocationUseCase(latitude, longitude, langCode)
                if (resolved != null) {
                    resolvedCity = resolved
                    _uiState.update { it.copy(selectedPlace = resolved.toPresentation()) }
                } else {
                    _uiState.update {
                        it.copy(errorMessage = UiText.res(R.string.error_map_no_location))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = UiText.res(R.string.error_map_resolve_failed))
                }
            }
            _uiState.update { it.copy(isSearching = false) }
        }
    }

    /** The user pressed "Confirm" on the card: publish the city that was resolved for the last tap. */
    fun onLocationConfirmed() {
        val city = resolvedCity ?: return
        viewModelScope.launch {
            selectCityUseCase(SelectedCity(city.fullName, city.latitude, city.longitude))
        }
    }
}
