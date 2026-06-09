package com.mhq.fynecast.data.repository

import com.mhq.fynecast.domain.models.CitySuggestionModel

interface LocationRepository {
    suspend fun getCitySuggestions(query: String): List<CitySuggestionModel>
}