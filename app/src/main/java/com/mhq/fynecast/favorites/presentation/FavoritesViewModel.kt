package com.mhq.fynecast.favorites.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.core.domain.location.SelectCityUseCase
import com.mhq.fynecast.core.domain.location.SelectedCity
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshAllFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RelocalizeFavoriteNamesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RestoreFavoriteUseCase
import com.mhq.fynecast.favorites.presentation.mapper.toPresentation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel(
    private val selectCityUseCase: SelectCityUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val deleteFavoriteUseCase: DeleteFavoriteUseCase,
    private val restoreFavoriteUseCase: RestoreFavoriteUseCase,
    private val refreshAllFavoritesUseCase: RefreshAllFavoritesUseCase,
    private val relocalizeFavoriteNamesUseCase: RelocalizeFavoriteNamesUseCase
) : ViewModel() {

    /** The domain favorites, kept privately: null = the first load has not finished yet. */
    private val domainFavorites: StateFlow<List<FavoriteCityDomainModel>?> =
        getFavoritesUseCase().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /** The screen only ever sees presentation models. */
    val uiState: StateFlow<FavoritesUiState> = domainFavorites
        .map { favorites ->
            when {
                favorites == null -> FavoritesUiState.Loading
                favorites.isEmpty() -> FavoritesUiState.Empty
                else -> FavoritesUiState.Success(favorites.map { it.toPresentation() })
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoritesUiState.Loading
        )

    private val _events = Channel<FavoritesEvent>(Channel.BUFFERED)
    val events: Flow<FavoritesEvent> = _events.receiveAsFlow()

    private var lastRemoved: FavoriteCityDomainModel? = null

    /** The user tapped a favorite row: make that city the one Home shows. */
    fun onFavoriteSelected(favoriteId: String) {
        val city = findFavorite(favoriteId) ?: return
        viewModelScope.launch {
            selectCityUseCase(
                SelectedCity(
                    name = "${city.cityName}, ${city.countryName}",
                    latitude = city.latitude,
                    longitude = city.longitude
                )
            )
        }
    }

    fun onDeleteFavorite(favoriteId: String) {
        val city = findFavorite(favoriteId) ?: return
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

    /**
     * Called when the Favorites tab is shown (and again if the app language changes).
     * Both use cases skip the work if they already ran recently.
     */
    fun onScreenShown(langCode: String) {
        viewModelScope.launch {
            try {
                relocalizeFavoriteNamesUseCase(langCode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Old names stay; nothing else to do.
            }
        }
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

    private fun findFavorite(favoriteId: String): FavoriteCityDomainModel? =
        domainFavorites.value?.firstOrNull { it.id == favoriteId }
}
