package com.mhq.fynecast.home.data.api.location

import com.mhq.fynecast.home.data.api.location.dto.PhotonResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface LocationApiService {
    @GET("api/")
    suspend fun getSuggestions(
        @Query("q") query: String,
        @Query("lang") lang: String,
        @Query("limit") limit: Int = 5,
        @Query("osm_tag") tag: String = "place"
    ): PhotonResponseDto

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("lang") lang: String,
        @Query("limit") limit: Int = 10
        //@Query("osm_tag") tag: String = "place"
    ): PhotonResponseDto
}