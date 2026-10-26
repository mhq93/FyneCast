package com.mhq.fynecast.settings.ui.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

/** One setting: a round icon, the label, and a two-choice pill selector at the end. */
@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    isFirstOptionSelected: Boolean,
    firstOptionLabel: String,
    secondOptionLabel: String,
    onFirstOptionSelected: () -> Unit,
    onSecondOptionSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onBackground = MaterialTheme.colorScheme.onBackground

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 18.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(onBackground.copy(alpha = 0.06f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = onBackground.copy(alpha = 0.75f),
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            color = onBackground,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        // The selector keeps the same left-to-right order in every language.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            SettingsSegmentedControl(
                firstLabel = firstOptionLabel,
                secondLabel = secondOptionLabel,
                isFirstSelected = isFirstOptionSelected,
                onFirstSelected = onFirstOptionSelected,
                onSecondSelected = onSecondOptionSelected
            )
        }
    }
}

@Composable
private fun SettingsSegmentedControl(
    firstLabel: String,
    secondLabel: String,
    isFirstSelected: Boolean,
    onFirstSelected: () -> Unit,
    onSecondSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val shape = RoundedCornerShape(12.dp)

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .width(148.dp)
            .height(36.dp)
            .clip(shape)
            .background(onBackground.copy(alpha = 0.06f))
            .border(1.dp, onBackground.copy(alpha = 0.12f), shape)
            .padding(3.dp)
            .selectableGroup()
    ) {
        Segment(
            text = firstLabel,
            selected = isFirstSelected,
            onClick = onFirstSelected,
            modifier = Modifier.weight(1f)
        )
        Segment(
            text = secondLabel,
            selected = !isFirstSelected,
            onClick = onSecondSelected,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun Segment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else Color.Transparent,
        label = "segmentContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
        },
        label = "segmentContent"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(9.dp))
            .background(container)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Text(
            text = text,
            color = content,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun SettingsToggleRowPreview() {
    FyneCastTheme(isDarkMode = true) {
        Column {
            SettingsToggleRow(
                icon = Icons.Outlined.Notifications,
                label = "Notifications",
                isFirstOptionSelected = true,
                firstOptionLabel = "On",
                secondOptionLabel = "Off",
                onFirstOptionSelected = {},
                onSecondOptionSelected = {}
            )
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
    backgroundColor = 0xFFE0F2FE
)
@Composable
private fun SettingsToggleRowLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        SettingsToggleRow(
            icon = Icons.Outlined.Notifications,
            label = "Notifications",
            isFirstOptionSelected = false,
            firstOptionLabel = "On",
            secondOptionLabel = "Off",
            onFirstOptionSelected = {},
            onSecondOptionSelected = {}
        )
    }
}
