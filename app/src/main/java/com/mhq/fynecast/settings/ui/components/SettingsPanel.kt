package com.mhq.fynecast.settings.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.settings.presentation.model.LanguageOption

@Composable
fun SettingsPanel(
    notificationsEnabled: Boolean,
    isDarkMode: Boolean,
    isMetric: Boolean,
    currentLanguage: LanguageOption,
    onNotificationsToggle: (Boolean) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onMetricToggle: (Boolean) -> Unit,
    onLanguageChange: (LanguageOption) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassyCard(
        cornerRadius = 24.dp,
        padding = 0.dp,
        containerAlpha = 0.06f,
        borderAlpha = 0.15f,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsToggleRow(
                icon = Icons.Outlined.Straighten,
                label = stringResource(R.string.units),
                isFirstOptionSelected = isMetric,
                firstOptionLabel = stringResource(R.string.metric),
                secondOptionLabel = stringResource(R.string.imperial),
                onFirstOptionSelected = { onMetricToggle(true) },
                onSecondOptionSelected = { onMetricToggle(false) }
            )
            SettingsPanelDivider()
            SettingsToggleRow(
                icon = Icons.Outlined.Notifications,
                label = stringResource(R.string.notifications),
                isFirstOptionSelected = notificationsEnabled,
                firstOptionLabel = stringResource(R.string.on),
                secondOptionLabel = stringResource(R.string.off),
                onFirstOptionSelected = { onNotificationsToggle(true) },
                onSecondOptionSelected = { onNotificationsToggle(false) }
            )
            SettingsPanelDivider()
            SettingsToggleRow(
                icon = Icons.Outlined.Language,
                label = stringResource(R.string.language),
                isFirstOptionSelected = currentLanguage == LanguageOption.English,
                firstOptionLabel = stringResource(R.string.english),
                secondOptionLabel = "العربية",
                onFirstOptionSelected = { onLanguageChange(LanguageOption.English) },
                onSecondOptionSelected = { onLanguageChange(LanguageOption.Arabic) }
            )
            SettingsPanelDivider()
            SettingsToggleRow(
                icon = Icons.Outlined.DarkMode,
                label = stringResource(R.string.dark_mode),
                isFirstOptionSelected = !isDarkMode,
                firstOptionLabel = stringResource(R.string.light),
                secondOptionLabel = stringResource(R.string.dark),
                onFirstOptionSelected = { onDarkModeToggle(false) },
                onSecondOptionSelected = { onDarkModeToggle(true) }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun SettingsPanelPreview() {
    FyneCastTheme(isDarkMode = true) {
        SettingsPanel(
            notificationsEnabled = true,
            isDarkMode = true,
            isMetric = true,
            currentLanguage = LanguageOption.English,
            onNotificationsToggle = {},
            onDarkModeToggle = {},
            onMetricToggle = {},
            onLanguageChange = {}
        )
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
    backgroundColor = 0xFFE0F2FE
)
@Composable
private fun SettingsPanelLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        SettingsPanel(
            notificationsEnabled = false,
            isDarkMode = false,
            isMetric = false,
            currentLanguage = LanguageOption.Arabic,
            onNotificationsToggle = {},
            onDarkModeToggle = {},
            onMetricToggle = {},
            onLanguageChange = {}
        )
    }
}
