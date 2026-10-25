package com.mhq.fynecast.auth.ui.login.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.auth.presentation.login.LoginViewModel
import com.mhq.fynecast.auth.ui.social.SocialTokenFetcher
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun LoginContainer(
    loginViewModel: LoginViewModel = viewModel(factory = AppViewModelFactory),
    onLoginClick: () -> Unit,
    onSignUpNavigate: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    val isEmailValid = loginViewModel.isEmailValid(email)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        loginViewModel.navigationEvent.collect { onLoginClick() }
    }

    LaunchedEffect(Unit) {
        loginViewModel.uiEvent.collectLatest { errorMessage ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = errorMessage.asString(context),
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LoginContent(
            email = email,
            isEmailValid = isEmailValid,
            onEmailChanged = { email = it.trim() },
            password = password,
            onPasswordChanged = { password = it },
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
            onSignUpClick = onSignUpNavigate,
            onLoginClick = { loginViewModel.onLoginAction(email, password) },            onForgotPasswordClick = onForgotPasswordClick,
            onGoogleAuthenticate = {
                scope.launch {
                    loginViewModel.onGoogleAuthenticate {
                        SocialTokenFetcher.getGoogleIdToken(context)
                    }
                }
            },
            onFacebookAuthenticate = {
                (context as? ComponentActivity)?.let { activity ->
                    scope.launch {
                        loginViewModel.onFacebookAuthenticate {
                            SocialTokenFetcher.getFacebookAccessToken(activity)
                        }
                    }
                }
            },
            modifier = modifier
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .padding(horizontal = 24.dp)
        ) { snackbarData -> AuthWarningSnackbar(snackbarData) }
    }
}