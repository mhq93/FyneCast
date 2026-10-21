package com.mhq.fynecast.home.domain.repository

import com.mhq.fynecast.home.domain.models.CitySuggestionModel

interface LocationRepository {
    suspend fun getCitySuggestions(query: String, lang: String): List<CitySuggestionModel>
    suspend fun getCityNameOffMap(lat: Double, lon: Double, lang: String): CitySuggestionModel?
    /** [name] rewritten in [lang] for the place near these coordinates, or null if not found. */
    suspend fun getLocalizedPlaceName(name: String, lat: Double, lon: Double, lang: String): String?
}