package com.mhq.fynecast.home.data.mapper

import com.mhq.fynecast.alerts.data.mapper.toDomain
import com.mhq.fynecast.home.data.api.weather.dto.WeatherApiResponse
import com.mhq.fynecast.home.domain.models.DailyForecastDomainModel
import com.mhq.fynecast.home.domain.models.HourlyForecastDomainModel
import com.mhq.fynecast.home.domain.models.WeatherDomainModel
import com.mhq.fynecast.home.domain.models.WeatherStatsDomainModel
import com.mhq.fynecast.util.TimestampFormatter

fun WeatherApiResponse.toDomain(): WeatherDomainModel {
    val domainStats = WeatherStatsDomainModel(
        humidity = this.current.humidity,
        windKph = this.current.windKph,
        windMph = this.current.windMph,
        pressureMb = this.current.pressureMb,
        pressureIn = this.current.pressureIn,
        uvIndex = this.current.uv
    )

    val domainDailyList = this.forecast.forecastDay.map { dayDto ->
        val formattedDates = TimestampFormatter.getFormattedDateAndTime(dayDto.date)
        DailyForecastDomainModel(
            dayName = formattedDates.first,
            monthDayStr = formattedDates.second,
            iconUrl = dayDto.day.dayCondition.icon,
            description = dayDto.day.dayCondition.description,
            maxTempC = dayDto.day.maxTempC,
            maxTempF = dayDto.day.maxTempF,
            minTempC = dayDto.day.minTempC,
            minTempF = dayDto.day.minTempF
        )
    }

    val firstDayDto = this.forecast.forecastDay.firstOrNull()
    val domainHourlyList = firstDayDto?.hour?.map { hourDto ->
        HourlyForecastDomainModel(
            formattedHour = TimestampFormatter.getFormattedHour(hourDto.hourTime),
            iconUrl = hourDto.hourCondition.icon,
            tempC = hourDto.hourTempC,
            tempF = hourDto.hourTempF
        )
    } ?: emptyList()

    val domainAlerts = this.alerts?.alert?.map { it.toDomain() } ?: emptyList()

    return WeatherDomainModel(
        cityName = this.location.cityName,
        country = this.location.country,
        latitude = this.location.cityLatitude,
        longitude = this.location.cityLongitude,
        currentTempC = this.current.tempC,
        currentTempF = this.current.tempF,
        conditionText = this.current.condition.description,
        conditionIcon = this.current.condition.icon,
        alerts = domainAlerts,
        stats = domainStats,
        dailyForecasts = domainDailyList,
        hourlyForecasts = domainHourlyList
    )
}