package com.mhq.fynecast.ui.screens.alerts.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.data.repository.AppRepository
import com.mhq.fynecast.FyneCastApplication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AlertsViewModel(
    private val repository: AppRepository
) : ViewModel() {

    val uiState: StateFlow<AlertsUiState> = repository.weatherDataCache
        .map { response ->
            if (response == null) {
                AlertsUiState.Empty
            } else {
                val alertList = response.alerts?.alert ?: emptyList()
                if (alertList.isEmpty()) {
                    AlertsUiState.NoAlerts
                } else {
                    AlertsUiState.Success(alerts = alertList)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlertsUiState.Loading
        )

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as FyneCastApplication)
                AlertsViewModel(
                    repository = application.container.appRepository
                )
            }
        }
    }
}
