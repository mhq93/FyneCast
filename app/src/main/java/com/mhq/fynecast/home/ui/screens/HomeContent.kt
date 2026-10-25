package com.mhq.fynecast.home.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.format.DateTimeFormats
import com.mhq.fynecast.core.ui.format.TemperatureFormatter
import com.mhq.fynecast.core.ui.globalcomponents.currentLocale
import com.mhq.fynecast.home.presentation.model.CitySuggestionPresentationModel
import com.mhq.fynecast.home.presentation.model.WeatherPresentationModel
import com.mhq.fynecast.home.ui.components.WeatherDailyCarousel
import com.mhq.fynecast.home.ui.components.WeatherHeroCard
import com.mhq.fynecast.home.ui.components.WeatherHourlyCarousel
import com.mhq.fynecast.home.ui.components.WeatherSearchBar
import com.mhq.fynecast.home.ui.components.rememberCityTime
import kotlin.math.roundToInt

@Composable
fun HomeContent(
    weather: WeatherPresentationModel,
    citySuggestions: List<CitySuggestionPresentationModel>,
    selectedCityName: String,
    isMetric: Boolean,
    isFavorite: Boolean,
    isRefreshing: Boolean,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (CitySuggestionPresentationModel) -> Unit,
    onMapIconClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAlertsBannerClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // Today's range comes from the first forecast day.
    val today = weather.dailyForecasts.firstOrNull()
    val high =
        today?.let { TemperatureFormatter.format(if (isMetric) it.maxTempC else it.maxTempF) }
            ?: "–"
    val low = today?.let { TemperatureFormatter.format(if (isMetric) it.minTempC else it.minTempF) }
        ?: "–"
    val humidity = "${weather.stats.humidity}%"
    val windSpeed = (if (isMetric) weather.stats.windKph else weather.stats.windMph).roundToInt()
    val wind = "$windSpeed ${stringResource(if (isMetric) R.string.kph else R.string.mph)}"

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Sized to fit a normal phone without scrolling; the scroll is only a fallback for small screens.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = contentPadding.calculateBottomPadding() + 12.dp)
                .statusBarsPadding()
        ) {
            WeatherSearchBar(
                suggestions = citySuggestions,
                onQueryChange = onQueryChange,
                onSuggestionClick = onSuggestionClick,
                onMapClick = onMapIconClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
            WeatherHeroCard(
                icon = weather.conditionIcon,
                city = selectedCityName.substringBefore(","),
                timestamp = rememberCityTime(
                    timeZoneId = weather.timeZoneId,
                    fallback = weather.lastUpdated?.let {
                        DateTimeFormats.lastUpdated(it, currentLocale())
                    }.orEmpty()
                ),
                description = weather.conditionText,
                temperature = TemperatureFormatter.format(
                    if (isMetric)
                        weather.currentTempC
                    else
                        weather.currentTempF
                ),
                high = high,
                low = low,
                humidity = humidity,
                wind = wind,
                isFavorite = isFavorite,
                alertsNumber = weather.alertsCount,
                onToggleFavorite = onToggleFavorite,
                onAlertsBannerClick = onAlertsBannerClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
            WeatherHourlyCarousel(
                hourlyForecasts = weather.hourlyForecasts,
                isMetric = isMetric,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
            WeatherDailyCarousel(
                dailyForecasts = weather.dailyForecasts,
                isMetric = isMetric,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
        }
        AnimatedVisibility(
            visible = isRefreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }
    }
}
