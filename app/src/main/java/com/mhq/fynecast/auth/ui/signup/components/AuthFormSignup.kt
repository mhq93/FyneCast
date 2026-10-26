package com.mhq.fynecast.auth.ui.signup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
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
    onSignupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val moveToNext = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    val passwordTransformation =
        if (arePasswordsVisible) VisualTransformation.None else PasswordVisualTransformation()

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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = moveToNext
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
                    imeAction = ImeAction.Next
                ),
                keyboardActions = moveToNext
            )
            AuthTextField(
                value = userPassword,
                onValueChange = onUserPasswordChanged,
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.enter_password),
                leadingIcon = Icons.Default.Lock,
                isError = !isPasswordValid,
                trailingContent = {
                    AuthPasswordToggle(
                        isPasswordVisible = arePasswordsVisible,
                        onToggle = onTogglePasswordVisibility
                    )
                },
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
                trailingContent = {
                    AuthPasswordToggle(
                        isPasswordVisible = arePasswordsVisible,
                        onToggle = onTogglePasswordVisibility
                    )
                },
                visualTransformation = passwordTransformation,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
            AuthPrimaryButton(
                text = stringResource(R.string.sign_up),
                onClick = onSignupClick
            )
        }
    }
}
