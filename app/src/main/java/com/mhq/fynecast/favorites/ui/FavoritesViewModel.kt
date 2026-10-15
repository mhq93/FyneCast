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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val deleteFavoriteUseCase: DeleteFavoriteUseCase,
) : ViewModel() {

    private val pendingDeletion = MutableStateFlow<FavoriteCityDomainModel?>(null)

    private val _uiEventChannel = Channel<String>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()

    val favoritesPageFlow: Flow<PagingData<FavoriteCityDomainModel>> =
        pendingDeletion
            .flatMapLatest { pending ->
                getFavoritesUseCase().map { pagingData ->
                    if (pending == null) pagingData
                    else pagingData.filter { it.stableKey != pending.stableKey }
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

    fun markPendingDeletion(city: FavoriteCityDomainModel) {
        if (pendingDeletion.value != null) return
        pendingDeletion.value = city
    }

    /** Called once the undo window has genuinely closed (snackbar dismissed/timed out). */
    fun confirmDelete() {
        val city = pendingDeletion.value ?: return
        viewModelScope.launch {
            try {
                deleteFavoriteUseCase(cityName = city.cityName, countryName = city.countryName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiEventChannel.send("Couldn't remove ${city.cityName}. Please try again.")
            } finally {
                pendingDeletion.value = null
            }
        }
    }

    fun undoDelete() {
        pendingDeletion.value = null
    }

    companion object {
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