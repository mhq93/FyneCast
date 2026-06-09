package com.mhq.fynecast.domain.usecases.favorites

import androidx.paging.PagingData
import com.mhq.fynecast.data.database.FavoriteEntity
import com.mhq.fynecast.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow

class FetchFavoritesUseCase(private val repository: FavoritesRepository) {
    operator fun invoke(): Flow<PagingData<FavoriteEntity>> {
        return repository.fetchFavorites()
    }
}