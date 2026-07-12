package com.mhq.fynecast.home.data.api.weather

import com.mhq.fynecast.home.data.api.weather.dto.WeatherApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v1/forecast.json")
    suspend fun getWeatherData(
        @Query("key") key: String,
        @Query("q") city: String,
        @Query("days") days: Int = 7,
        @Query("aqi") airQualityInfo: String = "no",
        @Query("alerts") alerts: String = "yes",
        @Query("lang") languageCode: String
    ): WeatherApiResponse
}