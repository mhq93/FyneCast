package com.mhq.fynecast.auth.ui.forgotpassword.ui.screens

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
import com.mhq.fynecast.auth.ui.forgotpassword.ui.ForgotPasswordViewModel
import com.mhq.fynecast.core.ui.globalcomponents.AuthWarningSnackbar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ForgotPasswordContainer(
    forgotPasswordViewModel: ForgotPasswordViewModel = viewModel(factory = ForgotPasswordViewModel.factory),
    onSendLinkSuccessNavigate: () -> Unit,
    onBackToLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val email by forgotPasswordViewModel.email.collectAsStateWithLifecycle()
    val isEmailValid by forgotPasswordViewModel.isEmailValid.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        forgotPasswordViewModel.uiEvent.collectLatest { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ForgotPasswordContent(
            email = email,
            isEmailValid = isEmailValid,
            onEmailChanged = forgotPasswordViewModel::onEmailChanged,
            onBackToLoginClick = onBackToLoginClick,
            onSendLink = {
                forgotPasswordViewModel.handlePasswordReset(onSuccess = onSendLinkSuccessNavigate)
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