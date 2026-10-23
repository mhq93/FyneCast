package com.mhq.fynecast.home.data.mapper

import com.mhq.fynecast.home.data.api.weather.dto.WeatherApiResponse
import com.mhq.fynecast.core.domain.weather.DailyForecastDomainModel
import com.mhq.fynecast.core.domain.weather.HourlyForecastDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherStatsDomainModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val API_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val API_LOCAL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm")
private const val HOURLY_WINDOW = 24

private fun parseDateTime(raw: String, formatter: DateTimeFormatter): LocalDateTime? =
    runCatching { LocalDateTime.parse(raw, formatter) }.getOrNull()

fun WeatherApiResponse.toDomain(): WeatherDomainModel {
    val domainStats = WeatherStatsDomainModel(
        humidity = this.current.humidity,
        windKph = this.current.windKph,
        windMph = this.current.windMph,
        pressureMb = this.current.pressureMb,
        pressureIn = this.current.pressureIn,
        uvIndex = this.current.uv
    )

    val domainDailyList = forecast.forecastDay.mapNotNull { dayDto ->
        val date =
            runCatching { LocalDate.parse(dayDto.date) }.getOrNull() ?: return@mapNotNull null
        DailyForecastDomainModel(
            date = date,
            iconUrl = dayDto.day.dayCondition.icon,
            description = dayDto.day.dayCondition.description,
            maxTempC = dayDto.day.maxTempC,
            maxTempF = dayDto.day.maxTempF,
            minTempC = dayDto.day.minTempC,
            minTempF = dayDto.day.minTempF
        )
    }

    // Hours of the city's current local hour onward, across all forecast days.
    val allHours = forecast.forecastDay
        .flatMap { it.hour }
        .mapNotNull { hourDto ->
            parseDateTime(hourDto.hourTime, API_DATE_TIME)?.let { time ->
                HourlyForecastDomainModel(
                    time = time,
                    iconUrl = hourDto.hourCondition.icon,
                    tempC = hourDto.hourTempC,
                    tempF = hourDto.hourTempF
                )
            }
        }

    val currentLocalHour = parseDateTime(
        location.cityLocaltime,
        API_LOCAL_TIME
    )?.truncatedTo(ChronoUnit.HOURS)

    val domainHourlyList = (
            if (currentLocalHour == null)
                allHours
            else
                allHours.filter { !it.time.isBefore(currentLocalHour) }
            ).take(HOURLY_WINDOW)

    val domainAlerts = alerts?.alerts?.map { it.toDomain() }?.distinctBy { it.id } ?: emptyList()

    return WeatherDomainModel(
        cityName = this.location.cityName,
        country = this.location.country,
        latitude = this.location.cityLatitude,
        longitude = this.location.cityLongitude,
        currentTempC = this.current.tempC,
        currentTempF = this.current.tempF,
        conditionText = this.current.condition.description,
        conditionIcon = this.current.condition.icon,
        lastUpdated = parseDateTime(current.lastUpdated, API_DATE_TIME),
        timeZoneId = this.location.timeZoneId,
        alerts = domainAlerts,
        stats = domainStats,
        dailyForecasts = domainDailyList,
        hourlyForecasts = domainHourlyList
    )
}