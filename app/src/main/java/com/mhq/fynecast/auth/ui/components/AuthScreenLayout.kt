package com.mhq.fynecast.auth.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The shared frame of the sign-in screens: a soft warm glow at the top and a cool one at the
 * bottom corner, with the content centred and scrollable (it scrolls on small screens
 * and when the keyboard is open). The glow stays still while the content scrolls.
 */
@Composable
fun AuthScreenLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val warmGlow = MaterialTheme.colorScheme.secondary
    val coolGlow = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(warmGlow.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width * 0.9f
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(coolGlow.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(0f, size.height),
                        radius = size.width * 0.9f
                    )
                )
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            content = content
        )
    }
}
