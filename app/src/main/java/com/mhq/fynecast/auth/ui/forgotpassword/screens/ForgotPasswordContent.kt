package com.mhq.fynecast.auth.ui.forgotpassword.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.ui.components.AuthFooterLink
import com.mhq.fynecast.auth.ui.components.AuthHeader
import com.mhq.fynecast.auth.ui.components.AuthScreenLayout
import com.mhq.fynecast.auth.ui.forgotpassword.components.AuthForgotPasswordForm
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AuthScreenLayout(modifier = modifier) {
        AuthHeader(
            title = stringResource(R.string.recover_password),
            subtitle = stringResource(R.string.enter_the_email_associated_with_your_account) +
                "\n" +
                stringResource(R.string.we_will_send_a_link_to_reset_your_password),
            icon = Icons.Filled.LockReset
        )
        Spacer(modifier = Modifier.height(24.dp))
        AuthForgotPasswordForm(
            email = email,
            isEmailValid = isEmailValid,
            onEmailChanged = onEmailChanged,
            onSendLink = onSendLink,
            focusRequester = focusRequester
        )
        Spacer(modifier = Modifier.height(12.dp))
        AuthFooterLink(
            question = stringResource(R.string.remembered_credentials),
            action = stringResource(R.string.login),
            onClick = onBackToLoginClick
        )
    }
}

@Composable
private fun ForgotPasswordPreviewContent(isDarkMode: Boolean) {
    FyneCastTheme(isDarkMode = isDarkMode) {
        ForgotPasswordContent(
            email = "",
            isEmailValid = true,
            onEmailChanged = {},
            onBackToLoginClick = {},
            onSendLink = {}
        )
    }
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE, heightDp = 700)
@Composable
private fun ForgotPasswordLightPreview() = ForgotPasswordPreviewContent(isDarkMode = false)

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A,
    heightDp = 700
)
@Composable
private fun ForgotPasswordDarkPreview() = ForgotPasswordPreviewContent(isDarkMode = true)
