package com.mhq.fynecast.settings.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.settings.ui.components.SettingsLogoutButton
import com.mhq.fynecast.settings.ui.components.SettingsPanel
import com.mhq.fynecast.settings.ui.components.SettingsProfileHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    notificationsEnabled: Boolean,
    isDarkMode: Boolean,
    isMetric: Boolean,
    profileName: String,
    profileEmail: String,
    profileImageUri: String?,
    currentLanguage: String,
    onNotificationsToggle: (Boolean) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onMetricToggle: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsProfileHeader(
                profileName = profileName,
                profileEmail = profileEmail,
                profileImageUri = profileImageUri,
                onEditProfileClick = onEditProfileClick,
            )
            SettingsPanel(
                isMetric = isMetric,
                onMetricToggle = onMetricToggle,
                isDarkMode = isDarkMode,
                onDarkModeToggle = onDarkModeToggle,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                notificationsEnabled = notificationsEnabled,
                onNotificationsToggle = onNotificationsToggle
            )
            SettingsLogoutButton(
                onLogoutClick = onLogoutClick
            )
        }
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    FyneCastTheme() {
        SettingsContent(
            notificationsEnabled = true,
            isDarkMode = true,
            isMetric = true,
            profileName = "name",
            profileEmail = "email",
            onNotificationsToggle = {},
            onDarkModeToggle = {},
            onMetricToggle = {},
            onEditProfileClick = {},
            onLogoutClick = {},
            contentPadding = PaddingValues(0.dp),
            currentLanguage = "",
            onLanguageChange = {},
            profileImageUri = null
        )
    }
}