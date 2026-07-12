package com.mhq.fynecast.alerts.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.alerts.ui.AlertsUiState
import com.mhq.fynecast.alerts.ui.AlertsViewModel

@Composable
fun AlertsContainer(
    alertsViewModel: AlertsViewModel = viewModel(factory = AlertsViewModel.factory),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {

    val uiState by alertsViewModel.uiState.collectAsStateWithLifecycle()
    val trackedAlertIds by alertsViewModel.trackedAlertIds.collectAsStateWithLifecycle()
    val currentCityName by alertsViewModel.currentCityName.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is AlertsUiState.Loading -> {
            AlertsBlank(
                contentPadding = contentPadding,
                modifier = modifier
            )
        }
        is AlertsUiState.Success -> {
            AlertsContent(
                alerts = state.alerts,
                trackedAlertIds = trackedAlertIds,
                onTrackToggle = { clickedAlert ->
                    alertsViewModel.onAlertTrackToggled(
                        alert = clickedAlert,
                        cityName = currentCityName
                    )
                },
                contentPadding = contentPadding,
                modifier = modifier
            )
        }
    }
}