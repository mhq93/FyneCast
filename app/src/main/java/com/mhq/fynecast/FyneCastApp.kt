package com.mhq.fynecast.app

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mhq.fynecast.ui.screens.alerts.screens.AlertsScreenContainer
import com.mhq.fynecast.ui.screens.alerts.screens.AlertsViewModel
import com.mhq.fynecast.ui.screens.favorites.screens.FavoritesScreenContainer
import com.mhq.fynecast.ui.screens.favorites.screens.FavoritesViewModel
import com.mhq.fynecast.ui.screens.home.screens.HomeUiState
import com.mhq.fynecast.ui.screens.home.screens.HomeViewModel
import com.mhq.fynecast.ui.screens.map.MapPickerScreen
import com.mhq.fynecast.ui.screens.home.screens.HomeContainer
import com.mhq.fynecast.ui.components.BottomNavigationBar
import com.mhq.fynecast.ui.components.NavigationScreens
import com.mhq.fynecast.ui.screens.settings.screens.EditProfileScreen
import com.mhq.fynecast.ui.screens.settings.screens.EditProfileViewModel
import com.mhq.fynecast.ui.screens.auth.login.screens.ForgotPasswordScreen
import com.mhq.fynecast.ui.screens.auth.login.screens.LoginScreen
import com.mhq.fynecast.ui.screens.auth.signup.screens.SignupScreen
import com.mhq.fynecast.ui.screens.settings.screens.SettingsScreen
import com.mhq.fynecast.ui.screens.settings.screens.SettingsViewModel
import com.mhq.fynecast.ui.theme.BabyBlue
import com.mhq.fynecast.ui.theme.DuskBlue
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.LilacBlue
import com.mhq.fynecast.ui.theme.MidnightBlue

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FyneCastApp(
    onRequestPermission: () -> Unit
) {

    // 1. Initialize your settings view model at the root navigation scope layer
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory)

    // 2. Collect your live dark mode configuration state reactively into Compose state
    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()

    // 3. Wrap your entire application scaffold inside your theme layer here!
    FyneCastTheme(isDarkMode = isDarkMode) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory)
        val homeUiState by homeViewModel.homeUiState.collectAsState()

        val rootRoutes = listOf(
            NavigationScreens.Home.route,
            NavigationScreens.Favorites.route,
            NavigationScreens.Alerts.route,
            NavigationScreens.Settings.route
        )

        val shouldShowBar = currentRoute in rootRoutes ||
                (currentRoute == NavigationScreens.Home.route && homeUiState is HomeUiState.Success)

        val context = LocalContext.current
        val container = (context.applicationContext as FyneCastApplication).container

        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = shouldShowBar,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    BottomNavigationBar(navController = navController)
                }
            },
            modifier = Modifier.background(
                Brush.verticalGradient(
                    listOf(
                        MidnightBlue,
                        DuskBlue,
                        LilacBlue,
                        BabyBlue
                    )
                )
            )
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = NavigationScreens.Signup.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(NavigationScreens.Signup.route) {
                    SignupScreen(
                        onLoginClick = {
                            navController.navigate(NavigationScreens.Login.route) {
                                launchSingleTop = true
                            }
                        },
                        onRegisterSubmit = {
                            navController.navigate(NavigationScreens.Home.route) {
                                popUpTo(NavigationScreens.Signup.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                composable(NavigationScreens.Login.route) {
                    LoginScreen(
                        onSignUpClick = {
                            navController.navigate(NavigationScreens.Signup.route) {
                                launchSingleTop = true
                            }
                        },
                        onLoginSuccess = {
                            navController.navigate(NavigationScreens.Home.route) {
                                popUpTo(NavigationScreens.Login.route) { inclusive = true }
                            }
                        },
                        onForgotPasswordClick = {
                            navController.navigate(NavigationScreens.ForgotPassword.route) {
                                popUpTo(NavigationScreens.ForgotPassword.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                composable(NavigationScreens.ForgotPassword.route) {
                    ForgotPasswordScreen(
                        onBackToLoginClick = {
                            //navController.popBackStack()
                            navController.navigate(NavigationScreens.Login.route) {
                                launchSingleTop = true
                            }
                        },
                        onSubmitSuccess = {
                            navController.navigate(NavigationScreens.Login.route) {
                                popUpTo(NavigationScreens.Login.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                composable(NavigationScreens.EditProfile.route) {
                    val editProfileViewModel: EditProfileViewModel =
                        viewModel(factory = EditProfileViewModel.factory)

                    EditProfileScreen(
                        onBackClick = { navController.popBackStack() },
                        contentPadding = innerPadding,
                        viewModel = editProfileViewModel
                    )
                }

                composable(NavigationScreens.Home.route) {
                    val isMetric by settingsViewModel.isMetric.collectAsState()

                    HomeContainer(
                        viewModel = homeViewModel,
                        onNavigateToMap = { navController.navigate("map_picker") },
                        onRequestPermission = onRequestPermission,
                        contentPadding = innerPadding,
                        isMetric = isMetric
                    )
                }

                composable(NavigationScreens.Favorites.route) {
                    val favoritesViewModel: FavoritesViewModel =
                        viewModel(factory = FavoritesViewModel.factory)

                    FavoritesScreenContainer(
                        viewModel = favoritesViewModel,
                        onFavoriteClick = { favorite ->
                            val query = "${favorite.latitude},${favorite.longitude}"
                            homeViewModel.selectedCityName = favorite.cityName
                            homeViewModel.fetchWeatherData(query)
                            navController.navigate(NavigationScreens.Home.route) {
                                popUpTo(NavigationScreens.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateHome = {
                            navController.navigate(NavigationScreens.Home.route) {
                                popUpTo(NavigationScreens.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        contentPadding = innerPadding
                    )
                }

                composable(NavigationScreens.Alerts.route) {
                    val alertsViewModel: AlertsViewModel =
                        viewModel(factory = AlertsViewModel.factory)

                    AlertsScreenContainer(
                        contentPadding = innerPadding,
                        viewModel = alertsViewModel,
                        onNavigateHome = {
                            navController.navigate(NavigationScreens.Home.route) {
                                popUpTo(NavigationScreens.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                composable(NavigationScreens.Settings.route) {
                    //val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory)

                    SettingsScreen(
                        contentPadding = innerPadding,
                        onLogoutClick = {
                            navController.navigate(NavigationScreens.Signup.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onEditProfileClick = {
                            navController.navigate(NavigationScreens.EditProfile.route) {
                                launchSingleTop = true
                            }
                        },
                        viewModel = settingsViewModel
                    )
                }

                composable("map_picker") {
                    MapPickerScreen(
                        onLocationPicked = { suggestion ->
                            homeViewModel.onCitySelected(suggestion)
                            if (navController.currentBackStackEntry?.destination?.route == "map_picker") {
                                navController.popBackStack()
                            }
                        },
                        photonLocationRepository = container.photonLocationRepository,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun FyneCastPreview() {
    FyneCastTheme {
        FyneCastApp(
            onRequestPermission = {}
        )
    }
}