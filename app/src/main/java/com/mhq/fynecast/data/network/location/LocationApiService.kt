package com.mhq.fynecast.data.network.location

import com.mhq.fynecast.data.network.location.dto.PhotonResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface LocationApiService {
    @GET("api/")
    suspend fun getSuggestions(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("lang") lang: String = "en",
        //@Query("osm_tag") tag: String = "place"
    ): PhotonResponseDto

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("lang") lang: String = "en",
        //@Query("osm_tag") tag: String = "place"
    ): PhotonResponseDto
}
