package com.mhq.fynecast.ui.screens.alerts.screens

import com.mhq.fynecast.data.network.weather.dto.AlertDto

sealed interface AlertsUiState {
    object Loading : AlertsUiState
    object Empty : AlertsUiState
    object NoAlerts : AlertsUiState
    data class Success(val alerts: List<AlertDto>) : AlertsUiState
}