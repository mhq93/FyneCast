package com.mhq.fynecast.auth.ui.editprofile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.ui.components.AuthPasswordToggle
import com.mhq.fynecast.auth.ui.components.AuthPrimaryButton
import com.mhq.fynecast.auth.ui.components.AuthTextField
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun EditProfileForm(
    username: String,
    isUsernameValid: Boolean,
    onUsernameChanged: (String) -> Unit,
    userEmail: String,
    isEmailValid: Boolean,
    onUserEmailChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    showPasswordFields: Boolean = false,
    newPassword: String = "",
    isNewPasswordValid: Boolean = true,
    onNewPasswordChanged: (String) -> Unit = {},
    confirmPassword: String = "",
    doPasswordsMatch: Boolean = true,
    onConfirmPasswordChanged: (String) -> Unit = {},
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: () -> Unit = {},
    usernameFocusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val moveToNext = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })

    GlassyCard(
        cornerRadius = 28.dp,
        padding = 20.dp,
        containerAlpha = 0.06f,
        borderAlpha = 0.15f,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthTextField(
                value = username,
                onValueChange = onUsernameChanged,
                label = stringResource(R.string.name),
                placeholder = stringResource(R.string.enter_name),
                leadingIcon = Icons.Default.Person,
                isError = !isUsernameValid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = moveToNext,
                modifier = Modifier.focusRequester(usernameFocusRequester)
            )
            AuthTextField(
                value = userEmail,
                onValueChange = onUserEmailChanged,
                label = stringResource(R.string.email),
                placeholder = stringResource(R.string.enter_email),
                leadingIcon = Icons.Default.Email,
                isError = !isEmailValid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = if (showPasswordFields) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { focusManager.clearFocus() }
                )
            )
            if (showPasswordFields) {
                Text(
                    text = stringResource(R.string.email_switch_password_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                val passwordTransformation =
                    if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
                val visibilityToggle: @Composable () -> Unit = {
                    AuthPasswordToggle(
                        isPasswordVisible = isPasswordVisible,
                        onToggle = onTogglePasswordVisibility
                    )
                }
                AuthTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChanged,
                    label = stringResource(R.string.password),
                    placeholder = stringResource(R.string.enter_password),
                    leadingIcon = Icons.Default.Lock,
                    isError = !isNewPasswordValid,
                    trailingContent = visibilityToggle,
                    visualTransformation = passwordTransformation,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = moveToNext
                )
                AuthTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChanged,
                    label = stringResource(R.string.confirm_password),
                    placeholder = stringResource(R.string.confirm_password),
                    leadingIcon = Icons.Default.Lock,
                    isError = !doPasswordsMatch,
                    trailingContent = visibilityToggle,
                    visualTransformation = passwordTransformation,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }
            AuthPrimaryButton(
                text = stringResource(R.string.save_changes),
                onClick = onSaveClick
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun EditProfileFormPreview() {
    FyneCastTheme(isDarkMode = true) {
        EditProfileForm(
            username = "Mohamed",
            isUsernameValid = true,
            onUsernameChanged = {},
            userEmail = "mohamed@example.com",
            isEmailValid = true,
            onUserEmailChanged = {},
            onSaveClick = {}
        )
    }
}