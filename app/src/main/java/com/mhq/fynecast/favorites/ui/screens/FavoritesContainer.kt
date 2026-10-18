package com.mhq.fynecast.favorites.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mhq.fynecast.R
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.ui.FavoritesEvent
import com.mhq.fynecast.favorites.ui.FavoritesViewModel
import com.mhq.fynecast.favorites.ui.components.UndoDeleteSnackbar
import kotlinx.coroutines.launch

@Composable
fun FavoritesContainer(
    favoritesViewModel: FavoritesViewModel = viewModel(factory = FavoritesViewModel.factory),
    onCityRowSelected: (FavoriteCityDomainModel) -> Unit,
    isMetric: Boolean,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val undoLabel = stringResource(R.string.undo)
    val lazyFavoriteCities = favoritesViewModel.favoritesPageFlow.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        favoritesViewModel.uiEvent.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(favoritesViewModel, resources) {
        favoritesViewModel.events.collect { event ->
            launch {   // each event gets its own coroutine so a new message replaces the old one
                snackbarHostState.currentSnackbarData?.dismiss()
                when (event) {
                    is FavoritesEvent.Removed -> {
                        val result = snackbarHostState.showSnackbar(
                            message = resources.getString(R.string.city_removed, event.cityName),
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) favoritesViewModel.onUndoRemove()
                    }
                    is FavoritesEvent.RemoveFailed ->
                        snackbarHostState.showSnackbar(
                            message = resources.getString(R.string.city_remove_failed, event.cityName),
                            duration = SnackbarDuration.Short
                        )
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (lazyFavoriteCities.itemCount) {
            0 if lazyFavoriteCities.loadState.refresh is LoadState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            0 -> {
                FavoritesBlank(
                    contentPadding = contentPadding
                )
            }
            else -> {
                FavoritesContent(
                    favoriteCities = lazyFavoriteCities,
                    onCityRowSelected = onCityRowSelected,
                    onDeleteFavorite = favoritesViewModel::onDeleteFavorite,
                    contentPadding = contentPadding,
                    isMetric = isMetric
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 160.dp)
        ) { data -> UndoDeleteSnackbar(snackbarData = data) }
    }
}