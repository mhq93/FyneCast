package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.core.domain.location.LocationRepository

class GetLocationSuggestionsUseCase(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(query: String, lang: String): List<CitySuggestionModel> {
        val sanitizedQuery = query.trim()
        if (sanitizedQuery.length <= 2) {
            return emptyList()
        }
        return locationRepository.getCitySuggestions(sanitizedQuery, lang)
    }
}