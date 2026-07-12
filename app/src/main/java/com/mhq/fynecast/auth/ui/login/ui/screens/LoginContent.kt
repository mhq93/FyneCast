package com.mhq.fynecast.auth.ui.login.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.ui.login.ui.components.AuthFormLogin
import com.mhq.fynecast.core.ui.globalcomponents.AppName
import com.mhq.fynecast.core.ui.globalcomponents.AuthActionButton
import com.mhq.fynecast.core.ui.globalcomponents.AuthFooterHyperlink
import com.mhq.fynecast.core.ui.globalcomponents.AuthSocialOptions
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

    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
            .padding(vertical = 24.dp)
    ) {
        AppName(
            text = stringResource(R.string.app_name)
        )
        AuthFormLogin(
            email = email,
            isEmailValid = isEmailValid,
            onEmailChanged = onEmailChanged,
            password = password,
            onPasswordChanged = onPasswordChanged,
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            onForgotPasswordClick = onForgotPasswordClick,
        )
        AuthActionButton(
            text = stringResource(R.string.login),
            onDoAction = { onLoginClick() }
        )
        AuthSocialOptions(
            onGoogleAuthenticate = { onGoogleAuthenticate() },
            onFacebookAuthenticate = { onFacebookAuthenticate() }
        )
        AuthFooterHyperlink(
            question = stringResource(R.string.don_t_have_an_account),
            answer = stringResource(R.string.sign_up),
            onDoAction = { onSignUpClick() }
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
fun LoginScreenPreview() {
    FyneCastTheme() {
        LoginContent(
            email = "",
            isEmailValid = true,
            onEmailChanged = {},
            password = "",
            onPasswordChanged = {},
            isPasswordVisible = true,
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onSignUpClick = {},
            onForgotPasswordClick = {},
            onGoogleAuthenticate = {},
            onFacebookAuthenticate = {}
        )
    }
}