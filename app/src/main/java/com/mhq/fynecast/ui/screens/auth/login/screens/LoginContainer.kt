package com.mhq.fynecast.ui.screens.auth.login.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.R
import com.mhq.fynecast.ui.components.GlassyCard
import com.mhq.fynecast.ui.screens.auth.components.AppName
import com.mhq.fynecast.ui.screens.auth.components.SocialMediaButton
import com.mhq.fynecast.ui.theme.BabyBlue
import com.mhq.fynecast.ui.theme.DuskBlue
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.LightBabyBlue
import com.mhq.fynecast.ui.theme.LightIceBlue
import com.mhq.fynecast.ui.theme.LightMiddayBlue
import com.mhq.fynecast.ui.theme.LilacBlue
import com.mhq.fynecast.ui.theme.MidnightBlue
import com.mhq.fynecast.ui.theme.NeonGreen
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onSignUpClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val passwordVisible by viewModel.passwordVisible.collectAsState()
    val isEmailValid by viewModel.isEmailValid.collectAsState()
    val isLoginEnabled by viewModel.isLoginEnabled.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LoginContent(
            email = email,
            password = password,
            passwordVisible = passwordVisible,
            isEmailValid = isEmailValid,
            isLoginEnabled = isLoginEnabled,
            onEmailChanged = viewModel::onEmailChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
            onSignUpClick = onSignUpClick,
            onLoginSubmit = {
                val validationError = viewModel.getFormValidationError()
                if (validationError != null) {
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = validationError,
                            duration = SnackbarDuration.Short
                        )
                    }
                } else {
                    onLoginSuccess()
                }
            },
            onForgotPasswordClick = onForgotPasswordClick,
            modifier = modifier
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // 1. Anchor higher: lifts the notification cleanly away from the glass navigation edge
                .padding(bottom = 96.dp)
                .padding(horizontal = 24.dp)
        ) { snackbarData ->
            // 2. Custom Card Layout replacing the plain standard box container
            Card(
                colors = CardDefaults.cardColors(containerColor = MidnightBlue),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // High-contrast Warning Alert Icon
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Alert Info",
                        tint = NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )

                    // Stylized Warning Note Text
                    Text(
                        text = snackbarData.visuals.message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}



