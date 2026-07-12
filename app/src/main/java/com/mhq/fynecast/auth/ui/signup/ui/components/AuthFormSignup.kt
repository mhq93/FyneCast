package com.mhq.fynecast.auth.ui.signup.ui.components

import android.content.res.Configuration
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
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
fun AuthFormSignup(
    username: String,
    isUsernameValid: Boolean,
    onUsernameChanged: (String) -> Unit,
    userEmail: String,
    isEmailValid: Boolean,
    onUserEmailChanged: (String) -> Unit,
    userPassword: String,
    isPasswordValid: Boolean,
    onUserPasswordChanged: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChanged: (String) -> Unit,
    doPasswordsMatch: Boolean,
    arePasswordsVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {

    val focusManager = LocalFocusManager.current
    val emailFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { usernameFocusRequester.requestFocus() }
            )
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
            keyboardActions = KeyboardActions(
                onNext = { emailFocusRequester.requestFocus() }
            )
        )
        AuthInputField(
            value = userPassword,
            label = stringResource(R.string.password),
            placeholder = stringResource(R.string.enter_password),
            leadingIcon = Icons.Default.Lock,
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector =
                            if (arePasswordsVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            isError = !isPasswordValid,
            onValueChange = onUserPasswordChanged,
            visualTransformation =
                if (arePasswordsVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions =
                KeyboardActions(
                    onNext = { passwordFocusRequester.requestFocus() }
                )
        )
        AuthInputField(
            value = confirmPassword,
            label = stringResource(R.string.confirm_password),
            placeholder = stringResource(R.string.confirm_password),
            leadingIcon = Icons.Default.Lock,
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector =
                            if (arePasswordsVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            },
            isError = !doPasswordsMatch,
            onValueChange = onConfirmPasswordChanged,
            visualTransformation =
                if (arePasswordsVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            )
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
private fun AuthFormSignupPreview() {
    FyneCastTheme() {
        AuthFormSignup(
            username = "Username",
            userEmail = "username@gmail.com",
            userPassword = "123abc",
            confirmPassword = "123abc",
            isUsernameValid = true,
            isEmailValid = true,
            isPasswordValid = true,
            doPasswordsMatch = true,
            arePasswordsVisible = true,
            onUsernameChanged = {},
            onUserEmailChanged = {},
            onUserPasswordChanged = {},
            onConfirmPasswordChanged = {},
            onTogglePasswordVisibility = {}
        )
    }
}