package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import java.time.LocalDate

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
        cornerRadius = 24.dp,
        padding = 14.dp,
        containerAlpha = 0.08f,
        borderAlpha = 0.15f,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.daily_forecast),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp.unlessArabic()
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            dailyForecasts.forEach { item ->
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
            }
        }
    }
}

private fun previewDays() = List(7) { i ->
    DailyForecastPresentationModel(
        date = LocalDate.of(2026, 10, 25).plusDays(i.toLong()),
        iconUrl = "",
        description = "Sunny",
        maxTempC = 28.0 - i,
        maxTempF = 82.0 - i,
        minTempC = 17.0 + i % 3,
        minTempF = 63.0 + i % 3
    )
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE)
@Composable
private fun WeatherDailyCarouselLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        WeatherDailyCarousel(dailyForecasts = previewDays(), isMetric = true)
    }
}

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
private fun WeatherDailyCarouselDarkPreview() {
    FyneCastTheme(isDarkMode = true) {
        WeatherDailyCarousel(dailyForecasts = previewDays(), isMetric = true)
    }
}
