package com.mhq.fynecast.settings.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun SettingsPanel(
    notificationsEnabled: Boolean,
    isDarkMode: Boolean,
    isMetric: Boolean,
    currentLanguage: String,
    onNotificationsToggle: (Boolean) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onMetricToggle: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassyCard(
        padding = 0.dp,
        modifier = modifier.padding(
                start = 16.dp,
                top = 32.dp,
                bottom = 32.dp,
                end = 16.dp
        ).fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsToggleRow(
                label = stringResource(R.string.units),
                isFirstOptionSelected = isMetric,
                firstOptionLabel = stringResource(R.string.metric),
                secondOptionLabel = stringResource(R.string.imperial),
                onFirstOptionSelected = { onMetricToggle(true) },
                onSecondOptionSelected = { onMetricToggle(false) }
            )
            SettingsPanelDivider()
            SettingsToggleRow(
                label = stringResource(R.string.notifications),
                isFirstOptionSelected = notificationsEnabled,
                firstOptionLabel = stringResource(R.string.on),
                secondOptionLabel = stringResource(R.string.off),
                onFirstOptionSelected = { onNotificationsToggle(true) },
                onSecondOptionSelected = { onNotificationsToggle(false) }
            )
            SettingsPanelDivider()
            SettingsToggleRow(
                label = stringResource(R.string.language),
                isFirstOptionSelected = currentLanguage == "English",
                firstOptionLabel = stringResource(R.string.english),
                secondOptionLabel = "العربية",
                onFirstOptionSelected = { onLanguageChange("English") },
                onSecondOptionSelected = { onLanguageChange("Arabic") }
            )
            SettingsPanelDivider()
            SettingsToggleRow(
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

@Preview
@Composable
private fun SettingsPanelPreview() {
    FyneCastTheme() {
        SettingsPanel(
            notificationsEnabled = true,
            isDarkMode = true,
            isMetric = true,
            currentLanguage = "",
            onNotificationsToggle = {},
            onDarkModeToggle = {},
            onMetricToggle = {},
            onLanguageChange = {}
        )
    }
}