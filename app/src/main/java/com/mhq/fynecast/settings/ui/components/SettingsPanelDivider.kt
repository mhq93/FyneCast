package com.mhq.fynecast.settings.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A hairline that starts after the row icon, so it lines up with the labels. */
@Composable
fun SettingsPanelDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
        modifier = Modifier.padding(start = 60.dp, end = 14.dp)
    )
}
