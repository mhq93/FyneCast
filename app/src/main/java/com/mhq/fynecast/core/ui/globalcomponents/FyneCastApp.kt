package com.mhq.fynecast.core.ui.globalcomponents

import android.os.Build
import android.widget.Toast
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mhq.fynecast.R
import com.mhq.fynecast.alerts.presentation.AlertsViewModel
import com.mhq.fynecast.alerts.ui.screens.AlertsContainer
import com.mhq.fynecast.auth.presentation.editprofile.EditProfileViewModel
import com.mhq.fynecast.auth.ui.editprofile.ui.screens.EditProfileContainer
import com.mhq.fynecast.auth.ui.forgotpassword.ui.screens.ForgotPasswordContainer
import com.mhq.fynecast.auth.ui.login.ui.screens.LoginContainer
import com.mhq.fynecast.auth.presentation.signup.SignupViewModel
import com.mhq.fynecast.auth.ui.signup.ui.screens.SignupContainer
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.navigation.AppDestination
import com.mhq.fynecast.core.navigation.BottomNavTab
import com.mhq.fynecast.core.navigation.BottomNavigationBar
import com.mhq.fynecast.core.presentation.SessionViewModel
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.favorites.presentation.FavoritesViewModel
import com.mhq.fynecast.favorites.ui.screens.FavoritesContainer
import com.mhq.fynecast.home.presentation.HomeUiState
import com.mhq.fynecast.home.presentation.HomeViewModel
import com.mhq.fynecast.home.ui.screens.HomeContainer
import com.mhq.fynecast.map.presentation.MapPickerViewModel
import com.mhq.fynecast.map.ui.screens.MapPickerScreen
import com.mhq.fynecast.settings.presentation.SettingsViewModel
import com.mhq.fynecast.settings.ui.screens.SettingsContainer
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val AUTO_REFRESH_INTERVAL_MILLIS = 15 * 60 * 1000L

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun FyneCastApp(
    homeViewModel: HomeViewModel,
    alertsViewModel: AlertsViewModel,
    settingsViewModel: SettingsViewModel,
    sessionViewModel: SessionViewModel,
    onRequestPermission: (String) -> Unit
) {
    val isMetric by settingsViewModel.isMetric.collectAsStateWithLifecycle()
    val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()
    val appLangCode = currentLanguage.code
    val storedDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDarkMode = storedDarkMode ?: isSystemInDarkTheme()

    LaunchedEffect(appLangCode) {
        homeViewModel.onLanguageChanged(appLangCode)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, appLangCode) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(AUTO_REFRESH_INTERVAL_MILLIS.milliseconds)
                homeViewModel.onRefreshRequested(appLangCode)
            }
        }
    }

    FyneCastTheme(isDarkMode = isDarkMode) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val context = LocalContext.current
        LaunchedEffect(sessionViewModel, navController) {
            sessionViewModel.sessionExpired.collect {
                Toast.makeText(
                    context,
                    R.string.session_expired_sign_in_again,
                    Toast.LENGTH_LONG
                ).show()
                navController.navigate(AppDestination.Login.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        val homeUiState = homeViewModel.state.collectAsStateWithLifecycle().value.uiState

        val rootRoutes = BottomNavTab.entries.map { it.destination.route }

        val shouldShowBar = if (currentRoute == AppDestination.Home.route) {
            homeUiState !is HomeUiState.Loading
        } else {
            currentRoute in rootRoutes
        }

        val bottomContentPadding = if (shouldShowBar) {
            PaddingValues(bottom = 120.dp)
        } else {
            PaddingValues(0.dp)
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
                    composable(AppDestination.Signup.route) {
                        val signupViewModel: SignupViewModel =
                            viewModel(factory = AppViewModelFactory)
                        SignupContainer(
                            onLoginNavigate = { navController.goToAuthScreen(AppDestination.Login.route) },
                            onSignupClick = { navController.enterApp() },
                            modifier = Modifier,
                            signupViewModel = signupViewModel
                        )
                    }

                    composable(AppDestination.Login.route) {
                        LoginContainer(
                            onSignUpNavigate = { navController.goToAuthScreen(AppDestination.Signup.route) },
                            onLoginClick = { navController.enterApp() },
                            onForgotPasswordClick = {
                                navController.goToAuthScreen(
                                    AppDestination.ForgotPassword.route
                                )
                            },
                            modifier = Modifier
                        )
                    }

                    composable(AppDestination.ForgotPassword.route) {
                        ForgotPasswordContainer(
                            onBackToLoginClick = { navController.goToAuthScreen(AppDestination.Login.route) },
                            onSendLinkSuccessNavigate = {
                                navController.goToAuthScreen(
                                    AppDestination.Login.route
                                )
                            },
                            modifier = Modifier
                        )
                    }

                    composable(AppDestination.EditProfile.route) {
                        val editProfileViewModel: EditProfileViewModel =
                            viewModel(factory = AppViewModelFactory)
                        EditProfileContainer(
                            onBackClick = { navController.popBackStack() },
                            contentPadding = bottomContentPadding,
                            editProfileViewModel = editProfileViewModel
                        )
                    }

                    composable(AppDestination.Home.route) {
                        val isMetric by settingsViewModel.isMetric.collectAsStateWithLifecycle()
                        HomeContainer(
                            homeViewModel = homeViewModel,
                            onNavigateToMap = { navController.navigate(AppDestination.MapPicker.route) },
                            onAlertsBannerClick = { navController.switchToTab(BottomNavTab.Alerts) },
                            onRequestPermission = onRequestPermission,
                            contentPadding = bottomContentPadding,
                            isMetric = isMetric,
                            langCode = appLangCode
                        )
                    }

                    composable(AppDestination.Favorites.route) {
                        val favoritesViewModel: FavoritesViewModel =
                            viewModel(factory = AppViewModelFactory)

                        FavoritesContainer(
                            favoritesViewModel = favoritesViewModel,
                            onFavoriteSelected = { favoriteId ->
                                favoritesViewModel.onFavoriteSelected(favoriteId)
                                navController.switchToTab(BottomNavTab.Home)
                            },
                            contentPadding = bottomContentPadding,
                            isMetric = isMetric
                        )
                    }

                    composable(AppDestination.Alerts.route) {
                        AlertsContainer(
                            contentPadding = bottomContentPadding,
                            alertsViewModel = alertsViewModel
                        )
                    }

                    composable(AppDestination.Settings.route) {
                        SettingsContainer(
                            settingsViewModel = settingsViewModel,
                            contentPadding = bottomContentPadding,
                            onEditProfileClick = {
                                navController.navigate(AppDestination.EditProfile.route)
                            },
                            onLogoutClick = {
                                navController.navigate(AppDestination.Signup.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(AppDestination.MapPicker.route) {
                        val mapLangCode = currentLanguage.code
                        val mapPickerViewModel: MapPickerViewModel =
                            viewModel(factory = AppViewModelFactory)

                        val currentWeather = (homeUiState as? HomeUiState.Success)?.weather
                        MapPickerScreen(
                            mapPickerViewModel = mapPickerViewModel,
                            langCode = mapLangCode,
                            initialLatitude = currentWeather?.latitude,
                            initialLongitude = currentWeather?.longitude,
                            onLocationConfirmed = {
                                mapPickerViewModel.onLocationConfirmed()
                                if (navController.currentBackStackEntry?.destination?.route == AppDestination.MapPicker.route) {
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
                    onItemSelected = { tab -> navController.switchToTab(tab) }
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

/** Switches to a bottom-bar tab. Home is the base of the back stack, so "Home" just pops back to it. */
private fun NavController.switchToTab(tab: BottomNavTab) {
    val route = tab.destination.route
    if (currentDestination?.route == route) return
    if (tab == BottomNavTab.Home) {
        if (!popBackStack(route, inclusive = false)) {
            navigate(route) { launchSingleTop = true }
        }
    } else {
        navigate(route) {
            popUpTo(AppDestination.Home.route)
            launchSingleTop = true
        }
    }
}

private fun NavController.enterApp() {
    navigate(AppDestination.Home.route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}