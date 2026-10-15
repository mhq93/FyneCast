package com.mhq.fynecast.alerts.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.R
import com.mhq.fynecast.alerts.ui.AlertsEvent
import com.mhq.fynecast.alerts.ui.AlertsUiState
import com.mhq.fynecast.alerts.ui.AlertsViewModel
import com.mhq.fynecast.alerts.ui.components.NotificationsDialog
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AlertsContainer(
    alertsViewModel: AlertsViewModel = viewModel(factory = AlertsViewModel.factory),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by alertsViewModel.uiState.collectAsStateWithLifecycle()
    val trackedAlertIds by alertsViewModel.trackedAlertIds.collectAsStateWithLifecycle()
    val showNotificationsPrompt by alertsViewModel.showNotificationsPrompt.collectAsStateWithLifecycle()
    var showOpenSettingsDialog by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> alertsViewModel.onNotificationPermissionResult(granted) }

    fun showMessage(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    LaunchedEffect(alertsViewModel) {
        alertsViewModel.events.collect { event ->
            when (event) {
                AlertsEvent.AlertAlreadyExpired ->
                    showMessage(resources.getString(R.string.alert_already_expired))

                is AlertsEvent.AlertTrackingStarted ->
                    showMessage(resources.getString(R.string.alert_tracking_started, event.eventName))


                AlertsEvent.RequestNotificationPermission ->
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

                AlertsEvent.NotificationPermissionDenied -> {
                    // Denied with no rationale available means "don't ask again": only settings can fix it.
                    val activity = context.findActivity()
                    val blockedForGood = activity != null &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(
                                activity, Manifest.permission.POST_NOTIFICATIONS
                            )
                    if (blockedForGood) {
                        showMessage(
                            message = resources.getString(R.string.notifications_settings_message),
                            actionLabel = resources.getString(R.string.notifications_settings_confirm)
                        ) { context.openNotificationSettings() }
                    } else {
                        showMessage(resources.getString(R.string.notifications_denied_message))
                    }
                }
            }
        }
    }

    if (showNotificationsPrompt) {
        NotificationsDialog(
            message = stringResource(R.string.notifications_dialog_message),
            confirmText = stringResource(R.string.notifications_dialog_confirm),
            onConfirm = alertsViewModel::onNotificationsPromptConfirmed,
            onDismiss = alertsViewModel::onNotificationsPromptDismissed
        )
    }

    if (showOpenSettingsDialog) {
        NotificationsDialog(
            message = stringResource(R.string.notifications_settings_message),
            confirmText = stringResource(R.string.notifications_settings_confirm),
            onConfirm = {
                showOpenSettingsDialog = false
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            },
            onDismiss = { showOpenSettingsDialog = false }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is AlertsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is AlertsUiState.Success -> {
                if (state.alerts.isEmpty()) {
                    AlertsBlank(contentPadding = contentPadding)
                } else {
                    AlertsContent(
                        alerts = state.alerts,
                        trackedAlertIds = trackedAlertIds,
                        onTrackToggle = { alertsViewModel.onAlertTrackToggled(it) },
                        contentPadding = contentPadding
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding())
        )
    }
}

private fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}