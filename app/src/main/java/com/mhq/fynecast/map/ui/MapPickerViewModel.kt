package com.mhq.fynecast.map.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.map.domain.usecases.ReverseGeocodeLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapPickerViewModel(
    private val reverseGeocodeLocationUseCase: ReverseGeocodeLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapPickerUiState())
    val uiState: StateFlow<MapPickerUiState> = _uiState.asStateFlow()

    fun onMapClicked(latitude: Double, longitude: Double, langCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, selectedLocation = null, errorMessage = null) }
            try {
                val resolvedLocation = reverseGeocodeLocationUseCase(latitude, longitude, langCode)
                if (resolvedLocation != null) {
                    _uiState.update { it.copy(selectedLocation = resolvedLocation) }
                } else {
                    _uiState.update {
                        it.copy(errorMessage = "No location data found here. Please pick a more specific location.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Couldn't resolve this location. Please try again.")
                }
            } finally {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FyneCastApplication
                MapPickerViewModel(
                    reverseGeocodeLocationUseCase = app.container.reverseGeocodeLocationUseCase
                )
            }
        }
    }
}