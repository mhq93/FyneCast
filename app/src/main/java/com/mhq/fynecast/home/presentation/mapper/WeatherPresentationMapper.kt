package com.mhq.fynecast.home.presentation.mapper

import com.mhq.fynecast.core.domain.weather.DailyForecastDomainModel
import com.mhq.fynecast.core.domain.weather.HourlyForecastDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherStatsDomainModel
import com.mhq.fynecast.home.presentation.model.DailyForecastPresentationModel
import com.mhq.fynecast.home.presentation.model.HourlyForecastPresentationModel
import com.mhq.fynecast.home.presentation.model.WeatherPresentationModel
import com.mhq.fynecast.home.presentation.model.WeatherStatsPresentationModel

fun WeatherDomainModel.toPresentation(): WeatherPresentationModel = WeatherPresentationModel(
    latitude = latitude,
    longitude = longitude,
    currentTempC = currentTempC,
    currentTempF = currentTempF,
    conditionText = conditionText,
    conditionIcon = conditionIcon,
    lastUpdated = lastUpdated,
    timeZoneId = timeZoneId,
    alertsCount = alerts.size,
    stats = stats.toPresentation(),
    dailyForecasts = dailyForecasts.map { it.toPresentation() },
    hourlyForecasts = hourlyForecasts.map { it.toPresentation() }
)

internal fun WeatherStatsDomainModel.toPresentation() = WeatherStatsPresentationModel(
    humidity = humidity,
    windKph = windKph,
    windMph = windMph,
    pressureMb = pressureMb,
    pressureIn = pressureIn,
    uvIndex = uvIndex
)

internal fun DailyForecastDomainModel.toPresentation() = DailyForecastPresentationModel(
    date = date,
    iconUrl = iconUrl,
    description = description,
    maxTempC = maxTempC,
    maxTempF = maxTempF,
    minTempC = minTempC,
    minTempF = minTempF
)

internal fun HourlyForecastDomainModel.toPresentation() = HourlyForecastPresentationModel(
    time = time,
    iconUrl = iconUrl,
    tempC = tempC,
    tempF = tempF
)
