package com.mhq.fynecast.alerts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.usecases.GetAlertsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertIdsUseCase
import com.mhq.fynecast.alerts.domain.usecases.ToggleAlertsUseCase
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentCityNameUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlertsViewModel(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val toggleAlertsUseCase: ToggleAlertsUseCase,
    private val getTrackedAlertIdsUseCase: GetTrackedAlertIdsUseCase,
    private val observeCurrentCityNameUseCase: ObserveCurrentCityNameUseCase
) : ViewModel() {

    val uiState: StateFlow<AlertsUiState> = getAlertsUseCase()
        .map { alerts -> if (alerts.isEmpty()) AlertsUiState.Loading else AlertsUiState.Success(alerts) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AlertsUiState.Loading)

    val trackedAlertIds: StateFlow<List<String>> = getTrackedAlertIdsUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentCityName: StateFlow<String> = observeCurrentCityNameUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Unknown Location")

    fun onAlertTrackToggled(alert: AlertDomainModel, cityName: String) {
        viewModelScope.launch { toggleAlertsUseCase(alert, cityName) }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)

                AlertsViewModel(
                    getAlertsUseCase = app.container.getAlertsUseCase,
                    toggleAlertsUseCase = app.container.toggleAlertsUseCase,
                    getTrackedAlertIdsUseCase = app.container.getTrackedAlertIdsUseCase,
                    observeCurrentCityNameUseCase = app.container.observeCurrentCityNameUseCase
                )
            }
        }
    }
}