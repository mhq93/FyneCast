package com.mhq.fynecast

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
import com.mhq.fynecast.ui.components.BottomNavigationScreens
import com.mhq.fynecast.ui.screens.auth.editprofile.EditProfileContainer
import com.mhq.fynecast.ui.screens.auth.editprofile.EditProfileViewModel
import com.mhq.fynecast.ui.screens.auth.forgotpassword.screens.ForgotPasswordContainer
import com.mhq.fynecast.ui.screens.auth.login.screens.LoginContainer
import com.mhq.fynecast.ui.screens.auth.signup.screens.SignupContainer
import com.mhq.fynecast.ui.screens.settings.screens.SettingsContainer
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

    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory)

    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()

    FyneCastTheme(isDarkMode = isDarkMode) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory)
        val homeUiState by homeViewModel.homeUiState.collectAsState()

        val rootRoutes = listOf(
            BottomNavigationScreens.Home.route,
            BottomNavigationScreens.Favorites.route,
            BottomNavigationScreens.Alerts.route,
            BottomNavigationScreens.Settings.route
        )

        val shouldShowBar = currentRoute in rootRoutes ||
                (currentRoute == BottomNavigationScreens.Home.route && homeUiState is HomeUiState.Success)

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
                startDestination = BottomNavigationScreens.Signup.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(BottomNavigationScreens.Signup.route) {
                    SignupContainer(
                        onLoginNavigate = {
                            navController.navigate(BottomNavigationScreens.Login.route) {
                                launchSingleTop = true
                            }
                        },
                        onRegisterSuccessNavigate = {
                            navController.navigate(BottomNavigationScreens.Home.route) {
                                popUpTo(BottomNavigationScreens.Signup.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                composable(BottomNavigationScreens.Login.route) {
                    LoginContainer(
                        onSignUpClick = {
                            navController.navigate(BottomNavigationScreens.Signup.route) {
                                launchSingleTop = true
                            }
                        },
                        onLoginSuccess = {
                            navController.navigate(BottomNavigationScreens.Home.route) {
                                popUpTo(BottomNavigationScreens.Login.route) { inclusive = true }
                            }
                        },
                        onForgotPasswordClick = {
                            navController.navigate(BottomNavigationScreens.ForgotPassword.route) {
                                popUpTo(BottomNavigationScreens.ForgotPassword.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                composable(BottomNavigationScreens.ForgotPassword.route) {
                    ForgotPasswordContainer(
                        onBackToLoginClick = {
                            navController.navigate(BottomNavigationScreens.Login.route) {
                                launchSingleTop = true
                            }
                        },
                        onSendLinkSuccessNavigate = {
                            navController.navigate(BottomNavigationScreens.Login.route) {
                                popUpTo(BottomNavigationScreens.Login.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                composable(BottomNavigationScreens.EditProfile.route) {
                    val editProfileViewModel: EditProfileViewModel =
                        viewModel(factory = EditProfileViewModel.factory)

                    EditProfileContainer(
                        onBackClick = { navController.popBackStack() },
                        contentPadding = innerPadding,
                        viewModel = editProfileViewModel
                    )
                }

                composable(BottomNavigationScreens.Home.route) {
                    val isMetric by settingsViewModel.isMetric.collectAsState()

                    HomeContainer(
                        viewModel = homeViewModel,
                        onNavigateToMap = { navController.navigate("map_picker") },
                        onRequestPermission = onRequestPermission,
                        contentPadding = innerPadding,
                        isMetric = isMetric
                    )
                }

                composable(BottomNavigationScreens.Favorites.route) {
                    val favoritesViewModel: FavoritesViewModel =
                        viewModel(factory = FavoritesViewModel.factory)

                    FavoritesScreenContainer(
                        viewModel = favoritesViewModel,
                        onFavoriteClick = { favorite ->
                            val query = "${favorite.latitude},${favorite.longitude}"
                            homeViewModel.selectedCityName = favorite.cityName
                            homeViewModel.fetchWeatherData(query)
                            navController.navigate(BottomNavigationScreens.Home.route) {
                                popUpTo(BottomNavigationScreens.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateHome = {
                            navController.navigate(BottomNavigationScreens.Home.route) {
                                popUpTo(BottomNavigationScreens.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        contentPadding = innerPadding
                    )
                }

                composable(BottomNavigationScreens.Alerts.route) {
                    val alertsViewModel: AlertsViewModel =
                        viewModel(factory = AlertsViewModel.factory)

                    AlertsScreenContainer(
                        contentPadding = innerPadding,
                        viewModel = alertsViewModel,
                        onNavigateHome = {
                            navController.navigate(BottomNavigationScreens.Home.route) {
                                popUpTo(BottomNavigationScreens.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                composable(BottomNavigationScreens.Settings.route) {
                    SettingsContainer(
                        contentPadding = innerPadding,
                        onLogoutClick = {
                            navController.navigate(BottomNavigationScreens.Signup.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onEditProfileClick = {
                            navController.navigate(BottomNavigationScreens.EditProfile.route) {
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