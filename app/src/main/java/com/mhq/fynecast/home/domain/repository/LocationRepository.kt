package com.mhq.fynecast.home.domain.repository

import com.mhq.fynecast.home.domain.models.CitySuggestionModel

interface LocationRepository {
    suspend fun getCitySuggestions(query: String, lang: String): List<CitySuggestionModel>
    suspend fun getCityNameOffMap(lat: Double, lon: Double, lang: String): CitySuggestionModel?
}