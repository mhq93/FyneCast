package com.mhq.fynecast.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import kotlin.math.roundToInt

@Composable
fun WeatherHourlyItem(
    header: String,
    weatherIconUrl: String,
    temperature: Double,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .width(64.dp)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = header,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        AsyncImage(
            model = "https:$weatherIconUrl",
            contentDescription = stringResource(R.string.hourly_condition),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(42.dp)
        )

        Text(
            text = if (isMetric) "${temperature.roundToInt()}°" else "${temperature.roundToInt()}°",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WeatherHourlyItemPreview() {
    FyneCastTheme() {
        WeatherHourlyItem(
            header = "12:00",
            weatherIconUrl = "",
            temperature = 10.0,
            isMetric = true
        )
    }
}