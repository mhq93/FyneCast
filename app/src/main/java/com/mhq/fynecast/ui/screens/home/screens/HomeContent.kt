package com.mhq.fynecast.ui.screens.home.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.data.network.weather.WeatherApiResponse
import com.mhq.fynecast.data.network.weather.dto.AlertsDto
import com.mhq.fynecast.data.network.weather.dto.ConditionDto
import com.mhq.fynecast.data.network.weather.dto.CurrentDto
import com.mhq.fynecast.data.network.weather.dto.DayDto
import com.mhq.fynecast.data.network.weather.dto.ForecastDayDto
import com.mhq.fynecast.data.network.weather.dto.ForecastDto
import com.mhq.fynecast.data.network.weather.dto.HourDto
import com.mhq.fynecast.data.network.weather.dto.LocationDto
import com.mhq.fynecast.domain.models.CitySuggestionModel
import com.mhq.fynecast.ui.screens.home.components.WeatherDailyCarousel
import com.mhq.fynecast.ui.screens.home.components.WeatherHeroCard
import com.mhq.fynecast.ui.screens.home.components.WeatherHourlyCarousel
import com.mhq.fynecast.ui.screens.home.components.WeatherSearchBar
import com.mhq.fynecast.ui.screens.home.components.WeatherStatsGrid
import com.mhq.fynecast.ui.theme.BabyBlue
import com.mhq.fynecast.ui.theme.DuskBlue
import com.mhq.fynecast.ui.theme.LightBabyBlue
import com.mhq.fynecast.ui.theme.LightIceBlue
import com.mhq.fynecast.ui.theme.LightMiddayBlue
import com.mhq.fynecast.ui.theme.LilacBlue
import com.mhq.fynecast.ui.theme.MidnightBlue

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeContent(
    responseData: WeatherApiResponse,
    citySuggestions: List<CitySuggestionModel>,
    selectedCityName: String,
    isMetric: Boolean,
    isFavorite: Boolean,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (CitySuggestionModel) -> Unit,
    onMapIconClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val daysList = responseData.forecast.forecastDay

    val isLightThemeActive = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val dynamicBackgroundGradient = if (isLightThemeActive) {
        listOf(LightMiddayBlue, LightBabyBlue, LightIceBlue)
    } else {
        listOf(MidnightBlue, DuskBlue, LilacBlue, BabyBlue) // Deep Midnight spectrum
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(dynamicBackgroundGradient))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp)
                .statusBarsPadding()
        ) {
            //WeatherSearchBar...
            WeatherSearchBar(
                suggestions = citySuggestions,
                onQueryChange = onQueryChange,
                onSuggestionClick = onSuggestionClick,
                onMapClick = onMapIconClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            )
            //WeatherHeroCard...
            WeatherHeroCard(
                icon = responseData.current.condition.icon,
                city = selectedCityName,
                timestamp = responseData.location.cityLocaltime,
                description = responseData.current.condition.description,
                temperature = if (isMetric) "${responseData.current.tempC}°C" else "${responseData.current.tempF}°F",
                isFavorite = isFavorite,
                alertsNumber = responseData.alerts?.alert?.size ?: 0,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.fillMaxWidth(),
                isMetric = true,
            )
            //WeatherHourlyCarousel...
            WeatherHourlyCarousel(daysList, isMetric)
            //WeatherDailyCarousel...
            WeatherDailyCarousel(daysList, isMetric)
            //WeatherStatsGrid...
            WeatherStatsGrid(
                responseData = responseData,
                isMetric = isMetric,
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun HomeContentPreview() {
    // 1. Instantiating a valid model layout structure with your exact DTO keys
    val mockWeatherResponse = WeatherApiResponse(
        location = LocationDto(
            cityName = "Alexandria",
            cityRegion = "Alexandria",
            country = "Egypt",
            cityLatitude = 31.2001,
            cityLongitude = 29.9187,
            cityLocaltime = "2026-06-04 15:30"
        ),
        current = CurrentDto(
            tempC = 28.5,
            tempF = 83.3,
            isDay = 1,
            condition = ConditionDto(
                description = "Sunny and Clear",
                icon = "//://weatherapi.com"
            ),
            windKph = 16.0,
            windMph = 9.9,
            pressureMb = 1010.0,
            pressureIn = 1010.0,
            humidity = 55,
            cloud = 0,
            feelsLikeC = 29.0,
            feelsLikeF = 84.2,
            uv = 7.0
        ),
        forecast = ForecastDto(
            forecastDay = listOf(
                ForecastDayDto(
                    date = "2026-06-04",
                    day = DayDto( // Satisfies your DayDto layout reference mapping fields
                        maxTempC = 32.0,
                        minTempC = 22.0,
                        maxTempF = 32.0,
                        minTempF = 22.0,
                        dayCondition = ConditionDto("Sunny", "//://weatherapi.com"),
                        maxWindKph = 100.0,
                        maxWindMph = 100.0,
                        avgHumidity = 100,
                        dayUv = 10.0
                    ),
                    // Generates 24 hours to populate the hourly loop indexing list without crashes
                    hour = List(24) { hourIndex ->
                        HourDto(
                            hourTime = "2026-06-04 ${
                                hourIndex.toString().padStart(2, '0')
                            }:00",
                            hourTempC = 25.0,
                            hourTempF = 77.0,
                            hourCondition = ConditionDto(
                                "Sunny",
                                "//://weatherapi.com"
                            ),
                            isDay = 1,
                            hourHumidity = 50,
                            hourWindKph = 12.0
                        )
                    }
                )
            )
        ),
        alerts = AlertsDto(alert = emptyList())
    )

    // 2. Mount straight onto the stateless view template with valid dummy values
    HomeContent(
        responseData = mockWeatherResponse,
        citySuggestions = listOf(
            CitySuggestionModel(
                fullName = "Alexandria, Egypt",
                latitude = 31.2,
                longitude = 29.9
            ),
            CitySuggestionModel(
                fullName = "Cairo, Egypt",
                latitude = 30.0,
                longitude = 31.2
            )
        ),
        selectedCityName = "Alexandria, Egypt",
        isFavorite = true,
        isMetric = true,
        onQueryChange = {},
        onSuggestionClick = {},
        onMapIconClick = {},
        onToggleFavorite = {},
        contentPadding = PaddingValues(16.dp)
    )
}