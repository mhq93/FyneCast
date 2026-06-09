package com.mhq.fynecast.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.ui.components.GlassyCard
import com.mhq.fynecast.ui.theme.FyneCastTheme
import kotlin.math.roundToInt

@Composable
fun WeatherHeroCard(
    icon: String,
    city: String,
    timestamp: String,
    description: String,
    temperature: String,
    isFavorite: Boolean,
    alertsNumber: Int,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    isMetric: Boolean
) {
    GlassyCard(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // 1. Top Row: City Name and Heart Icon perfectly balanced at the exact same edge boundaries
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically // FIX: Restores smooth vertical level balance
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = city,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = timestamp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // FIX: Horizontal offset removed entirely. Sits edge-aligned with the city name text lines.
                LikeButton(
                    isLiked = isFavorite,
                    onLikeChanged = onToggleFavorite,
                    modifier = Modifier.offset(y = (-2).dp) // Micro vertical offset matches text baseline height
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Central Row: Left-anchored icon sitting tightly next to stacked weather text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 80.dp)
                        .clipToBounds(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AsyncImage(
                        model = "https:$icon",
                        contentDescription = description,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .size(110.dp)
                            .offset(x = (-22).dp)
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 0.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = temperature,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 56.sp
                    )
                    Text(
                        text = description.uppercase(),
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // 3. Bottom Layer: Conditional Government Warning Capsule Banner
            if (alertsNumber > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFD32F2F).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFD32F2F).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$alertsNumber ACTIVE WEATHER WARNINGS",
                        color = Color(0xFFEF5350),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeatherHeroCardPreview() {
    FyneCastTheme() {
        WeatherHeroCard(
            icon = "icon",
            city = "city",
            timestamp = "timestamp",
            description = "description",
            temperature = "30.0",
            isFavorite = false,
            isMetric = true,
            alertsNumber = 0,
            onToggleFavorite = {},
        )
    }
}