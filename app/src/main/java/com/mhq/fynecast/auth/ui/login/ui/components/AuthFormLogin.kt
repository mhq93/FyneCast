package com.mhq.fynecast.auth.ui.login.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.AuthInputField
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.core.ui.theme.AbyssalVoid

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
    modifier: Modifier = Modifier
) {

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    GlassyCard(
        modifier = modifier.padding(horizontal = 36.dp)
    ) {
        AuthInputField(
            value = email,
            label = stringResource(R.string.email),
            placeholder = stringResource(R.string.enter_email),
            leadingIcon = Icons.Default.Email,
            trailingIcon = null,
            isError = !isEmailValid,
            onValueChange = onEmailChanged,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.focusRequester(focusRequester)
        )
        AuthInputField(
            value = password,
            label = stringResource(R.string.password),
            placeholder = stringResource(R.string.enter_password),
            leadingIcon = Icons.Default.Lock,
            trailingIcon = {
                val image =
                    if (isPasswordVisible)
                        Icons.Default.Visibility
                    else
                        Icons.Default.VisibilityOff
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = image,
                        contentDescription = null,
                        tint = AbyssalVoid
                    )
                }
            },
            isError = false,
            onValueChange = onPasswordChanged,
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
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.forgot_password),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.clickable { onForgotPasswordClick() }
            )
        }
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

@Preview
@Composable
private fun AuthFormLoginPreview() {
    FyneCastTheme() {
        AuthFormLogin(
            email = "username@gmail.com",
            password = "12345",
            isPasswordVisible = true,
            isEmailValid = true,
            onEmailChanged = {},
            onPasswordChanged = {},
            onTogglePasswordVisibility = {},
            onForgotPasswordClick = {}
        )
    }
}