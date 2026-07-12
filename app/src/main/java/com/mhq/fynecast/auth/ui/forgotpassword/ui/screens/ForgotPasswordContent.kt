package com.mhq.fynecast.auth.ui.forgotpassword.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.ui.forgotpassword.ui.components.AuthForgotPasswordForm
import com.mhq.fynecast.core.ui.globalcomponents.AppName
import com.mhq.fynecast.core.ui.globalcomponents.AuthActionButton
import com.mhq.fynecast.core.ui.globalcomponents.AuthFooterHyperlink

@Composable
fun ForgotPasswordContent(
    email: String,
    isEmailValid: Boolean,
    onEmailChanged: (String) -> Unit,
    onBackToLoginClick: () -> Unit,
    onSendLink: () -> Unit,
    modifier: Modifier = Modifier
) {

    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 24.dp)
    ) {
        AppName(
            text = stringResource(R.string.app_name)
        )
        AuthForgotPasswordForm(
            email = email,
            isEmailValid = isEmailValid,
            onEmailChanged = onEmailChanged,
        )
        AuthActionButton(
            text = stringResource(R.string.send_link),
            onDoAction = { onSendLink() },
        )
        AuthFooterHyperlink(
            question = stringResource(R.string.remembered_credentials),
            answer = stringResource(R.string.login),
            onDoAction = { onBackToLoginClick() }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordContentPreview() {
    ForgotPasswordContent(
        email = "developer@fynecast.com",
        isEmailValid = true,
        onEmailChanged = {},
        onBackToLoginClick = {},
        onSendLink = {}
    )
}