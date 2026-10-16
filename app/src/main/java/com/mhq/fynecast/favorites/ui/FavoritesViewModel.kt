package com.mhq.fynecast.favorites.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RestoreFavoriteUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val deleteFavoriteUseCase: DeleteFavoriteUseCase,
    private val restoreFavoriteUseCase: RestoreFavoriteUseCase
) : ViewModel() {

    private val _uiEventChannel = Channel<String>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()

    val favoritesPageFlow: Flow<PagingData<FavoriteCityDomainModel>> =
        getFavoritesUseCase().cachedIn(viewModelScope)

    private val _events = Channel<FavoritesEvent>(Channel.BUFFERED)
    val events: Flow<FavoritesEvent> = _events.receiveAsFlow()

    private var lastRemoved: FavoriteCityDomainModel? = null

    fun onDeleteFavorite(city: FavoriteCityDomainModel) {
        viewModelScope.launch {
            try {
                // One quick DB call; it must finish even if the screen is closed right after.
                withContext(NonCancellable) { deleteFavoriteUseCase(city.id) }
                lastRemoved = city
                _events.send(FavoritesEvent.Removed(city.cityName))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(FavoritesEvent.RemoveFailed(city.cityName))
            }
        }
    }

    fun onUndoRemove() {
        val city = lastRemoved ?: return
        lastRemoved = null
        viewModelScope.launch { withContext(NonCancellable) { restoreFavoriteUseCase(city) } }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)

                FavoritesViewModel(
                    getFavoritesUseCase = app.container.getFavoritesUseCase,
                    deleteFavoriteUseCase = app.container.deleteFavoriteUseCase,
                    restoreFavoriteUseCase = app.container.restoreFavoriteUseCase
                )
            }
        }
    }
}