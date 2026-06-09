package com.mhq.fynecast.ui.screens.auth.login.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.R
import com.mhq.fynecast.ui.components.AppName
import com.mhq.fynecast.ui.components.AuthActionButton
import com.mhq.fynecast.ui.components.AuthFooterHyperlink
import com.mhq.fynecast.ui.components.AuthInputField
import com.mhq.fynecast.ui.components.AuthSocialOptions
import com.mhq.fynecast.ui.components.GlassyCard
import com.mhq.fynecast.ui.theme.BabyBlue
import com.mhq.fynecast.ui.theme.DuskBlue
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.LightBabyBlue
import com.mhq.fynecast.ui.theme.LightIceBlue
import com.mhq.fynecast.ui.theme.LightMiddayBlue
import com.mhq.fynecast.ui.theme.LilacBlue
import com.mhq.fynecast.ui.theme.MidnightBlue
import com.mhq.fynecast.ui.theme.NeonGreen

@Composable
fun LoginContent(
    email: String,
    password: String,
    passwordVisible: Boolean,
    isEmailValid: Boolean,
    isLoginEnabled: Boolean,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSignUpClick: () -> Unit,
    onLoginSubmit: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
    onGoogleAuthenticate: () -> Unit,
    onFacebookAuthenticate: () -> Unit
) {
    val isLightThemeActive = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val dynamicBackgroundGradient = remember(isLightThemeActive) {
        if (isLightThemeActive) {
            listOf(LightMiddayBlue, LightBabyBlue, LightIceBlue)
        } else {
            listOf(MidnightBlue, DuskBlue, LilacBlue, BabyBlue)
        }
    }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val gradientColors = listOf(NeonGreen, NeonGreen)
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(dynamicBackgroundGradient))
            .verticalScroll(scrollState)
            .padding(vertical = 24.dp)
    ) {
        AppName(
            text = "FyneCast",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Cursive,
            strokeWidth = 5f,
            strokeColor = Color.Black,
            gradientColors = gradientColors
        )

        GlassyCard(modifier = Modifier.padding(36.dp)) {
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
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.focusRequester(focusRequester)
            )

            AuthInputField(
                value = password,
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.enter_password),
                leadingIcon = Icons.Default.Lock,
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(imageVector = image, contentDescription = null, tint = MidnightBlue)
                    }
                },
                isError = false,
                onValueChange = onPasswordChanged,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
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
                    color = MidnightBlue,
                    modifier = Modifier.clickable { onForgotPasswordClick() }
                )
            }
        }

        AuthActionButton(
            text = stringResource(R.string.login),
            isSubmitEnabled = isLoginEnabled,
            onDoAction = { onLoginSubmit() }
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

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    FyneCastTheme() {
        LoginContent(
            email = "",
            password = "",
            passwordVisible = true,
            isEmailValid = true,
            isLoginEnabled = true,
            onEmailChanged = {},
            onPasswordChanged = {},
            onTogglePasswordVisibility = {},
            onSignUpClick = {},
            onLoginSubmit = {},
            onForgotPasswordClick = {},
            onGoogleAuthenticate = {},
            onFacebookAuthenticate = {}
        ) 
    }
}