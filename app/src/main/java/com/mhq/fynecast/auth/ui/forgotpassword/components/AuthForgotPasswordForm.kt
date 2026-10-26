package com.mhq.fynecast.auth.ui.forgotpassword.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.ui.components.AuthPrimaryButton
import com.mhq.fynecast.auth.ui.components.AuthTextField
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AuthForgotPasswordForm(
    email: String,
    isEmailValid: Boolean,
    onEmailChanged: (String) -> Unit,
    onSendLink: () -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    GlassyCard(
        cornerRadius = 28.dp,
        padding = 20.dp,
        containerAlpha = 0.06f,
        borderAlpha = 0.15f,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthTextField(
                value = email,
                onValueChange = onEmailChanged,
                label = stringResource(R.string.email),
                placeholder = stringResource(R.string.enter_email),
                leadingIcon = Icons.Default.Email,
                isError = !isEmailValid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.focusRequester(focusRequester)
            )
            AuthPrimaryButton(
                text = stringResource(R.string.send_link),
                onClick = onSendLink
            )
        }
    }
}

@Composable
private fun AuthForgotPasswordFormPreviewContent() {
    AuthForgotPasswordForm(
        email = "user@example.com",
        isEmailValid = true,
        onEmailChanged = {},
        onSendLink = {},
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F2FE
)
@Composable
fun AuthForgotPasswordFormLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        AuthForgotPasswordFormPreviewContent()
    }
}

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
fun AuthForgotPasswordFormDarkPreview() {
    FyneCastTheme(isDarkMode = true) {
        AuthForgotPasswordFormPreviewContent()
    }
}