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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.auth.ui.login.ui.LoginViewModel
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginContainer(
    loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.factory),
    onLoginClick: () -> Unit,
    onSignUpNavigate: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val email by loginViewModel.email.collectAsStateWithLifecycle()
    val isEmailValid by loginViewModel.isEmailValid.collectAsStateWithLifecycle()
    val password by loginViewModel.password.collectAsStateWithLifecycle()
    val isPasswordVisible by loginViewModel.passwordVisible.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        loginViewModel.uiEvent.collectLatest { errorMessage ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LoginContent(
            email = email,
            isEmailValid = isEmailValid,
            onEmailChanged = loginViewModel::onEmailChanged,
            password = password,
            onPasswordChanged = loginViewModel::onPasswordChanged,
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = loginViewModel::togglePasswordVisibility,
            onSignUpClick = onSignUpNavigate,
            onLoginClick = { loginViewModel.validateThenLogin(onSuccess = onLoginClick) },
            onForgotPasswordClick = onForgotPasswordClick,
            onGoogleAuthenticate = {
                loginViewModel.onGoogleAuthenticate(context, onLoginClick)
            },
            onFacebookAuthenticate = {
                (context as? ComponentActivity)?.let { activity ->
                    loginViewModel.onFacebookAuthenticate(activity, onLoginClick)
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