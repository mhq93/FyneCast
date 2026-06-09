package com.mhq.fynecast.ui.screens.auth.forgotpassword.screens

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
fun ForgotPasswordContainer(
    viewModel: ForgotPasswordViewModel = viewModel(),
    onSendLinkSuccessNavigate: () -> Unit, // Renamed for operational clarity
    onBackToLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val isEmailValid by viewModel.isEmailValid.collectAsStateWithLifecycle()
    val isSubmitEnabled by viewModel.isSubmitEnabled.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Listens for both runtime data check errors and successful email notifications
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { message ->
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
            isSubmitEnabled = isSubmitEnabled,
            onEmailChanged = viewModel::onEmailChanged,
            onBackToLoginClick = onBackToLoginClick,
            onSendLink = {
                // Triggers validation checks and safe network delivery loops
                viewModel.sendPasswordReset(onSuccess = onSendLinkSuccessNavigate)
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