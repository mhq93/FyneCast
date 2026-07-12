package com.mhq.fynecast.settings.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun SettingsToggleRow(
    label: String,
    isFirstOptionSelected: Boolean,
    firstOptionLabel: String,
    secondOptionLabel: String,
    onFirstOptionSelected: () -> Unit,
    onSecondOptionSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 16.dp
            )
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Ltr
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .width(180.dp)
            ) {
                SegmentedButton(
                    selected = isFirstOptionSelected,
                    onClick = onFirstOptionSelected,
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 0,
                        count = 2
                    ),
                    colors = customSegmentedColors()
                ) {
                    Text(
                        text = firstOptionLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                SegmentedButton(
                    selected = !isFirstOptionSelected,
                    onClick = onSecondOptionSelected,
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 1,
                        count = 2
                    ),
                    colors = customSegmentedColors()
                ) {
                    Text(
                        text = secondOptionLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
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
private fun SettingsToggleRowPreview() {
    FyneCastTheme() {
        SettingsToggleRow(
            label = "",
            isFirstOptionSelected = true,
            firstOptionLabel = "",
            secondOptionLabel = "",
            onFirstOptionSelected = {},
            onSecondOptionSelected = {}
        )
    }
}