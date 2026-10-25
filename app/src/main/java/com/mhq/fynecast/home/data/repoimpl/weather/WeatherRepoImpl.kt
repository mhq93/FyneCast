package com.mhq.fynecast.home.data.repoimpl.weather

import com.mhq.fynecast.core.domain.weather.WeatherDomainModel
import com.mhq.fynecast.core.domain.weather.WeatherRepository
import com.mhq.fynecast.home.data.datasource.weather.WeatherRemoteDataSource
import com.mhq.fynecast.home.data.mapper.toDomain

class WeatherRepoImpl(
    private val remoteDataSource: WeatherRemoteDataSource
) : WeatherRepository {

    override suspend fun fetchWeather(query: String, lang: String): WeatherDomainModel =
        remoteDataSource.fetchWeather(query, lang).toDomain()
}