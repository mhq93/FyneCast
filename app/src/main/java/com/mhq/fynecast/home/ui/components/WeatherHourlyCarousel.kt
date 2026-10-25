package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.format.DateTimeFormats
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.globalcomponents.currentLocale
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.home.presentation.model.HourlyForecastPresentationModel
import java.time.LocalDateTime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.core.ui.globalcomponents.unlessArabic

/**
 * A glass card holding one row of compact hour chips. The first chip is the current hour ("Now").
 * The row scrolls inside the card, and the next chip peeks in to show there is more.
 */
@Composable
fun WeatherHourlyCarousel(
    hourlyForecasts: List<HourlyForecastPresentationModel>,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    val locale = currentLocale()
    val nowLabel = stringResource(R.string.hourly_now)

    GlassyCard(
        cornerRadius = 24.dp,
        padding = 14.dp,
        containerAlpha = 0.08f,
        borderAlpha = 0.15f,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.hourly_forecast),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp.unlessArabic()
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = hourlyForecasts,
                    key = { _, item -> item.time.toString() }
                ) { index, item ->
                    WeatherHourlyItem(
                        header = if (index == 0) nowLabel else DateTimeFormats.hour(item.time, locale),
                        weatherIconUrl = item.iconUrl,
                        temperature = if (isMetric) item.tempC else item.tempF,
                        isCurrentHour = index == 0
                    )
                }
            }
        }
    }
}

private fun previewHours() = List(6) { i ->
    HourlyForecastPresentationModel(
        time = LocalDateTime.of(2026, 10, 25, 15, 0).plusHours(i.toLong()),
        iconUrl = "",
        tempC = 26.0 - i,
        tempF = 79.0 - i
    )
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE)
@Composable
private fun WeatherHourlyCarouselLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        WeatherHourlyCarousel(hourlyForecasts = previewHours(), isMetric = true)
    }
}

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
private fun WeatherHourlyCarouselDarkPreview() {
    FyneCastTheme(isDarkMode = true) {
        WeatherHourlyCarousel(hourlyForecasts = previewHours(), isMetric = true)
    }
}

//package com.mhq.fynecast.home.ui.components
//
//import android.content.res.Configuration
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import com.mhq.fynecast.R
//import com.mhq.fynecast.core.ui.format.DateTimeFormats
//import com.mhq.fynecast.core.ui.globalcomponents.currentLocale
//import com.mhq.fynecast.core.ui.theme.FyneCastTheme
//import com.mhq.fynecast.home.presentation.model.HourlyForecastPresentationModel
//import java.time.LocalDateTime
//
///**
// * A single row of compact hour chips. The first chip is the current hour ("Now").
// * It scrolls edge to edge, so the next chip peeks in to show there is more.
// */
//@Composable
//fun WeatherHourlyCarousel(
//    hourlyForecasts: List<HourlyForecastPresentationModel>,
//    isMetric: Boolean,
//    modifier: Modifier = Modifier
//) {
//    val locale = currentLocale()
//    val nowLabel = stringResource(R.string.hourly_now)
//
//    LazyRow(
//        modifier = modifier.fillMaxWidth(),
//        contentPadding = PaddingValues(horizontal = 16.dp),
//        horizontalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        itemsIndexed(
//            items = hourlyForecasts,
//            key = { _, item -> item.time.toString() }
//        ) { index, item ->
//            WeatherHourlyItem(
//                header = if (index == 0) nowLabel else DateTimeFormats.hour(item.time, locale),
//                weatherIconUrl = item.iconUrl,
//                temperature = if (isMetric) item.tempC else item.tempF,
//                isCurrentHour = index == 0
//            )
//        }
//    }
//}
//
//private fun previewHours() = List(6) { i ->
//    HourlyForecastPresentationModel(
//        time = LocalDateTime.of(2026, 10, 25, 15, 0).plusHours(i.toLong()),
//        iconUrl = "",
//        tempC = 26.0 - i,
//        tempF = 79.0 - i
//    )
//}
//
//@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE)
//@Composable
//private fun WeatherHourlyCarouselLightPreview() {
//    FyneCastTheme(isDarkMode = false) {
//        WeatherHourlyCarousel(hourlyForecasts = previewHours(), isMetric = true)
//    }
//}
//
//@Preview(
//    name = "Dark",
//    uiMode = Configuration.UI_MODE_NIGHT_YES,
//    showBackground = true,
//    backgroundColor = 0xFF0F172A
//)
//@Composable
//private fun WeatherHourlyCarouselDarkPreview() {
//    FyneCastTheme(isDarkMode = true) {
//        WeatherHourlyCarousel(hourlyForecasts = previewHours(), isMetric = true)
//    }
//}
