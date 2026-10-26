package com.mhq.fynecast.auth.ui.login.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.ui.components.AuthPasswordToggle
import com.mhq.fynecast.auth.ui.components.AuthPrimaryButton
import com.mhq.fynecast.auth.ui.components.AuthTextField
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard

@Composable
fun AuthFormLogin(
    email: String,
    isEmailValid: Boolean,
    onEmailChanged: (String) -> Unit,
    password: String,
    onPasswordChanged: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLoginClick: () -> Unit,
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
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            AuthTextField(
                value = password,
                onValueChange = onPasswordChanged,
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.enter_password),
                leadingIcon = Icons.Default.Lock,
                isError = false,
                trailingContent = {
                    AuthPasswordToggle(
                        isPasswordVisible = isPasswordVisible,
                        onToggle = onTogglePasswordVisibility
                    )
                },
                visualTransformation =
                    if (isPasswordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )
            Text(
                text = stringResource(R.string.forgot_password),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(role = Role.Button, onClick = onForgotPasswordClick)
                    .padding(horizontal = 2.dp, vertical = 4.dp)
            )
            AuthPrimaryButton(
                text = stringResource(R.string.login),
                onClick = onLoginClick
            )
        }
    }
}
