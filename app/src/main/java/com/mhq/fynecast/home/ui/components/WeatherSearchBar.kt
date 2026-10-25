package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.home.presentation.model.CitySuggestionPresentationModel

private val SearchBarHeight = 44.dp

/** A slim glass pill: search icon, the field, a clear button and a map button. */
@Composable
fun WeatherSearchBar(
    suggestions: List<CitySuggestionPresentationModel>,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (CitySuggestionPresentationModel) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }
    val isListVisible = showSuggestions && suggestions.isNotEmpty() && searchQuery.length > 2
    val focusManager = LocalFocusManager.current

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val onSurfaceColor = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.onPrimary
    val pillShape = RoundedCornerShape(SearchBarHeight / 2)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(10f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SearchBarHeight)
                .clip(pillShape)
                .background(onSurfaceColor.copy(alpha = if (isFocused) 0.10f else 0.06f))
                .border(
                    width = 1.dp,
                    color = if (isFocused) accent.copy(alpha = 0.5f) else onSurfaceColor.copy(alpha = 0.12f),
                    shape = pillShape
                )
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search_icon),
                tint = if (isFocused) accent else onSurfaceColor.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = stringResource(R.string.enter_a_city_name),
                        color = onSurfaceColor.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onQueryChange(it)
                        showSuggestions = it.isNotEmpty()
                    },
                    singleLine = true,
                    interactionSource = interactionSource,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = onSurfaceColor),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (isListVisible) {
                                onSuggestionClick(suggestions[0])
                                searchQuery = suggestions[0].name
                                showSuggestions = false
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(SearchBarHeight)
                        .clip(CircleShape)
                        .clickable {
                            searchQuery = ""
                            onQueryChange("")
                            showSuggestions = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clear_input),
                        tint = onSurfaceColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // The whole 44 dp square is the tap target; the visible circle is smaller.
            Box(
                modifier = Modifier
                    .size(SearchBarHeight)
                    .clip(CircleShape)
                    .clickable { onMapClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(onSurfaceColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = stringResource(R.string.pick_from_map),
                        tint = onSurfaceColor.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isListVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            GlassyCard(
                cornerRadius = 22.dp,
                padding = 4.dp,
                containerAlpha = 0.25f,
                borderAlpha = 0.35f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .zIndex(11f)
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    items(suggestions) { suggestion ->
                        Column {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = suggestion.name,
                                        color = onSurfaceColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = accent.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        searchQuery = suggestion.name
                                        showSuggestions = false
                                        onSuggestionClick(suggestion)
                                        focusManager.clearFocus()
                                    }
                                    .padding(vertical = 2.dp)
                            )
                            if (suggestion != suggestions.last()) {
                                HorizontalDivider(
                                    color = onSurfaceColor.copy(alpha = 0.06f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE)
@Composable
private fun WeatherSearchBarLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        WeatherSearchBar(
            suggestions = emptyList(),
            onQueryChange = {},
            onSuggestionClick = {},
            onMapClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
private fun WeatherSearchBarDarkPreview() {
    FyneCastTheme(isDarkMode = true) {
        WeatherSearchBar(
            suggestions = emptyList(),
            onQueryChange = {},
            onSuggestionClick = {},
            onMapClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
