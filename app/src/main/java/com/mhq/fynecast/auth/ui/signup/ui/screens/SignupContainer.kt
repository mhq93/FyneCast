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
import com.mhq.fynecast.auth.presentation.signup.SignupViewModel
import com.mhq.fynecast.auth.ui.social.SocialTokenFetcher
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SignupContainer(
    signupViewModel: SignupViewModel = viewModel(factory = AppViewModelFactory),
    onSignupClick: () -> Unit,
    onLoginNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userName by rememberSaveable { mutableStateOf("") }
    var userEmail by rememberSaveable { mutableStateOf("") }
    var userPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var arePasswordsVisible by rememberSaveable { mutableStateOf(false) }

    val isUsernameValid = signupViewModel.isUsernameValid(userName)
    val isEmailValid = signupViewModel.isEmailValid(userEmail)
    val isPasswordValid = signupViewModel.isPasswordValid(userPassword)
    val doPasswordsMatch = signupViewModel.doPasswordsMatch(userPassword, confirmPassword)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        signupViewModel.navigationEvent.collect { onSignupClick() }
    }

    LaunchedEffect(Unit) {
        signupViewModel.uiEvent.collectLatest { errorMessage ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = errorMessage.asString(context),
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
            onUsernameChanged = { userName = it },
            email = userEmail,
            isEmailValid = isEmailValid,
            onEmailChanged = { userEmail = it.trim() },
            password = userPassword,
            isPasswordValid = isPasswordValid,
            onPasswordChanged = { userPassword = it },
            confirmPassword = confirmPassword,
            onConfirmPasswordChanged = { confirmPassword = it },
            doPasswordsMatch = doPasswordsMatch,
            arePasswordsVisible = arePasswordsVisible,
            onTogglePasswordVisibility = { arePasswordsVisible = !arePasswordsVisible },            onGoogleAuthenticate = {
                scope.launch {
                    signupViewModel.onGoogleAuthenticate {
                        SocialTokenFetcher.getGoogleIdToken(context)
                    }
                }
            },
            onFacebookAuthenticate = {
                (context as? ComponentActivity)?.let { activity ->
                    scope.launch {
                        signupViewModel.onFacebookAuthenticate {
                            SocialTokenFetcher.getFacebookAccessToken(activity)
                        }
                    }
                }
            },
            onLoginClick = onLoginNavigate,
            onSignupClick = {
                signupViewModel.onSignupAction(userName, userEmail, userPassword, confirmPassword)
            },
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