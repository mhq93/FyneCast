package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.home.domain.models.SavedCityDomainModel
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class GetLastSearchedCityUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(): SavedCityDomainModel? {
        val prefs = userPreferencesRepository.getPreferences()
        val name = prefs.lastSearchedCityName
        val lat = prefs.lastSearchedCityLat
        val lon = prefs.lastSearchedCityLon
        return if (name != null && lat != null && lon != null) {
            SavedCityDomainModel(name, lat, lon)
        } else null
    }
}