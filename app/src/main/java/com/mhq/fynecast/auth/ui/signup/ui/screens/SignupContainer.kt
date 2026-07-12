package com.mhq.fynecast.auth.ui.signup.ui.screens

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
import com.mhq.fynecast.auth.ui.signup.ui.SignupViewModel
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SignupContainer(
    signupViewModel: SignupViewModel = viewModel(factory = SignupViewModel.factory),
    onSignupClick: () -> Unit,
    onLoginNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userName by signupViewModel.username.collectAsStateWithLifecycle()
    val isUsernameValid by signupViewModel.isUsernameValid.collectAsStateWithLifecycle()
    val userEmail by signupViewModel.userEmail.collectAsStateWithLifecycle()
    val isEmailValid by signupViewModel.isEmailValid.collectAsStateWithLifecycle()
    val userPassword by signupViewModel.userPassword.collectAsStateWithLifecycle()
    val isPasswordValid by signupViewModel.isPasswordValid.collectAsStateWithLifecycle()
    val confirmPassword by signupViewModel.confirmPassword.collectAsStateWithLifecycle()
    val doPasswordsMatch by signupViewModel.doPasswordsMatch.collectAsStateWithLifecycle()
    val arePasswordsVisible by signupViewModel.arePasswordsVisible.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        signupViewModel.uiEvent.collectLatest { errorMessage ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        SignupContent(
            username = userName,
            isUsernameValid = isUsernameValid,
            onUsernameChanged = signupViewModel::onUsernameChanged,
            email = userEmail,
            isEmailValid = isEmailValid,
            onEmailChanged = signupViewModel::onUserEmailChanged,
            password = userPassword,
            isPasswordValid = isPasswordValid,
            onPasswordChanged = signupViewModel::onUserPasswordChanged,
            confirmPassword = confirmPassword,
            onConfirmPasswordChanged = signupViewModel::onConfirmPasswordChanged,
            doPasswordsMatch = doPasswordsMatch,
            arePasswordsVisible = arePasswordsVisible,
            onTogglePasswordVisibility = signupViewModel::togglePasswordsVisibility,
            onGoogleAuthenticate = {
                signupViewModel.onGoogleAuthenticate(context, onSignupClick)
            },
            onFacebookAuthenticate = {
                (context as? ComponentActivity)?.let { activity ->
                    signupViewModel.onFacebookAuthenticate(activity, onSignupClick)
                }
            },
            onLoginClick = onLoginNavigate,
            onSignupClick = { signupViewModel.validateThenRegister(onSignupClick) },
            modifier = modifier
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .padding(horizontal = 24.dp)
        ) { data -> AuthWarningSnackbar(data) }
    }
}