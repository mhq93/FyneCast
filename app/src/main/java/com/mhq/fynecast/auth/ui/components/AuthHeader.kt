package com.mhq.fynecast.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.unlessArabic

/** The brand mark, the app name in spaced capitals, then the screen title and a short line. */
@Composable
fun AuthHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val glow = MaterialTheme.colorScheme.secondary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(glow.copy(alpha = 0.32f), glow.copy(alpha = 0.06f))
                    )
                )
                .border(1.dp, onBackground.copy(alpha = 0.12f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.WbSunny,
                contentDescription = null,
                tint = glow,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.app_name).uppercase(),
            color = onBackground.copy(alpha = 0.85f),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp.unlessArabic()
            ),
            modifier = Modifier.clearAndSetSemantics {}
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            color = onBackground,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp.unlessArabic()
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            color = onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
