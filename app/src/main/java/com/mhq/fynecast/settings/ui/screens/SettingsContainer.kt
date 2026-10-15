package com.mhq.fynecast.settings.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.R
import com.mhq.fynecast.settings.ui.SettingsViewModel

@Composable
fun SettingsContainer(
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory),
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }

    val profileName by settingsViewModel.profileName.collectAsStateWithLifecycle()
    val profileEmail by settingsViewModel.profileEmail.collectAsStateWithLifecycle()
    val profileImageUri by settingsViewModel.profileImageUri.collectAsStateWithLifecycle()
    val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()

    val isMetric by settingsViewModel.isMetric.collectAsStateWithLifecycle()
    val isDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        settingsViewModel.logoutComplete.collect {
            onLogoutClick()
        }
    }

    LaunchedEffect(settingsViewModel, resources) {
        settingsViewModel.logoutFailed.collect {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(resources.getString(R.string.logout_failed))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SettingsContent(
            isMetric = isMetric,
            onMetricToggle = settingsViewModel::toggleUnitSystem,
            isDarkMode = isDarkMode,
            onDarkModeToggle = settingsViewModel::toggleDarkMode,
            notificationsEnabled = notificationsEnabled,
            onNotificationsToggle = settingsViewModel::toggleNotifications,
            profileName = profileName,
            profileEmail = profileEmail,
            profileImageUri = profileImageUri,
            currentLanguage = currentLanguage,
            onLanguageChange = settingsViewModel::toggleLanguage,
            onEditProfileClick = onEditProfileClick,
            onLogoutClick = { settingsViewModel.logOutUserSession() },
            contentPadding = contentPadding,
            modifier = modifier
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding())
        )
    }
}