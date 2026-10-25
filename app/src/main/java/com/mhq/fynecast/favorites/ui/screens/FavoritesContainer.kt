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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.R
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.ui.globalcomponents.currentLocale
import com.mhq.fynecast.favorites.presentation.FavoritesEvent
import com.mhq.fynecast.favorites.presentation.FavoritesUiState
import com.mhq.fynecast.favorites.presentation.FavoritesViewModel
import com.mhq.fynecast.favorites.ui.components.UndoDeleteSnackbar
import kotlinx.coroutines.launch

@Composable
fun FavoritesContainer(
    favoritesViewModel: FavoritesViewModel = viewModel(factory = AppViewModelFactory),
    onFavoriteSelected: (favoriteId: String) -> Unit,
    isMetric: Boolean,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val undoLabel = stringResource(R.string.undo)
    val uiState by favoritesViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val langCode = currentLocale().language

    /**
     * Brings every favorite's temperature up to date when the tab is shown
     * (the use case skips the call if it ran in the last 10 minutes).
     * */
    LaunchedEffect(langCode) {
        favoritesViewModel.onScreenShown(langCode)
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
        when (val state = uiState) {
            FavoritesUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            FavoritesUiState.Empty -> {
                FavoritesBlank(contentPadding = contentPadding)
            }

            is FavoritesUiState.Success -> {
                FavoritesContent(
                    favorites = state.favorites,
                    onFavoriteClick = onFavoriteSelected,
                    onFavoriteDelete = favoritesViewModel::onDeleteFavorite,
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
