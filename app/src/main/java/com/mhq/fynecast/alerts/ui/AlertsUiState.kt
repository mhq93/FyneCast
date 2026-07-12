package com.mhq.fynecast.alerts.ui

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel

sealed interface AlertsUiState {
    object Loading : AlertsUiState
    data class Success(val alerts: List<AlertDomainModel>) : AlertsUiState
}