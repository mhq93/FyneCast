package com.mhq.fynecast.favorites.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.models.stableKey
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class FavoritesViewModel(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val deleteFavoriteUseCase: DeleteFavoriteUseCase,
) : ViewModel() {

    private data class PendingDeletion(val key: String, val job: Job)
    private val pendingDeletion = MutableStateFlow<PendingDeletion?>(null)

    val favoritesPageFlow: Flow<PagingData<FavoriteCityDomainModel>> =
        pendingDeletion
            .flatMapLatest { pending ->
                getFavoritesUseCase().map { pagingData ->
                    if (pending == null) pagingData
                    else pagingData.filter { it.stableKey != pending.key }
                }
            }
            .cachedIn(viewModelScope)

    val hasPendingDeletion: StateFlow<Boolean> =
        pendingDeletion
            .map { it != null }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                false
            )

    fun deleteFavorite(city: FavoriteCityDomainModel) {
        if (pendingDeletion.value != null) return

        val key = city.stableKey
        val job = viewModelScope.launch {
            delay(UNDO_GRACE_PERIOD_MS.milliseconds)
            runCatching {
                deleteFavoriteUseCase(cityName = city.cityName, countryName = city.countryName)
            }
            pendingDeletion.value = null
        }
        pendingDeletion.value = PendingDeletion(key, job)
    }

    fun undoDelete() {
        pendingDeletion.value?.job?.cancel()
        pendingDeletion.value = null
    }

    companion object {
        private const val UNDO_GRACE_PERIOD_MS = 4_000L

        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)

                FavoritesViewModel(
                    getFavoritesUseCase = app.container.getFavoritesUseCase,
                    deleteFavoriteUseCase = app.container.deleteFavoriteUseCase
                )
            }
        }
    }
}