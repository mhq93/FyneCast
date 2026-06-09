package com.mhq.fynecast.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FyneCastDarkScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = MidnightBlue,
    background = MidnightBlue,
    onBackground = Color.White,
    surface = DuskBlue,
    onSurface = Color.White,
    outline = Color.White.copy(alpha = 0.2f)
)

private val FyneCastLightScheme = lightColorScheme(
    primary = ForestGreen,              // High-contrast deep green for accent text/highlights
    onPrimary = Color.White,            // Crisp white text when sitting inside solid colored blocks
    background = LightIceBlue,          // Base soft ice-blue canvas color
    onBackground = MidnightBlue,        // Dark blue text for maximum legibility on bright backdrops
    surface = Color.White,              // Pure white surfaces for drop-down lists or text boxes
    onSurface = MidnightBlue,           // Dark blue text for content inside input fields
    outline = LightSlateSurface         // Frosted light gray for thin lines and split dividers
)

@Composable
fun FyneCastTheme(
    isDarkMode: Boolean = isSystemInDarkTheme(), // Defaults to system tracking settings
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkMode) FyneCastDarkScheme else FyneCastLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(),
        content = content
    )
}