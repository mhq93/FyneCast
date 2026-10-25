package com.mhq.fynecast.favorites.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.favorites.presentation.model.FavoriteCityPresentationModel
import com.mhq.fynecast.favorites.ui.components.FavoriteCityItem
import com.mhq.fynecast.favorites.ui.components.SwipeBackground

@Composable
fun FavoritesContent(
    favorites: List<FavoriteCityPresentationModel>,
    onFavoriteClick: (favoriteId: String) -> Unit,
    onFavoriteDelete: (favoriteId: String) -> Unit,
    isMetric: Boolean,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    modifier: Modifier = Modifier
) {
    // Same 16 dp side margin as the Home cards.
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = favorites,
                key = { it.id }
            ) { favorite ->
                val currentFavorite by rememberUpdatedState(favorite)
                val currentOnDelete by rememberUpdatedState(onFavoriteDelete)

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            /** Runs once, when the user finishes the swipe. */
                            currentOnDelete(currentFavorite.id)
                        }
                        /**
                         * Never keep a row in the "swiped away" state.
                         * The row leaves the list when the delete lands, and a row brought back
                         * by Undo must start settled so it can be swiped again.
                         * */
                        false
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    modifier = Modifier.animateItem(),
                    backgroundContent = { SwipeBackground(dismissState) },
                    enableDismissFromStartToEnd = false
                ) {
                    FavoriteCityItem(
                        favorite = favorite,
                        isMetric = isMetric,
                        onClick = { onFavoriteClick(favorite.id) }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun FavoritesContentPreview() {
    FyneCastTheme {
        FavoritesContent(
            favorites = emptyList(),
            onFavoriteClick = {},
            onFavoriteDelete = {},
            isMetric = true
        )
    }
}
