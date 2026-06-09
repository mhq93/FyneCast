package com.mhq.fynecast.ui.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.ui.components.GlassyCard
import com.mhq.fynecast.ui.theme.FyneCastTheme

@Composable
fun SettingsRow(
    icon: ImageVector,
    label: String,
    action: @Composable () -> Unit
) {
    GlassyCard(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp) // 1. Tighter external margin stacking
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .wrapContentHeight() // 2. Forces row height to conform tightly to content size
                .padding(vertical = 4.dp), // 3. Minimal vertical space for a snug fit
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            action()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsRowPreview() {
    FyneCastTheme() {
        SettingsRow(
            icon = Icons.Default.Home,
            label = "Some Label",
            action = {}
        )
    }
}
