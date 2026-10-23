package com.mhq.fynecast.core.navigation

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onItemSelected: (BottomNavTab) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .padding(
                start = 40.dp,
                end = 40.dp,
                bottom = 0.dp
            )
            .navigationBarsPadding()
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            BottomNavTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = currentRoute == tab.destination.route,
                    label = {
                        Text(
                            text = stringResource(id = tab.titleRes),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    onClick = { onItemSelected(tab) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun BottomNavigationBarPreview() {
    FyneCastTheme() {
        BottomNavigationBar(
            currentRoute = "",
            onItemSelected = {}
        )
    }
}