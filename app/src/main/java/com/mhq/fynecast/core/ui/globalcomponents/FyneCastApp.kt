package com.mhq.fynecast.core.ui.globalcomponents

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mhq.fynecast.alerts.ui.AlertsViewModel
import com.mhq.fynecast.alerts.ui.screens.AlertsContainer
import com.mhq.fynecast.auth.ui.editprofile.ui.EditProfileViewModel
import com.mhq.fynecast.auth.ui.editprofile.ui.screens.EditProfileContainer
import com.mhq.fynecast.auth.ui.forgotpassword.ui.screens.ForgotPasswordContainer
import com.mhq.fynecast.auth.ui.login.ui.screens.LoginContainer
import com.mhq.fynecast.auth.ui.signup.ui.SignupViewModel
import com.mhq.fynecast.auth.ui.signup.ui.screens.SignupContainer
import com.mhq.fynecast.core.navigation.BottomNavigationBar
import com.mhq.fynecast.core.navigation.BottomNavigationItem
import com.mhq.fynecast.core.navigation.SessionViewModel
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.favorites.ui.FavoritesViewModel
import com.mhq.fynecast.favorites.ui.screens.FavoritesContainer
import com.mhq.fynecast.home.ui.HomeUiState
import com.mhq.fynecast.home.ui.HomeViewModel
import com.mhq.fynecast.home.ui.screens.HomeContainer
import com.mhq.fynecast.map.ui.MapPickerViewModel
import com.mhq.fynecast.map.ui.screens.MapPickerScreen
import com.mhq.fynecast.settings.domain.models.AppLanguage
import com.mhq.fynecast.settings.ui.SettingsViewModel
import com.mhq.fynecast.settings.ui.screens.SettingsContainer

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun FyneCastApp(
    homeViewModel: HomeViewModel,
    alertsViewModel: AlertsViewModel,
    settingsViewModel: SettingsViewModel,
    sessionViewModel: SessionViewModel,
    onRequestPermission: (String) -> Unit
) {

    val context = LocalContext.current
    val storedDarkMode by settingsViewModel.isDarkMode.collectAsState()
    val isDarkMode = storedDarkMode ?: isSystemInDarkTheme()
    val isMetric by settingsViewModel.isMetric.collectAsState()
    val currentLanguage by settingsViewModel.currentLanguage.collectAsState()
    val appLangCode = AppLanguage.fromStoredName(currentLanguage).code

    LaunchedEffect(appLangCode) {
        homeViewModel.onLanguageChanged(appLangCode)
    }

    FyneCastTheme(isDarkMode = isDarkMode) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val homeUiState = homeViewModel.state.collectAsState().value.uiState

        val rootRoutes = listOf(
            BottomNavigationItem.Home.route,
            BottomNavigationItem.Favorites.route,
            BottomNavigationItem.Alerts.route,
            BottomNavigationItem.Settings.route
        )

        val shouldShowBar = if (currentRoute == BottomNavigationItem.Home.route) {
            homeUiState !is HomeUiState.Loading
        } else {
            currentRoute in rootRoutes
        }

        val bottomContentPadding = if (shouldShowBar) {
            PaddingValues(bottom = 120.dp)
        } else {
            PaddingValues(0.dp)
        }

        BackHandler(enabled = currentRoute in rootRoutes) {
            if (currentRoute == BottomNavigationItem.Home.route) {
                (context as? Activity)?.finish()
            } else {
                navController.navigate(BottomNavigationItem.Home.route) {
                    popUpTo(BottomNavigationItem.Home.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(dynamicBackgroundGradient()))
            ) {
                NavHost(
                    navController = navController,
                    startDestination = sessionViewModel.startDestination,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(BottomNavigationItem.Signup.route) {
                        val signupViewModel: SignupViewModel =
                            viewModel(factory = SignupViewModel.factory)
                        SignupContainer(
                            onLoginNavigate = { navController.goToAuthScreen(BottomNavigationItem.Login.route) },
                            onSignupClick = { navController.enterApp() },
                            modifier = Modifier,
                            signupViewModel = signupViewModel
                        )
                    }

                    composable(BottomNavigationItem.Login.route) {
                        LoginContainer(
                            onSignUpNavigate = { navController.goToAuthScreen(BottomNavigationItem.Signup.route) },
                            onLoginClick = { navController.enterApp() },
                            onForgotPasswordClick = {
                                navController.goToAuthScreen(
                                    BottomNavigationItem.ForgotPassword.route
                                )
                            },
                            modifier = Modifier
                        )
                    }

                    composable(BottomNavigationItem.ForgotPassword.route) {
                        ForgotPasswordContainer(
                            onBackToLoginClick = { navController.goToAuthScreen(BottomNavigationItem.Login.route) },
                            onSendLinkSuccessNavigate = {
                                navController.goToAuthScreen(
                                    BottomNavigationItem.Login.route
                                )
                            },
                            modifier = Modifier
                        )
                    }

                    composable(BottomNavigationItem.EditProfile.route) {
                        val editProfileViewModel: EditProfileViewModel =
                            viewModel(factory = EditProfileViewModel.factory)
                        EditProfileContainer(
                            onBackClick = { navController.popBackStack() },
                            contentPadding = bottomContentPadding,
                            editProfileViewModel = editProfileViewModel
                        )
                    }

                    composable(BottomNavigationItem.Home.route) {
                        val isMetric by settingsViewModel.isMetric.collectAsState()
                        HomeContainer(
                            homeViewModel = homeViewModel,
                            onNavigateToMap = { navController.navigate("map_picker") },
                            onAlertsBannerClick = { navController.navigate(BottomNavigationItem.Alerts.route) },
                            onRequestPermission = onRequestPermission,
                            contentPadding = bottomContentPadding,
                            isMetric = isMetric,
                            langCode = appLangCode
                        )
                    }

                    composable(BottomNavigationItem.Favorites.route) {
                        val favoritesViewModel: FavoritesViewModel =
                            viewModel(factory = FavoritesViewModel.factory)

                        FavoritesContainer(
                            favoritesViewModel = favoritesViewModel,
                            onCityRowSelected = { favoriteCity ->
                                homeViewModel.onCitySelected(
                                    suggestionName = "${favoriteCity.cityName}, ${favoriteCity.countryName}",
                                    lat = favoriteCity.latitude,
                                    lon = favoriteCity.longitude,
                                    langCode = appLangCode
                                )
                                navController.navigate(BottomNavigationItem.Home.route) {
                                    popUpTo(BottomNavigationItem.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            contentPadding = bottomContentPadding,
                            isMetric = isMetric
                        )
                    }

                    composable(BottomNavigationItem.Alerts.route) {
                        AlertsContainer(
                            contentPadding = bottomContentPadding,
                            alertsViewModel = alertsViewModel
                        )
                    }

                    composable(BottomNavigationItem.Settings.route) {
                        SettingsContainer(
                            settingsViewModel = settingsViewModel,
                            contentPadding = bottomContentPadding,
                            onEditProfileClick = {
                                navController.navigate(BottomNavigationItem.EditProfile.route)
                            },
                            onLogoutClick = {
                                navController.navigate(BottomNavigationItem.Signup.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable("map_picker") {
                        val mapLangCode = AppLanguage.fromStoredName(currentLanguage).code
                        val mapPickerViewModel: MapPickerViewModel =
                            viewModel(factory = MapPickerViewModel.factory)

                        MapPickerScreen(
                            mapPickerViewModel = mapPickerViewModel,
                            langCode = mapLangCode,
                            onLocationPicked = { location ->
                                homeViewModel.onCitySelected(
                                    suggestionName = location.fullName,
                                    lat = location.latitude,
                                    lon = location.longitude,
                                    langCode = mapLangCode
                                )
                                if (navController.currentBackStackEntry?.destination?.route == "map_picker") {
                                    navController.popBackStack()
                                }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = shouldShowBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onItemSelected = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FyneCastAppPreview() {
    FyneCastTheme {}
}

private fun NavController.goToAuthScreen(route: String) {
    /** Return to it if it is already below us;
     * otherwise push it. Never stack duplicates. */
    if (!popBackStack(route, inclusive = false)) {
        navigate(route) { launchSingleTop = true }
    }
}

private fun NavController.enterApp() {
    navigate(BottomNavigationItem.Home.route) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}