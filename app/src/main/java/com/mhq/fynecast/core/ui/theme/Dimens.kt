package com.mhq.fynecast.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppDimensions(
    val paddingSmall: Dp = 8.dp,
    val paddingMedium: Dp = 16.dp,
    val paddingLarge: Dp = 24.dp,
    val spacingElement: Dp = 12.dp,
    val cardCornerRadius: Dp = 16.dp,
    val iconSizeSmall: Dp = 16.dp
)

// Global reference object
val LocalDimensions = staticCompositionLocalOf { AppDimensions() }
