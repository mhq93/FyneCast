package com.mhq.fynecast.auth.ui.editprofile.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.mhq.fynecast.core.ui.globalcomponents.AuthInputField
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

    GlassyCard(
        modifier = modifier.padding(horizontal = 36.dp)
    ) {
        AuthInputField(
            value = username,
            label = stringResource(R.string.name),
            placeholder = stringResource(R.string.enter_name),
            leadingIcon = Icons.Default.Person,
            trailingIcon = null,
            isError = !isUsernameValid,
            onValueChange = onUsernameChanged,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = {
                focusManager.moveFocus(
                    FocusDirection.Down
                )
            }),
            modifier = Modifier.focusRequester(usernameFocusRequester)
        )
        Spacer(
            modifier = Modifier.height(8.dp)
        )
        AuthInputField(
            value = userEmail,
            label = stringResource(R.string.email),
            placeholder = stringResource(R.string.enter_email),
            leadingIcon = Icons.Default.Email,
            trailingIcon = null,
            isError = !isEmailValid,
            onValueChange = onUserEmailChanged,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = {
                focusManager.moveFocus(
                    FocusDirection.Down
                )
            })
        )
        if (showPasswordFields) {
            Text(
                text = stringResource(R.string.email_switch_password_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            val visibilityToggle: @Composable () -> Unit = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = stringResource(
                            if (isPasswordVisible) R.string.hide_password else R.string.show_password
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val passwordTransformation =
                if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()

            AuthInputField(
                value = newPassword,
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.enter_password),
                leadingIcon = Icons.Default.Lock,
                trailingIcon = visibilityToggle,
                isError = !isNewPasswordValid,
                onValueChange = onNewPasswordChanged,
                visualTransformation = passwordTransformation,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                })
            )
            Spacer(modifier = Modifier.height(8.dp))
            AuthInputField(
                value = confirmPassword,
                label = stringResource(R.string.confirm_password),
                placeholder = stringResource(R.string.confirm_password),
                leadingIcon = Icons.Default.Lock,
                trailingIcon = visibilityToggle,
                isError = !doPasswordsMatch,
                onValueChange = onConfirmPasswordChanged,
                visualTransformation = passwordTransformation,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
        }
    }
}

@Preview
@Composable
private fun EditProfileFormPreview() {
    FyneCastTheme() {
        EditProfileForm(
            username = "",
            isUsernameValid = true,
            onUsernameChanged = {},
            userEmail = "",
            isEmailValid = true,
            onUserEmailChanged = {}
        )
    }
}