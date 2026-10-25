package com.mhq.fynecast.home.presentation.mapper

import com.mhq.fynecast.core.domain.weather.AlertDomainModel
import com.mhq.fynecast.core.domain.weather.DailyForecastDomainModel
import com.mhq.fynecast.core.domain.weather.HourlyForecastDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherStatsDomainModel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WeatherPresentationMapperTest {

    private fun alert(id: String) = AlertDomainModel(
        id = id, headline = "h", severity = "Severe", event = "Storm",
        effectiveMillis = 0L, expiresMillis = null, description = "d", instruction = "i"
    )

    private val weather = WeatherDomainModel(
        cityName = "Alexandria",
        country = "Egypt",
        latitude = 31.2,
        longitude = 29.9,
        currentTempC = 24.0,
        currentTempF = 75.2,
        conditionText = "Sunny",
        conditionIcon = "//cdn/icon.png",
        lastUpdated = LocalDateTime.of(2026, 10, 25, 12, 0),
        timeZoneId = "Africa/Cairo",
        alerts = listOf(alert("a"), alert("b")),
        stats = WeatherStatsDomainModel(
            humidity = 60, windKph = 10.0, windMph = 6.2,
            pressureMb = 1012.0, pressureIn = 29.9, uvIndex = 5.0
        ),
        dailyForecasts = listOf(
            DailyForecastDomainModel(
                date = LocalDate.of(2026, 10, 25), iconUrl = "d", description = "Clear",
                maxTempC = 27.0, maxTempF = 80.6, minTempC = 19.0, minTempF = 66.2
            )
        ),
        hourlyForecasts = listOf(
            HourlyForecastDomainModel(
                time = LocalDateTime.of(2026, 10, 25, 13, 0), iconUrl = "h", tempC = 25.0, tempF = 77.0
            )
        )
    )

    @Test
    fun `banner count is the number of alerts`() {
        assertEquals(2, weather.toPresentation().alertsCount)
    }

    @Test
    fun `location and both temperatures are carried over`() {
        val shown = weather.toPresentation()

        assertEquals(31.2, shown.latitude, 0.0)
        assertEquals(29.9, shown.longitude, 0.0)
        assertEquals(24.0, shown.currentTempC, 0.0)
        assertEquals(75.2, shown.currentTempF, 0.0)
    }

    @Test
    fun `stats and forecasts are mapped`() {
        val shown = weather.toPresentation()

        assertEquals(60, shown.stats.humidity)
        assertEquals(1, shown.dailyForecasts.size)
        assertEquals(27.0, shown.dailyForecasts[0].maxTempC, 0.0)
        assertEquals(1, shown.hourlyForecasts.size)
        assertEquals(77.0, shown.hourlyForecasts[0].tempF, 0.0)
    }
}
