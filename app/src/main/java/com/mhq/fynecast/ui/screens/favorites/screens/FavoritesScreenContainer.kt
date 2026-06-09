package com.mhq.fynecast.ui.screens.favorites.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mhq.fynecast.data.database.FavoriteEntity
import com.mhq.fynecast.ui.theme.NeonGreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FavoritesScreenContainer(
    viewModel: FavoritesViewModel,
    onFavoriteClick: (FavoriteEntity) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
    onNavigateHome: () -> Unit
) {
    val lazyFavoriteCities = viewModel.favorites.collectAsLazyPagingItems()
    val isListEmpty = lazyFavoriteCities.itemCount == 0
    val isInitialLoading = lazyFavoriteCities.loadState.refresh is LoadState.Loading

    when {
        isInitialLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
        }
        isListEmpty -> {
            FavoritesScreenBlank(
                onNavigateHome = onNavigateHome,
                contentPadding = contentPadding,
                modifier = modifier
            )
        }
        else -> {
            FavoritesScreen(
                viewModel = viewModel,
                lazyFavoriteCities = lazyFavoriteCities,
                onFavoriteClick = onFavoriteClick,
                contentPadding = contentPadding,
                modifier = modifier
            )
        }
    }
}