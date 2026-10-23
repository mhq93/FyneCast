package com.mhq.fynecast.core.domain.location

import kotlinx.coroutines.flow.Flow

/** Carries "the user picked this city" from any screen to Home. Each pick is delivered once. */
interface SelectedCityRepository {
    val selections: Flow<SelectedCity>
    suspend fun select(city: SelectedCity)
}