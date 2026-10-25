package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.globalcomponents.unlessArabic
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

/**
 * The main weather card: city and time, big temperature with the condition icon,
 * and a one-line High / Low / Humidity / Wind strip. Every value arrives already formatted.
 */
@Composable
fun WeatherHeroCard(
    icon: String,
    city: String,
    timestamp: String,
    description: String,
    temperature: String,
    high: String,
    low: String,
    humidity: String,
    wind: String,
    isFavorite: Boolean,
    alertsNumber: Int,
    onToggleFavorite: () -> Unit,
    onAlertsBannerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val glowColor = MaterialTheme.colorScheme.secondary
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    GlassyCard(
        cornerRadius = 28.dp,
        padding = 0.dp,
        containerAlpha = 0.06f,
        borderAlpha = 0.15f,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // A soft warm glow behind the icon corner (the card clips it to its shape).
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(
                                x = if (isRtl) size.width * 0.15f else size.width * 0.85f,
                                y = size.height * 0.25f
                            ),
                            radius = size.width * 0.6f
                        )
                    )
                }
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = city,
                        color = onBackground,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp.unlessArabic()
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = timestamp.uppercase(),
                        color = onBackground.copy(alpha = 0.45f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp.unlessArabic()
                        )
                    )
                }

                LikeButton(
                    isLiked = isFavorite,
                    onLikeChanged = onToggleFavorite
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = temperature,
                        color = onBackground,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.W200,
                        lineHeight = 64.sp,
                        letterSpacing = (-2).sp.unlessArabic()
                    )
                    Text(
                        text = description.uppercase(),
                        color = onBackground.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp.unlessArabic()
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AsyncImage(
                    model = "https:$icon",
                    contentDescription = description,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(84.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = onBackground.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroStat(
                    value = high,
                    label = stringResource(R.string.stat_high),
                    modifier = Modifier.weight(1f)
                )
                HeroStatDivider()
                HeroStat(
                    value = low,
                    label = stringResource(R.string.stat_low),
                    modifier = Modifier.weight(1f)
                )
                HeroStatDivider()
                HeroStat(
                    value = humidity,
                    label = stringResource(R.string.humidity),
                    modifier = Modifier.weight(1f)
                )
                HeroStatDivider()
                HeroStat(
                    value = wind,
                    label = stringResource(R.string.wind),
                    modifier = Modifier.weight(1f)
                )
            }

            if (alertsNumber > 0) {
                Spacer(modifier = Modifier.height(10.dp))

                val alertShape = RoundedCornerShape(14.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(alertShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            shape = alertShape
                        )
                        .clickable { onAlertsBannerClick() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = stringResource(R.string.warning),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.active_weather_warnings,
                            alertsNumber,
                            alertsNumber
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp.unlessArabic()
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label.uppercase(),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp.unlessArabic()
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HeroStatDivider() {
    VerticalDivider(
        modifier = Modifier.height(26.dp),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
    )
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE)
@Composable
private fun WeatherHeroCardLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        WeatherHeroCard(
            icon = "",
            city = "Cairo",
            timestamp = "3:42 PM",
            description = "Mostly sunny",
            temperature = "24°",
            high = "28°",
            low = "17°",
            humidity = "42%",
            wind = "12 km/h",
            isFavorite = true,
            alertsNumber = 2,
            onToggleFavorite = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
private fun WeatherHeroCardDarkPreview() {
    FyneCastTheme(isDarkMode = true) {
        WeatherHeroCard(
            icon = "",
            city = "Cairo",
            timestamp = "3:42 PM",
            description = "Mostly sunny",
            temperature = "24°",
            high = "28°",
            low = "17°",
            humidity = "42%",
            wind = "12 km/h",
            isFavorite = false,
            alertsNumber = 0,
            onToggleFavorite = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
