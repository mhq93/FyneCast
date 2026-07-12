package com.mhq.fynecast.favorites.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.models.stableKey
import com.mhq.fynecast.favorites.ui.components.FavoriteCityItem
import com.mhq.fynecast.favorites.ui.components.SwipeBackground
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesContent(
    favoriteCities: LazyPagingItems<FavoriteCityDomainModel>,
    onCityRowSelected: (FavoriteCityDomainModel) -> Unit,
    hasPendingDeletion: Boolean,
    onDeleteFavorite: (FavoriteCityDomainModel) -> Unit,
    isMetric: Boolean,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(vertical = 48.dp, horizontal = 24.dp)
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                count = favoriteCities.itemCount,
                key = favoriteCities.itemKey { it.stableKey }
            ) { index ->
                val favorite = favoriteCities[index]
                if (favorite != null) {
                    val currentFavorite by rememberUpdatedState(favorite)
                    val currentOnDelete by rememberUpdatedState(onDeleteFavorite)
                    val currentHasPendingDeletion by rememberUpdatedState(hasPendingDeletion)
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            value == SwipeToDismissBoxValue.EndToStart && !currentHasPendingDeletion
                        }
                    )

                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            currentOnDelete(currentFavorite)
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = { SwipeBackground(dismissState) },
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = !hasPendingDeletion
                    ) {
                        FavoriteCityItem(
                            favoriteCityDomainModel = favorite,
                            isMetric = isMetric,
                            modifier = Modifier.clickable(enabled = !hasPendingDeletion) {
                                onCityRowSelected(favorite)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun FavoritesContentPreview() {
    FyneCastTheme() {
        val mockFavoriteCities = flowOf(
            PagingData.empty<FavoriteCityDomainModel>()
        ).collectAsLazyPagingItems()

        FavoritesContent(
            favoriteCities = mockFavoriteCities,
            onCityRowSelected = {},
            onDeleteFavorite = {},
            hasPendingDeletion = true,
            isMetric = true
        )
    }
}