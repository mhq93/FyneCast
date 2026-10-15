package com.mhq.fynecast.alerts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import com.mhq.fynecast.alerts.domain.usecases.GetAlertsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertIdsUseCase
import com.mhq.fynecast.alerts.domain.usecases.ToggleAlertsUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentLocationUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlertsViewModel(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val getTrackedAlertIdsUseCase: GetTrackedAlertIdsUseCase,
    private val observeCurrentLocationUseCase: ObserveCurrentLocationUseCase,
    private val toggleAlertsUseCase: ToggleAlertsUseCase,
    private val toggleNotificationsUseCase: ToggleNotificationsUseCase,
    private val handleNotificationPermissionResultUseCase: HandleNotificationPermissionResultUseCase
) : ViewModel() {

    val uiState: StateFlow<AlertsUiState> = getAlertsUseCase()
        .map { alerts ->
            if (alerts == null) AlertsUiState.Loading else AlertsUiState.Success(alerts)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            AlertsUiState.Loading
        )

    val trackedAlertIds: StateFlow<List<String>> = getTrackedAlertIdsUseCase()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    val currentLocation: StateFlow<TrackedLocation?> =
        observeCurrentLocationUseCase()
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                null
            )

    private var pendingAlert: AlertDomainModel? = null

    private val _showNotificationsPrompt = MutableStateFlow(false)
    val showNotificationsPrompt: StateFlow<Boolean> = _showNotificationsPrompt.asStateFlow()

    private val _events = Channel<AlertsEvent>(Channel.BUFFERED)
    val events: Flow<AlertsEvent> = _events.receiveAsFlow()

    fun onAlertTrackToggled(alert: AlertDomainModel) {
        viewModelScope.launch { toggle(alert) }
    }

    fun onNotificationsPromptConfirmed() {
        _showNotificationsPrompt.value = false
        viewModelScope.launch {
            when (toggleNotificationsUseCase(wantsEnabled = true)) {
                ToggleNotificationsUseCase.Result.Enabled -> trackPendingAlert()
                ToggleNotificationsUseCase.Result.PermissionRequired ->
                    _events.send(AlertsEvent.RequestNotificationPermission)

                ToggleNotificationsUseCase.Result.Disabled -> Unit
            }
        }
    }

    fun onNotificationsPromptDismissed() {
        _showNotificationsPrompt.value = false
        pendingAlert = null
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            handleNotificationPermissionResultUseCase(granted)
            if (granted) {
                trackPendingAlert()
            } else {
                pendingAlert = null
                _events.send(AlertsEvent.NotificationPermissionDenied)
            }
        }
    }

    private suspend fun trackPendingAlert() {
        val alert = pendingAlert ?: return
        pendingAlert = null
        toggle(alert)
    }

    private suspend fun toggle(alert: AlertDomainModel) {
        val location = currentLocation.value ?: return
        when (toggleAlertsUseCase(alert, location)) {
            ToggleAlertsUseCase.Result.Tracked -> _events.send(
                AlertsEvent.AlertTrackingStarted(
                    alert.event
                )
            )

            ToggleAlertsUseCase.Result.AlreadyExpired -> _events.send(AlertsEvent.AlertAlreadyExpired)
            ToggleAlertsUseCase.Result.NotificationsRequired -> {
                pendingAlert = alert
                _showNotificationsPrompt.value = true
            }

            ToggleAlertsUseCase.Result.Untracked -> Unit
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)
                AlertsViewModel(
                    getAlertsUseCase = app.container.getAlertsUseCase,
                    getTrackedAlertIdsUseCase = app.container.getTrackedAlertIdsUseCase,
                    observeCurrentLocationUseCase = app.container.observeCurrentLocationUseCase,
                    toggleAlertsUseCase = app.container.toggleAlertsUseCase,
                    toggleNotificationsUseCase = app.container.toggleNotificationsUseCase,
                    handleNotificationPermissionResultUseCase = app.container.handleNotificationPermissionResultUseCase,
                )
            }
        }
    }
}