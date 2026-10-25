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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlin.collections.isNotEmpty

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
    val primaryAccent = MaterialTheme.colorScheme.onPrimary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(10f)
    ) {
        // Core horizontal capsule bar container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.15f))
                .then(
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = if (isFocused) {
                                listOf(primaryAccent.copy(alpha = 0.25f), Color.Transparent)
                            } else {
                                listOf(onSurfaceColor.copy(alpha = 0.08f), Color.Transparent)
                            }
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    color = if (isFocused) primaryAccent.copy(alpha = 0.4f) else onSurfaceColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(32.dp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Pill Wing: Search Icon Indicator (interactive if you want it to clear focus or request search)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_icon),
                    tint = if (isFocused) primaryAccent else onSurfaceColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Central Capsule Area: Unbordered Fluid Input Field Engine
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onQueryChange(it)
                    showSuggestions = it.isNotEmpty()
                },
                interactionSource = interactionSource,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = stringResource(R.string.enter_a_city_name),
                        color = onSurfaceColor.copy(alpha = 0.35f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            onQueryChange("")
                            showSuggestions = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear_input),
                                tint = onSurfaceColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = onSurfaceColor,
                    unfocusedTextColor = onSurfaceColor,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = primaryAccent
                ),
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
                )
            )

            // Right Pill Wing: Vivid Map Action Discover Launch Endpoint
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp)
                    .background(onSurfaceColor.copy(alpha = 0.05f))
                    .clickable { onMapClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = stringResource(R.string.pick_from_map),
                    tint = onSurfaceColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Dropdown suggestions layout floating over underlying panels
        AnimatedVisibility(
            visible = isListVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            GlassyCard(
                cornerRadius = 24.dp,
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
                                        tint = primaryAccent.copy(alpha = 0.7f),
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

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview(showBackground = true)
@Composable
fun WeatherSearchBarPreview() {
    FyneCastTheme() {
        WeatherSearchBar(
            suggestions = emptyList(),
            onQueryChange = {},
            onSuggestionClick = {},
            onMapClick = {}
        )
    }
}