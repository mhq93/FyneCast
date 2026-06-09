package com.mhq.fynecast.ui.screens.auth.signup.screens

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.ui.components.AuthWarningSnackbar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SignupContainer(
    viewModel: SignupViewModel = viewModel(),
    onRegisterSuccessNavigate: () -> Unit,
    onLoginNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val userPassword by viewModel.userPassword.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.confirmPassword.collectAsStateWithLifecycle()
    val arePasswordsVisible by viewModel.arePasswordsVisible.collectAsStateWithLifecycle()

    val isUsernameValid by viewModel.isUsernameValid.collectAsStateWithLifecycle()
    val isEmailValid by viewModel.isEmailValid.collectAsStateWithLifecycle()
    val doPasswordsMatch by viewModel.doPasswordsMatch.collectAsStateWithLifecycle()
    val isSubmitEnabled by viewModel.isSubmitEnabled.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Intercept one-time snackbar triggers (both local regex and remote Firebase fails)
    LaunchedEffect(Unit) {
        viewModel.validationErrorEvent.collectLatest { errorMessage ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SignupContent(
            userName = userName,
            userEmail = userEmail,
            userPassword = userPassword,
            confirmPassword = confirmPassword,
            arePasswordsVisible = arePasswordsVisible,
            isUsernameValid = isUsernameValid,
            isEmailValid = isEmailValid,
            doPasswordsMatch = doPasswordsMatch,
            isSubmitEnabled = isSubmitEnabled,
            onUsernameChanged = viewModel::onUsernameChanged,
            onUserEmailChanged = viewModel::onUserEmailChanged,
            onUserPasswordChanged = viewModel::onUserPasswordChanged,
            onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
            onTogglePasswordVisibility = viewModel::togglePasswordsVisibility,
            onLoginClick = onLoginNavigate,
            onRegisterSubmit = {
                // Execute Firebase background registration flow
                viewModel.registerWithFirebase(onSuccess = onRegisterSuccessNavigate)
            },
            modifier = modifier
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .padding(horizontal = 24.dp)
        ) { snackbarData ->
            AuthWarningSnackbar(snackbarData)
        }
    }
}