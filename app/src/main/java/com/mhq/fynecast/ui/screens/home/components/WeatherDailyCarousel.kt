package com.mhq.fynecast.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.data.network.weather.dto.ForecastDayDto
import com.mhq.fynecast.ui.components.GlassyCard
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.util.TimestampFormatter

@Composable
fun WeatherDailyCarousel(
    daysList: List<ForecastDayDto>,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    GlassyCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = stringResource(R.string.daily_forecast),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(end = 4.dp)
                )
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
                items(daysList.size) { index ->
                    WeatherDailyItem(
                        day = TimestampFormatter.getFormattedDateAndTime(daysList[index].date).first,
                        date = TimestampFormatter.getFormattedDateAndTime(daysList[index].date).second,
                        weatherIconUrl = daysList[index].day.dayCondition.icon,
                        weatherDescription = daysList[index].day.dayCondition.description,
                        highTemperature = if (isMetric) daysList[index].day.maxTempC else daysList[index].day.maxTempF,
                        lowTemperature = if (isMetric) daysList[index].day.minTempC else daysList[index].day.minTempF,
                        isMetric = isMetric
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun WeatherDailyCarouselPreview() {
    FyneCastTheme() {
        //WeatherDailyCarousel()
    }
}