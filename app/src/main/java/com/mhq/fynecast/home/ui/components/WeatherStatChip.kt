package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.globalcomponents.unlessArabic
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun WeatherStatChip(
    chipIcon: ImageVector,
    chipHeader: String,
    chipBody: String,
    chipSubtitle: String?,
    modifier: Modifier = Modifier
) {
    GlassyCard(
        cornerRadius = 24.dp,
        padding = 16.dp,
        containerAlpha = 0.08f,
        borderAlpha = 0.15f,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Upper Metadata Layer: Inline icon & subtle descriptive title text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), //surfaceVariantColor
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f) //surfaceVariantColor
                                )
                            )
                        )
                ) {
                    Icon(
                        imageVector = chipIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), //onSurfaceColor
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = chipHeader.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp.unlessArabic()
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), //onSurfaceColor
                    maxLines = 1
                )
            }

            // Lower Metric Block: Combined inline string for a flawless shared baseline
            Text(
                text = buildAnnotatedString {
                    // 1. The main numeric value
                    withStyle(
                        style = SpanStyle(
                            fontSize = 28.sp, //32
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground //onSurfaceColor
                        )
                    ) {
                        append(chipBody)
                    }

                    // 2. The unit suffix
                    if (!chipSubtitle.isNullOrBlank()) {
                        withStyle(
                            style = SpanStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f) //onSurfaceColor
                            )
                        ) {
                            append(" ${chipSubtitle.lowercase()}")
                        }
                    }
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.fillMaxWidth()
            )
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
private fun WeatherStatChipPreview() {
    FyneCastTheme() {
        WeatherStatChip(
            chipIcon = Icons.Default.WbSunny,
            chipHeader = "Chip Header",
            chipBody = "Chip Body",
            chipSubtitle = "Chip Subtitle"
        )
    }
}