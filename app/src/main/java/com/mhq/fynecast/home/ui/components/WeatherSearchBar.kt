package com.mhq.fynecast.home.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
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
import com.mhq.fynecast.home.domain.models.CitySuggestionModel

@Composable
fun WeatherSearchBar(
    suggestions: List<CitySuggestionModel>,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (CitySuggestionModel) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }
    val isListVisible = showSuggestions && suggestions.isNotEmpty() && searchQuery.length > 2
    val focusManager = LocalFocusManager.current

    val onSurfaceColor = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(5f) // Increased to ensure dropdown sits beautifully above all carousel elements
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onQueryChange(it)
                showSuggestions = it.isNotEmpty()
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.enter_a_city_name),
                    color = onSurfaceColor.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_icon),
                    tint = onSurfaceColor.copy(alpha = 0.6f)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            onQueryChange("")
                            showSuggestions = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear_input),
                                tint = onSurfaceColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(onClick = onMapClick) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = stringResource(R.string.pick_from_map),
                            tint = onSurfaceColor.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = onSurfaceColor,
                unfocusedTextColor = onSurfaceColor,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f),
                focusedBorderColor = onSurfaceColor.copy(alpha = 0.25f),
                unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.1f),
                cursorColor = onSurfaceColor
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (isListVisible) {
                        onSuggestionClick(suggestions[0])
                        searchQuery = suggestions[0].fullName
                        showSuggestions = false
                        focusManager.clearFocus()
                    }
                }
            )
        )

        if (isListVisible) {
            // Updated to leverage your premium GlassyCard framework wrapper natively
            GlassyCard(
                cornerRadius = 24.dp,
                padding = 4.dp,
                containerAlpha = 0.15f,
                borderAlpha = 0.25f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    items(suggestions) { suggestion ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = suggestion.fullName,
                                    color = onSurfaceColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = onSurfaceColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchQuery = suggestion.fullName
                                    showSuggestions = false
                                    onSuggestionClick(suggestion)
                                    focusManager.clearFocus()
                                }
                        )
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