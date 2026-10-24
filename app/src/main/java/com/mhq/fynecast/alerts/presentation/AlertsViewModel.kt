package com.mhq.fynecast.alerts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.alerts.domain.usecases.GetAlertsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertIdsUseCase
import com.mhq.fynecast.alerts.domain.usecases.ToggleAlertsUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.alerts.presentation.mapper.toPresentation
import com.mhq.fynecast.core.domain.location.TrackedLocation
import com.mhq.fynecast.core.domain.weather.AlertDomainModel
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentLocationUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel managing state, user actions, permission flows, and event channels for the alerts feature.
 */
class AlertsViewModel(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val toggleAlertsUseCase: ToggleAlertsUseCase,
    private val getTrackedAlertIdsUseCase: GetTrackedAlertIdsUseCase,
    private val observeCurrentLocationUseCase: ObserveCurrentLocationUseCase,
    private val toggleNotificationsUseCase: ToggleNotificationsUseCase,
    private val handleNotificationPermissionResultUseCase: HandleNotificationPermissionResultUseCase
) : ViewModel() {

    /** The domain alerts, kept privately: the screen only ever sees presentation models. */
    private val domainAlerts: StateFlow<List<AlertDomainModel>?> = getAlertsUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Exposes reactive UI state for alerts, transitioning between loading and success states. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AlertsUiState> = domainAlerts
        .transformLatest { alerts ->
            if (alerts != null) {
                emit(AlertsUiState.Success(alerts.map { it.toPresentation() }))
            } else {
                emit(AlertsUiState.Loading)
                delay(LOAD_TIMEOUT_MILLIS.milliseconds)
                emit(AlertsUiState.Unavailable)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            AlertsUiState.Loading
        )

    /** Stream of IDs representing currently tracked alerts. */
    val trackedAlertIds: StateFlow<List<String>> = getTrackedAlertIdsUseCase()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    /** Stream tracking the user's current location context. */
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

    /**
     * Triggered when the user clicks the tracking toggle on an alert card.
     */
    fun onAlertTrackToggled(alertId: String) {
        val alert = domainAlerts.value?.firstOrNull { it.id == alertId } ?: return
        viewModelScope.launch { toggle(alert) }
    }

    /**
     * Called when the user confirms the notification prompt dialog.
     *
     * Attempts to enable notifications; if system permission is required, emits an event to request it.
     */
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

    /**
     * Dismisses the notification prompt dialog and clears any pending alert reference.
     */
    fun onNotificationsPromptDismissed() {
        _showNotificationsPrompt.value = false
        pendingAlert = null
    }

    /**
     * Processes the runtime permission result for notifications.
     *
     * If granted, resumes tracking the pending alert; if denied, emits a denial event.
     */
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

    /** Resumes tracking for an alert that was held pending notification permission. */
    private suspend fun trackPendingAlert() {
        val alert = pendingAlert ?: return
        pendingAlert = null
        toggle(alert)
    }

    /** Executes toggle logic and handles resulting states (e.g. triggering permission dialogs or emitting snackbar events). */
    private suspend fun toggle(alert: AlertDomainModel) {
        val location = currentLocation.value ?: return
        when (toggleAlertsUseCase(alert, location)) {
            ToggleAlertsUseCase.Result.Tracked -> _events.send(AlertsEvent.AlertTrackingStarted(alert.event))
            ToggleAlertsUseCase.Result.AlreadyExpired -> _events.send(AlertsEvent.AlertAlreadyExpired)
            ToggleAlertsUseCase.Result.ExpiryUnknown -> _events.send(AlertsEvent.AlertExpiryUnknown)
            ToggleAlertsUseCase.Result.NotificationsRequired -> {
                pendingAlert = alert
                _showNotificationsPrompt.value = true
            }

            ToggleAlertsUseCase.Result.Untracked -> Unit
        }
    }

    companion object {
        private const val LOAD_TIMEOUT_MILLIS = 20_000L
    }
}