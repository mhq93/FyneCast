package com.mhq.fynecast.auth.ui.signup.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.ui.signup.ui.components.AuthFormSignup
import com.mhq.fynecast.core.ui.globalcomponents.AppName
import com.mhq.fynecast.core.ui.globalcomponents.AuthActionButton
import com.mhq.fynecast.core.ui.globalcomponents.AuthFooterHyperlink
import com.mhq.fynecast.core.ui.globalcomponents.AuthSocialOptions
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

    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
    ) {
        AppName(
            text = stringResource(R.string.app_name)
        )
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
            onTogglePasswordVisibility = onTogglePasswordVisibility
        )
        AuthActionButton(
            text = stringResource(R.string.sign_up),
            onDoAction = onSignupClick
        )
        AuthSocialOptions(
            onGoogleAuthenticate = onGoogleAuthenticate,
            onFacebookAuthenticate = onFacebookAuthenticate
        )
        AuthFooterHyperlink(
            question = stringResource(R.string.already_have_an_account),
            answer = stringResource(R.string.login),
            onDoAction = onLoginClick
        )
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview(showBackground = true)
@Composable
fun SignupScreenScreenPreview() {
    FyneCastTheme() {
        SignupContent(
            username = "",
            isUsernameValid = true,
            onUsernameChanged = {},
            email = "",
            onEmailChanged = {},
            isEmailValid = true,
            password = "",
            isPasswordValid = true,
            onPasswordChanged = {},
            confirmPassword = "",
            onConfirmPasswordChanged = {},
            doPasswordsMatch = true,
            arePasswordsVisible = true,
            onTogglePasswordVisibility = {},
            onSignupClick = {},
            onLoginClick = {},
            onGoogleAuthenticate = {},
            onFacebookAuthenticate = {}
        )
    }
}