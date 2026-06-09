package com.mhq.fynecast.ui.screens.settings.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsContainer(
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory),
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val isMetric by viewModel.isMetric.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    val profileName by viewModel.profileName.collectAsState()
    val profileEmail by viewModel.profileEmail.collectAsState()
    val profileImageUri by viewModel.profileImageUri.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState(initial = "English")

    SettingsContent(
        isMetric = isMetric,
        onMetricToggle = viewModel::toggleUnitSystem,
        isDarkMode = isDarkMode,
        onDarkModeToggle = viewModel::toggleDarkMode,
        notificationsEnabled = notificationsEnabled,
        onNotificationsToggle = viewModel::toggleNotifications,
        profileName = profileName,
        profileEmail = profileEmail,
        profileImageUri = profileImageUri,
        currentLanguage = currentLanguage,
        onLanguageChange = viewModel::toggleLanguage,
        onEditProfileClick = onEditProfileClick,
        onLogoutClick = onLogoutClick,
        contentPadding = contentPadding,
        modifier = modifier
    )
}

