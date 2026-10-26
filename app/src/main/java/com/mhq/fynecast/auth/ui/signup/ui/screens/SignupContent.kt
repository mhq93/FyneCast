package com.mhq.fynecast.auth.ui.signup.ui.screens

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
import com.mhq.fynecast.auth.ui.signup.ui.components.AuthFormSignup
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun SignupContent(
    username: String,
    isUsernameValid: Boolean,
    onUsernameChanged: (String) -> Unit,
    email: String,
    isEmailValid: Boolean,
    onEmailChanged: (String) -> Unit,
    password: String,
    onPasswordChanged: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChanged: (String) -> Unit,
    isPasswordValid: Boolean,
    doPasswordsMatch: Boolean,
    arePasswordsVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onSignupClick: () -> Unit,
    onLoginClick: () -> Unit,
    onGoogleAuthenticate: () -> Unit,
    onFacebookAuthenticate: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthScreenLayout(modifier = modifier) {
        AuthHeader(
            title = stringResource(R.string.auth_signup_title),
            subtitle = stringResource(R.string.auth_signup_subtitle)
        )
        Spacer(modifier = Modifier.height(20.dp))
        AuthFormSignup(
            username = username,
            isUsernameValid = isUsernameValid,
            onUsernameChanged = onUsernameChanged,
            userEmail = email,
            isEmailValid = isEmailValid,
            onUserEmailChanged = onEmailChanged,
            userPassword = password,
            isPasswordValid = isPasswordValid,
            onUserPasswordChanged = onPasswordChanged,
            confirmPassword = confirmPassword,
            onConfirmPasswordChanged = onConfirmPasswordChanged,
            doPasswordsMatch = doPasswordsMatch,
            arePasswordsVisible = arePasswordsVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            onSignupClick = onSignupClick
        )
        Spacer(modifier = Modifier.height(18.dp))
        AuthSocialSection(
            onGoogleAuthenticate = onGoogleAuthenticate,
            onFacebookAuthenticate = onFacebookAuthenticate
        )
        Spacer(modifier = Modifier.height(4.dp))
        AuthFooterLink(
            question = stringResource(R.string.already_have_an_account),
            action = stringResource(R.string.login),
            onClick = onLoginClick
        )
    }
}

@Composable
private fun SignupPreviewContent(isDarkMode: Boolean) {
    FyneCastTheme(isDarkMode = isDarkMode) {
        SignupContent(
            username = "",
            isUsernameValid = true,
            onUsernameChanged = {},
            email = "",
            isEmailValid = true,
            onEmailChanged = {},
            password = "",
            isPasswordValid = true,
            onPasswordChanged = {},
            confirmPassword = "",
            onConfirmPasswordChanged = {},
            doPasswordsMatch = true,
            arePasswordsVisible = false,
            onTogglePasswordVisibility = {},
            onSignupClick = {},
            onLoginClick = {},
            onGoogleAuthenticate = {},
            onFacebookAuthenticate = {}
        )
    }
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE, heightDp = 820)
@Composable
private fun SignupLightPreview() = SignupPreviewContent(isDarkMode = false)

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A,
    heightDp = 820
)
@Composable
private fun SignupDarkPreview() = SignupPreviewContent(isDarkMode = true)
