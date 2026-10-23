package com.mhq.fynecast.core.domain.location

class SelectCityUseCase(private val repository: SelectedCityRepository) {
    suspend operator fun invoke(city: SelectedCity) = repository.select(city)
}