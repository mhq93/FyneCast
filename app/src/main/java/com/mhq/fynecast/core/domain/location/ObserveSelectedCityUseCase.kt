package com.mhq.fynecast.core.domain.location

import kotlinx.coroutines.flow.Flow

class ObserveSelectedCityUseCase(private val repository: SelectedCityRepository) {
    operator fun invoke(): Flow<SelectedCity> = repository.selections
}