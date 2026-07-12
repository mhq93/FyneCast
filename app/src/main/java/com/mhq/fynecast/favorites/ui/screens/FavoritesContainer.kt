package com.mhq.fynecast.favorites.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
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
    val lazyFavoriteCities = favoritesViewModel.favoritesPageFlow.collectAsLazyPagingItems()
    val hasPendingDeletion by favoritesViewModel.hasPendingDeletion.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            lazyFavoriteCities.loadState.refresh is LoadState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            lazyFavoriteCities.itemCount == 0 -> {
                FavoritesBlank(
                    contentPadding = contentPadding
                )
            }

            else -> {
                FavoritesContent(
                    favoriteCities = lazyFavoriteCities,
                    onCityRowSelected = onCityRowSelected,
                    hasPendingDeletion = hasPendingDeletion,
                    onDeleteFavorite = { favorite ->
                        favoritesViewModel.deleteFavorite(favorite)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "${favorite.cityName} removed",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                favoritesViewModel.undoDelete()
                            }
                        }
                    },
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