package com.mhq.fynecast.auth.ui.login.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.ui.components.AuthFooterLink
import com.mhq.fynecast.auth.ui.components.AuthHeader
import com.mhq.fynecast.auth.ui.components.AuthScreenLayout
import com.mhq.fynecast.auth.ui.components.AuthSocialSection
import com.mhq.fynecast.auth.ui.login.ui.components.AuthFormLogin
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun LoginContent(
    email: String,
    isEmailValid: Boolean,
    onEmailChanged: (String) -> Unit,
    password: String,
    onPasswordChanged: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onGoogleAuthenticate: () -> Unit,
    onFacebookAuthenticate: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthScreenLayout(modifier = modifier) {
        AuthHeader(
            title = stringResource(R.string.auth_login_title),
            subtitle = stringResource(R.string.auth_login_subtitle)
        )
        Spacer(modifier = Modifier.height(24.dp))
        AuthFormLogin(
            email = email,
            isEmailValid = isEmailValid,
            onEmailChanged = onEmailChanged,
            password = password,
            onPasswordChanged = onPasswordChanged,
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            onForgotPasswordClick = onForgotPasswordClick,
            onLoginClick = onLoginClick
        )
        Spacer(modifier = Modifier.height(20.dp))
        AuthSocialSection(
            onGoogleAuthenticate = onGoogleAuthenticate,
            onFacebookAuthenticate = onFacebookAuthenticate
        )
        Spacer(modifier = Modifier.height(8.dp))
        AuthFooterLink(
            question = stringResource(R.string.don_t_have_an_account),
            action = stringResource(R.string.sign_up),
            onClick = onSignUpClick
        )
    }
}

@Composable
private fun LoginPreviewContent(isDarkMode: Boolean) {
    FyneCastTheme(isDarkMode = isDarkMode) {
        LoginContent(
            email = "",
            isEmailValid = true,
            onEmailChanged = {},
            password = "",
            onPasswordChanged = {},
            isPasswordVisible = false,
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onSignUpClick = {},
            onForgotPasswordClick = {},
            onGoogleAuthenticate = {},
            onFacebookAuthenticate = {}
        )
    }
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE, heightDp = 820)
@Composable
private fun LoginLightPreview() = LoginPreviewContent(isDarkMode = false)

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A,
    heightDp = 820
)
@Composable
private fun LoginDarkPreview() = LoginPreviewContent(isDarkMode = true)
