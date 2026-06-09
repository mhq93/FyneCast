package com.mhq.fynecast.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.ui.theme.FyneCastTheme
import kotlin.math.roundToInt

@Composable
fun WeatherDailyItem(
    day: String,
    date: String,
    weatherIconUrl: String,
    weatherDescription: String,
    highTemperature: Double,
    lowTemperature: Double,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    // FIX: Removed individual GlassyCard wrapper. Left as a clean, uniform layout column.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .width(72.dp) // Locks identical widths so items stack symmetrically in the loop
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = day,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = date,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), // Soft opacity sets secondary text hierarchy
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )

        AsyncImage(
            model = "https:$weatherIconUrl",
            contentDescription = weatherDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(42.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row {
            Text(
                text = "${highTemperature.roundToInt()}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFF8A80)
            )
            Text(
                text = if (isMetric) stringResource(R.string.c) else stringResource(R.string.f),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFF8A80)
            )
        }
        Row {
            Text(
                text = "${lowTemperature.roundToInt()}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF80D8FF)
            )
            Text(
                text = if (isMetric) stringResource(R.string.c) else stringResource(R.string.f),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF80D8FF)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeatherDailyItemPreview() {
    FyneCastTheme() {
        WeatherDailyItem(
            day = "",
            date = "",
            weatherIconUrl = "",
            weatherDescription = "BlaBla",
            highTemperature = 20.0,
            lowTemperature = 10.0,
            isMetric = true
        )
    }
}