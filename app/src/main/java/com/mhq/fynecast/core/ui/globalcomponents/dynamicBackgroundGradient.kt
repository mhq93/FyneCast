package com.mhq.fynecast.core.ui.globalcomponents

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun dynamicBackgroundGradient(): List<Color> {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return if (isLight) listOf(
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.background
    ) else listOf(
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant
    )
}