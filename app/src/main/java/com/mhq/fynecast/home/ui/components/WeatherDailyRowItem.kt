package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.core.ui.format.TemperatureFormatter
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

/** One day, about 30 dp tall: day and date, icon with description, then low, range bar, high. */
@Composable
fun WeatherDailyRowItem(
    day: String,
    date: String,
    weekMin: Double,
    weekMax: Double,
    weatherIconUrl: String,
    weatherDescription: String,
    highTemperature: Double,
    lowTemperature: Double,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.width(64.dp)) {
            Text(
                text = day,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Text(
                text = date,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        ) {
            AsyncImage(
                model = "https:$weatherIconUrl",
                contentDescription = weatherDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = weatherDescription,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                ),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.width(124.dp)
        ) {
            Text(
                text = TemperatureFormatter.format(lowTemperature),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.End,
                modifier = Modifier.width(32.dp)
            )

            val range = (weekMax - weekMin).coerceAtLeast(1.0)
            val start = ((lowTemperature - weekMin) / range).toFloat().coerceIn(0f, 1f)
            val end = ((highTemperature - weekMin) / range).toFloat().coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            ) {
                Row(Modifier.fillMaxSize()) {
                    if (start > 0f) Spacer(Modifier.weight(start))
                    Box(
                        Modifier
                            .weight((end - start).coerceAtLeast(0.08f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    if (end < 1f) Spacer(Modifier.weight(1f - end))
                }
            }

            Text(
                text = TemperatureFormatter.format(highTemperature),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.End,
                modifier = Modifier.width(32.dp)
            )
        }
    }
}

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
private fun WeatherDailyRowItemPreview() {
    FyneCastTheme(isDarkMode = true) {
        WeatherDailyRowItem(
            day = "Mon",
            date = "Oct 26",
            weekMin = 14.0,
            weekMax = 30.0,
            weatherIconUrl = "",
            weatherDescription = "Sunny",
            highTemperature = 28.0,
            lowTemperature = 17.0,
            modifier = Modifier.padding(16.dp)
        )
    }
}