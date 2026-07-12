package com.mhq.fynecast.home.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.home.domain.models.CitySuggestionModel
import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.ui.components.WeatherDailyCarousel
import com.mhq.fynecast.home.ui.components.WeatherHeroCard
import com.mhq.fynecast.home.ui.components.WeatherHourlyCarousel
import com.mhq.fynecast.home.ui.components.WeatherSearchBar
import com.mhq.fynecast.home.ui.components.WeatherStatsGrid

@Composable
fun HomeContent(
    weatherDomainModel: WeatherDomainModel,
    citySuggestions: List<CitySuggestionModel>,
    selectedCityName: String,
    isMetric: Boolean,
    isFavorite: Boolean,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (CitySuggestionModel) -> Unit,
    onMapIconClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAlertsBannerClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp)
                .statusBarsPadding()
        ) {
            WeatherSearchBar(
                suggestions = citySuggestions,
                onQueryChange = onQueryChange,
                onSuggestionClick = onSuggestionClick,
                onMapClick = onMapIconClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            )
            WeatherHeroCard(
                icon = weatherDomainModel.conditionIcon,
                city = selectedCityName.substringBefore(","),
                timestamp = "",
                description = weatherDomainModel.conditionText,
                temperature = if (isMetric) "${weatherDomainModel.currentTempC}°" else "${weatherDomainModel.currentTempF}°",
                isFavorite = isFavorite,
                alertsNumber = weatherDomainModel.alerts.size,
                onToggleFavorite = onToggleFavorite,
                onAlertsBannerClick = onAlertsBannerClick,
                modifier = Modifier.fillMaxWidth()
            )
            WeatherHourlyCarousel(
                hourlyForecasts = weatherDomainModel.hourlyForecasts,
                isMetric = isMetric
            )
            WeatherDailyCarousel(
                dailyForecasts = weatherDomainModel.dailyForecasts,
                isMetric = isMetric
            )
            WeatherStatsGrid(
                stats = weatherDomainModel.stats,
                isMetric = isMetric
            )
        }
    }
}