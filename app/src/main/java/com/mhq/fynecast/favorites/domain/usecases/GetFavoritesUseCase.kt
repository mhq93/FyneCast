package com.mhq.fynecast.favorites.domain.usecases

import androidx.paging.PagingData
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow

class GetFavoritesUseCase(private val favoritesRepository: FavoritesRepository) {
    operator fun invoke(): Flow<PagingData<FavoriteCityDomainModel>> {
        return favoritesRepository.getAllFavorites()
    }
}