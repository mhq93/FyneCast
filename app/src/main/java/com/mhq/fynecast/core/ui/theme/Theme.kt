package com.mhq.fynecast.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FyneCastDarkScheme = darkColorScheme(
    // Primary action — buttons, active segmented buttons, focused borders
    primary = AbyssalVoid,
    onPrimary = ElectricLimeGreen,
    // Secondary — weather metric accents (temperature highs, UV)
    secondary = Honey,
    onSecondary = AbyssalVoid,
    // Tertiary — storm/alert accents, special callouts
    tertiary = VividViolet,
    onTertiary = Color.White,
    // Background — the deepest layer, full screen canvas
    background = AbyssalVoid,
    onBackground = Color.White,
    // Surface — cards, bottom sheets, elevated containers
    surface = BiscayBlue,
    onSurface = ElectricLimeGreen,
    // SurfaceVariant — chips, hourly/daily weather rows, input fields
    surfaceVariant = SlateGray,
    onSurfaceVariant = ElectricLimeGreen.copy(alpha = 0.85f),
    // Error — weather warnings, validation errors
    error = VividRed,
    onError = Color.White,
    // Outline — dividers, inactive borders, field outlines
    outline = Color.White.copy(alpha = 0.15f),
    outlineVariant = Color.White.copy(alpha = 0.08f)
)

private val FyneCastLightScheme = lightColorScheme(
    // Primary action — buttons, active states, focused borders
    primary = SkyBlue,
    onPrimary = AbyssalVoid,
    // Secondary — warm weather accents
    secondary = Honey,
    onSecondary = Color.White,
    // Tertiary — cool blue accents for humidity, precipitation
    tertiary = SkyBlue,
    onTertiary = AbyssalVoid,
    // Background — soft ice blue canvas
    background = BabyBlue,
    onBackground = AbyssalVoid,
    // Surface — pure white cards, input fields, bottom sheets
    surface = PalerSkyBlue,
    onSurface = AbyssalVoid,
    // SurfaceVariant — weather grid tiles, hourly rows
    surfaceVariant = PalerSkyBlue,
    onSurfaceVariant = AbyssalVoid,
    // Error
    error = VividRed,
    onError = Color.White,
    // Outline — field borders, dividers
    outline = AbyssalVoid,
    outlineVariant = PalerSkyBlue
)

@Composable
fun FyneCastTheme(
    isDarkMode: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (isDarkMode)
            FyneCastDarkScheme
        else
            FyneCastLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(),
        content = content
    )
}