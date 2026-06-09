package com.mhq.fynecast.ui.screens.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.alerts.dto.AlertsDto
import com.mhq.fynecast.home.components.WeatherHeroCard
import com.mhq.fynecast.home.components.WeatherItemDaily
import com.mhq.fynecast.home.components.WeatherItemHourly
import com.mhq.fynecast.home.components.WeatherSearchBar
import com.mhq.fynecast.home.dto.ConditionDto
import com.mhq.fynecast.home.dto.CurrentDto
import com.mhq.fynecast.home.dto.DayDto
import com.mhq.fynecast.home.dto.ForecastDayDto
import com.mhq.fynecast.home.dto.ForecastDto
import com.mhq.fynecast.home.dto.HourDto
import com.mhq.fynecast.home.dto.LocationDto
import com.mhq.fynecast.home.location.autocomplete.dto.CitySuggestion
import com.mhq.fynecast.home.network.WeatherApiResponse
import com.mhq.fynecast.ui.components.GlassyCard
import com.mhq.fynecast.ui.theme.BabyBlue
import com.mhq.fynecast.ui.theme.DuskBlue
import com.mhq.fynecast.ui.theme.LightBabyBlue
import com.mhq.fynecast.ui.theme.LightIceBlue
import com.mhq.fynecast.ui.theme.LightMiddayBlue
import com.mhq.fynecast.ui.theme.LilacBlue
import com.mhq.fynecast.ui.theme.MidnightBlue
import com.mhq.fynecast.util.TimestampFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    responseData: WeatherApiResponse,
    citySuggestions: List<CitySuggestion>,
    selectedCityName: String,
    isFavorite: Boolean,
    isMetric: Boolean,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (CitySuggestion) -> Unit,
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
        //.background(Brush.verticalGradient(listOf(MidnightBlue, DuskBlue, LilacBlue, BabyBlue)))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp)
                .statusBarsPadding()
        ) {
            // 1. Search Bar Alignment (Synced to 16.dp edge constraints)
            WeatherSearchBar(
                suggestions = citySuggestions,
                onQueryChange = onQueryChange,
                onSuggestionClick = onSuggestionClick,
                onMapClick = onMapIconClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            )

            // 2. Core Highlights Presentation Card Panel
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

            // 3. Hourly Extended Carousel Grouped inside ONE clean card
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
                            contentDescription = "Hourly Forecast",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = "Hourly Forecast",
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
                                WeatherItemHourly(
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

            // 4. 7-Day Extended Outlook Grouped inside ONE clean card
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
                            contentDescription = "Daily Forecast",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = "7-Day Forecast",
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
                            WeatherItemDaily(
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

            // 5. Symmetric Sub-metrics Grid Layout Rows
            Column(modifier = Modifier.fillMaxWidth()) {
                // ROW 1: Humidity and Wind Speed
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        GlassyCard(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.WaterDrop,
                                        contentDescription = "Humidity",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = "Humidity",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(
                                                fontSize = 48.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        ) {
                                            append(responseData.current.humidity.toString())
                                        }
                                        withStyle(
                                            style = SpanStyle(
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onBackground.copy(
                                                    alpha = 0.7f
                                                ),
                                                baselineShift = BaselineShift(0.0f)
                                            )
                                        ) {
                                            append("%")
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Text(
                                    text = " ",
                                    fontSize = 14.sp,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GlassyCard(
                            modifier = Modifier
                                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Air,
                                        contentDescription = "Wind Speed",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text =
                                            "Wind",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Text(
                                    text = responseData.current.windKph.toString(),
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Text(
                                    text = if (isMetric) "KPH" else "MPH",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                }// ROW 2: Barometric Pressure and UV Index Indicators
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        GlassyCard(
                            modifier = Modifier
                                .padding(
                                    start = 16.dp,
                                    end = 8.dp,
                                    top = 8.dp,
                                    bottom = 8.dp
                                )
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Compress,
                                        contentDescription = "Pressure",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = "Pressure",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Text(
                                    text = (if (isMetric) responseData.current.pressureMb else responseData.current.pressureIn).toString(),
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Text(
                                    text = if (isMetric) "Millibars" else "Inches",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GlassyCard(
                            modifier = Modifier
                                .padding(
                                    start = 8.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 8.dp
                                )
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.WbSunny,
                                        contentDescription = "UV Index",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = "UV Index",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Text(
                                    text = responseData.current.uv.toString(),
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Text(
                                    text = " ",
                                    fontSize = 14.sp,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
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
            CitySuggestion(
                fullName = "Alexandria, Egypt",
                latitude = 31.2,
                longitude = 29.9
            ),
            CitySuggestion(
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