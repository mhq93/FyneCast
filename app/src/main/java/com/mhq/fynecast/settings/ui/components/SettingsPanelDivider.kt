package com.mhq.fynecast.settings.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun SettingsPanelDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.onBackground.copy(
            alpha = 0.08f
        ),
        modifier = Modifier.padding(
            horizontal = 16.dp
        )
    )
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
private fun SettingsPanelDividerPreview() {
    FyneCastTheme() {
        SettingsPanelDivider()
    }
}