package com.mhq.fynecast.navigation

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults.colors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mhq.fynecast.ui.theme.BabyBlue
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.MidnightBlue
import com.mhq.fynecast.ui.theme.NeonGreen

@Composable
fun BottomNavigationBar(navController: NavHostController) {

    val navigationItems = listOf(Screen.Home, Screen.Favorites, Screen.Alerts, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface(
        modifier = Modifier
            .padding(
                start = 40.dp,
                end = 40.dp,
                bottom = 0.dp
            )
            .navigationBarsPadding(),
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MidnightBlue
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            navigationItems.forEach { screen ->
                val isSelected =
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true

                NavigationBarItem(
                    selected = isSelected,
                    label = {
                        screen.title?.let {
                            Text(it, fontWeight = FontWeight.Bold)
                        }
                    },
                    icon = {
                        screen.icon?.let {
                            Icon(it, contentDescription = screen.title)
                        }
                    },
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = colors(
                        selectedIconColor = NeonGreen,
                        selectedTextColor = NeonGreen,
                        unselectedIconColor = BabyBlue.copy(alpha = 0.6f),
                        unselectedTextColor = BabyBlue.copy(alpha = 0.6f),
                        indicatorColor = MidnightBlue
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
        BottomNavigationBar(navController = rememberNavController())
    }
}