package com.mhq.fynecast.home.data.datasource.location

import com.mhq.fynecast.home.data.api.location.dto.PhotonResponseDto

/** Where place search and reverse geocoding come from over the network. Returns raw API data. */
interface LocationRemoteDataSource {
    suspend fun searchPlaces(query: String, lang: String): PhotonResponseDto
    suspend fun reverseGeocode(lat: Double, lon: Double, lang: String): PhotonResponseDto
}
