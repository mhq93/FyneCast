package com.mhq.fynecast.alerts.ui

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel

/**
 * Represents the UI state of the alerts screen.
 */
sealed interface AlertsUiState {
    object Loading : AlertsUiState
    object Unavailable : AlertsUiState
    data class Success(val alerts: List<AlertDomainModel>) : AlertsUiState
}