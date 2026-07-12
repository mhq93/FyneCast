package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.home.domain.models.DailyForecastDomainModel

@Composable
fun WeatherDailyCarousel(
    dailyForecasts: List<DailyForecastDomainModel>,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    GlassyCard(
        modifier = modifier
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
            .fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
//                Icon(
//                    imageVector = Icons.Filled.CalendarToday,
//                    contentDescription = stringResource(R.string.daily_forecast),
//                    tint = MaterialTheme.colorScheme.onBackground,
//                    modifier = Modifier.padding(end = 4.dp)
//                )
                Text(
                    text = stringResource(R.string.daily_forecast),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    count = dailyForecasts.size,
                    key = { index -> dailyForecasts[index].dayName + index }
                ) { index ->
                    val item = dailyForecasts[index]
                    WeatherDailyItem(
                        day = item.dayName,
                        date = item.monthDayStr,
                        weatherIconUrl = item.iconUrl,
                        weatherDescription = item.description,
                        highTemperature = if (isMetric) item.maxTempC else item.maxTempF,
                        lowTemperature = if (isMetric) item.minTempC else item.minTempF,
                        isMetric = isMetric
                    )
                }
            }
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
private fun WeatherDailyCarouselPreview() {
    FyneCastTheme() {
        WeatherDailyCarousel(
            dailyForecasts = emptyList(),
            isMetric = true
        )
    }
}