package com.mhq.fynecast.home.data.datasource.location

import com.mhq.fynecast.home.data.api.location.LocationApiService
import com.mhq.fynecast.home.data.api.location.dto.PhotonResponseDto

class PhotonLocationRemoteDataSource(
    private val locationApiService: LocationApiService
) : LocationRemoteDataSource {

    override suspend fun searchPlaces(query: String, lang: String): PhotonResponseDto =
        locationApiService.getSuggestions(query = query, lang = photonLang(lang))

    override suspend fun reverseGeocode(lat: Double, lon: Double, lang: String): PhotonResponseDto =
        locationApiService.reverseGeocode(lat = lat, lon = lon, lang = photonLang(lang))

    /** Photon's public server only knows English, German and French;
     * anything else falls back to English. */
    private fun photonLang(lang: String): String =
        if (lang in PHOTON_LANGUAGES)
            lang
        else
            "en"

    private companion object {
        val PHOTON_LANGUAGES = setOf("en", "de", "fr")
    }
}
