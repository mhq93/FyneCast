package com.mhq.fynecast.ui.screens.auth.signup.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
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
import com.mhq.fynecast.ui.components.GlassyCard
import com.mhq.fynecast.ui.components.AppName
import com.mhq.fynecast.ui.components.AuthActionButton
import com.mhq.fynecast.ui.components.AuthInputField
import com.mhq.fynecast.ui.components.AuthFooterHyperlink
import com.mhq.fynecast.ui.components.AuthSocialOptions
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
fun SignupContent(
    userName: String = "",
    userEmail: String = "",
    userPassword: String = "",
    confirmPassword: String = "",
    isUsernameValid: Boolean = true,
    isEmailValid: Boolean = true,
    doPasswordsMatch: Boolean = true,
    arePasswordsVisible: Boolean = true,
    isSubmitEnabled: Boolean = true,
    onUsernameChanged: (String) -> Unit = {},
    onUserEmailChanged: (String) -> Unit = {},
    onUserPasswordChanged: (String) -> Unit = {},
    onConfirmPasswordChanged: (String) -> Unit = {},
    onTogglePasswordVisibility: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onRegisterSubmit: () -> Unit = {},
    onGoogleAuthenticate: () -> Unit = {},
    onFacebookAuthenticate: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    val isLightThemeActive = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val dynamicBackgroundGradient = if (isLightThemeActive) {
        listOf(LightMiddayBlue, LightBabyBlue, LightIceBlue)
    } else {
        listOf(MidnightBlue, DuskBlue, LilacBlue, BabyBlue)
    }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val gradientColors = listOf(NeonGreen, NeonGreen)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(dynamicBackgroundGradient))
    ) {
        // App Identity Header...
        AppName(
            text = "FyneCast",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Cursive,
            strokeWidth = 5f,
            strokeColor = Color.Black,
            gradientColors = gradientColors
        )
        // Central Input Group Card...
        GlassyCard(modifier = Modifier.padding(36.dp)) {
            // 1. Name Input Field
            AuthInputField(
                value = userName,
                label = stringResource(R.string.name),
                placeholder = stringResource(R.string.enter_name),
                leadingIcon = Icons.Default.Person,
                trailingIcon = null,
                isError = !isUsernameValid,
                onValueChange = onUsernameChanged,
                visualTransformation = VisualTransformation.None,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                //modifier = Modifier.focusRequester(focusRequester)
            )
            // 2. Email Input Field
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
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            // 3. Password Input Field
            AuthInputField(
                value = userPassword,
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.enter_password),
                leadingIcon = Icons.Default.Lock,
                trailingIcon = {
                    val image =
                        if (arePasswordsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(imageVector = image, contentDescription = null, tint = MidnightBlue)
                    }
                },
                isError = false,
                onValueChange = onUserPasswordChanged,
                visualTransformation = if (arePasswordsVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            // 4. Confirm Password Input Field
            AuthInputField(
                value = confirmPassword,
                label = stringResource(R.string.confirm_password),
                placeholder = stringResource(R.string.confirm_password),
                leadingIcon = Icons.Default.Lock,
                trailingIcon = {
                    val image =
                        if (arePasswordsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(imageVector = image, contentDescription = null, tint = MidnightBlue)
                    }
                },
                isError = !doPasswordsMatch,
                onValueChange = onConfirmPasswordChanged,
                visualTransformation = if (arePasswordsVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
        }
        // Signup Button
        AuthActionButton(
            text = stringResource(R.string.sign_up),
            isSubmitEnabled = isSubmitEnabled,
            onDoAction = { onRegisterSubmit() }
        )
        // Social Authentication Channels...
        AuthSocialOptions(
            onGoogleAuthenticate = { onGoogleAuthenticate() },
            onFacebookAuthenticate = { onFacebookAuthenticate() }
        )
        // Navigation Footer Links...
        AuthFooterHyperlink(
            question = stringResource(R.string.already_have_an_account),
            answer = stringResource(R.string.login),
            onDoAction = { onLoginClick() }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SignupScreenScreenPreview() {
    FyneCastTheme() {
        SignupContent()
    }
}