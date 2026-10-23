package com.mhq.fynecast.favorites.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshAllFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RestoreFavoriteUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel(
    getFavoritesUseCase: GetFavoritesUseCase,
    private val deleteFavoriteUseCase: DeleteFavoriteUseCase,
    private val restoreFavoriteUseCase: RestoreFavoriteUseCase,
    private val refreshAllFavoritesUseCase: RefreshAllFavoritesUseCase
) : ViewModel() {

    /** null = the first load has not finished yet; an empty list = the user has no favorites. */
    val favorites: StateFlow<List<FavoriteCityDomainModel>?> =
        getFavoritesUseCase().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _events = Channel<FavoritesEvent>(Channel.BUFFERED)
    val events: Flow<FavoritesEvent> = _events.receiveAsFlow()

    private var lastRemoved: FavoriteCityDomainModel? = null

    fun onDeleteFavorite(city: FavoriteCityDomainModel) {
        viewModelScope.launch {
            try {
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

    /** Called when the Favorites tab is shown; the use case skips the call if it ran recently. */
    fun onScreenShown(langCode: String) {
        viewModelScope.launch {
            try {
                refreshAllFavoritesUseCase(langCode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Old temperatures stay; nothing else to do.
            }
        }
    }
}