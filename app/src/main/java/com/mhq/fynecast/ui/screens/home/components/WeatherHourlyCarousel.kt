package com.mhq.fynecast.ui.screens.home.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeatherHourlyCarousel(
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
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = stringResource(R.string.hourly_forecast),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = stringResource(R.string.hourly_forecast),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (daysList.isNotEmpty()) {
                    val todayHours = daysList[0].hour
                    items(todayHours.size) { index ->
                        WeatherHourlyItem(
                            header = TimestampFormatter.getFormattedHour(todayHours[index].hourTime),
                            weatherIconUrl = todayHours[index].hourCondition.icon,
                            temperature = if (isMetric) todayHours[index].hourTempC else todayHours[index].hourTempF,
                            isMetric = isMetric
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun WeatherHourlyCarouselPreview() {
    FyneCastTheme() {
        //WeatherHourlyCarousel()
    }
}