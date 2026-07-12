package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.core.ui.theme.LightBlue
import com.mhq.fynecast.core.ui.theme.SoftCoral
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .width(72.dp)
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
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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
                text = "${highTemperature.roundToInt()}°",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = SoftCoral
            )
        }
        Row {
            Text(
                text = "${lowTemperature.roundToInt()}°",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = LightBlue
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

@Preview(showBackground = true)
@Composable
fun WeatherDailyItemPreview() {
    FyneCastTheme() {
        WeatherDailyItem(
            day = "Hi",
            date = "Hi",
            weatherIconUrl = "Hi",
            weatherDescription = "Hi",
            highTemperature = 20.0,
            lowTemperature = 10.0,
            isMetric = true
        )
    }
}