package com.mhq.fynecast.alerts.presentation

import com.mhq.fynecast.alerts.presentation.model.AlertPresentationModel

/**
 * Represents the UI state of the alerts screen.
 */
sealed interface AlertsUiState {
    object Loading : AlertsUiState
    object Unavailable : AlertsUiState
    data class Success(val alerts: List<AlertPresentationModel>) : AlertsUiState
}