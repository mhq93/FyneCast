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