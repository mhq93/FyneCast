package com.mhq.fynecast.home.domain.mapper

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherDomainModel

fun WeatherDomainModel.toFavoriteCity() = FavoriteCityDomainModel(
    cityName = cityName,
    countryName = country,
    latitude = latitude,
    longitude = longitude,
    temperatureC = currentTempC,
    temperatureF = currentTempF,
    weatherIconUrl = conditionIcon
)

/** Same as [toFavoriteCity], but uses the name the user picked ("City, Country") when there is one. */
fun WeatherDomainModel.toFavoriteCity(displayName: String): FavoriteCityDomainModel {
    val base = toFavoriteCity()
    if (displayName.isBlank()) return base
    return base.copy(
        cityName = displayName.substringBefore(",").trim(),
        countryName = displayName.substringAfter(",", "").trim().ifBlank { base.countryName }
    )
}