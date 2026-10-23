package com.mhq.fynecast.favorites.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.ui.components.FavoriteCityItem
import com.mhq.fynecast.favorites.ui.components.SwipeBackground

@Composable
fun FavoritesContent(
    favoriteCities: List<FavoriteCityDomainModel>,
    onCityRowSelected: (FavoriteCityDomainModel) -> Unit,
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
                items = favoriteCities,
                key = { it.id }
            ) { favorite ->
                val currentFavorite by rememberUpdatedState(favorite)
                val currentOnDelete by rememberUpdatedState(onDeleteFavorite)

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            /** Runs once, when the user finishes the swipe. */
                            currentOnDelete(currentFavorite)
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
                        favoriteCityDomainModel = favorite,
                        isMetric = isMetric,
                        modifier = Modifier.clickable { onCityRowSelected(favorite) }
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
            favoriteCities = emptyList(),
            onCityRowSelected = {},
            onDeleteFavorite = {},
            isMetric = true
        )
    }
}