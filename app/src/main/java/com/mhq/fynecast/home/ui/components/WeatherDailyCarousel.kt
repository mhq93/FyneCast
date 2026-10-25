package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.format.DateTimeFormats
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.globalcomponents.currentLocale
import com.mhq.fynecast.core.ui.globalcomponents.unlessArabic
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.home.presentation.model.DailyForecastPresentationModel

@Composable
fun WeatherDailyCarousel(
    dailyForecasts: List<DailyForecastPresentationModel>,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    val locale = currentLocale()
    val weekMin = dailyForecasts.minOfOrNull { if (isMetric) it.minTempC else it.minTempF } ?: 0.0
    val weekMax = dailyForecasts.maxOfOrNull { if (isMetric) it.maxTempC else it.maxTempF } ?: 0.0

    GlassyCard(
        cornerRadius = 28.dp,
        padding = 20.dp,
        containerAlpha = 0.08f,
        borderAlpha = 0.15f,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.daily_forecast),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp.unlessArabic()
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                dailyForecasts.forEachIndexed { index, item ->
                    WeatherDailyRowItem(
                        day = DateTimeFormats.dayName(item.date, locale),
                        date = DateTimeFormats.monthDay(item.date, locale),
                        weekMin = weekMin,
                        weekMax = weekMax,
                        weatherIconUrl = item.iconUrl,
                        weatherDescription = item.description,
                        highTemperature = if (isMetric) item.maxTempC else item.maxTempF,
                        lowTemperature = if (isMetric) item.minTempC else item.minTempF
                    )

                    // Prevent drawing an isolated extra separator line at the final item index boundary
                    if (index < dailyForecasts.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
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
