package com.mhq.fynecast.ui.screens.settings.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.MidnightBlue
import com.mhq.fynecast.ui.theme.NeonGreen

@Composable
fun ToggleSwitch(
    state: Boolean,
    onToggle: (Boolean) -> Unit,
    activeText: String? = null,
    inactiveText: String? = null,
    activeIcon: ImageVector? = null,
    inactiveIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val horizontalBias by animateFloatAsState(
        targetValue = if (state) 1f else -1f,
        label = "ThumbAnimation"
    )

    Box(
        modifier = modifier
            .width(122.dp)
            .height(48.dp)
            .clip(CircleShape)
            .background(if (state) MidnightBlue else MidnightBlue.copy(alpha = 0.5f))
            .clickable { onToggle(!state) }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (inactiveIcon != null) {
                    Icon(
                        imageVector = inactiveIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (!state) NeonGreen else NeonGreen.copy(alpha = 0.5f)
                    )
                } else if (inactiveText != null) {
                    Text(
                        text = inactiveText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!state) NeonGreen else NeonGreen.copy(alpha = 0.5f)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (activeIcon != null) {
                    Icon(
                        imageVector = activeIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (state) NeonGreen else NeonGreen.copy(alpha = 0.4f)
                    )
                } else if (activeText != null) {
                    Text(
                        text = activeText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state) NeonGreen else NeonGreen.copy(alpha = 0.4f)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .align(BiasAlignment(horizontalBias, 0f))
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun animateAlignmentAsState(targetAlignment: Alignment): State<Alignment> {
    val bias by animateFloatAsState(
        targetValue = if (targetAlignment == Alignment.CenterEnd) 1f else -1f,
        label = "alignmentAnimation"
    )
    return remember {
        derivedStateOf { BiasAlignment(horizontalBias = bias, verticalBias = 0f) }
    }
}

@Preview(showBackground = true)
@Composable
fun ToggleSwitchPreview() {
    FyneCastTheme() {
        ToggleSwitch(
            state = true,
            onToggle = {},
            activeText = "ON",
            inactiveText = "OFF"
        )
    }
}

