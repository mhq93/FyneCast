package com.mhq.fynecast.ui.screens.auth.signup.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.R
import com.mhq.fynecast.ui.screens.auth.components.WarningSnackbar
import com.mhq.fynecast.ui.components.GlassyCard
import com.mhq.fynecast.ui.screens.auth.components.SocialMediaButton
import com.mhq.fynecast.ui.theme.BabyBlue
import com.mhq.fynecast.ui.theme.DuskBlue
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.LightBabyBlue
import com.mhq.fynecast.ui.theme.LightIceBlue
import com.mhq.fynecast.ui.theme.LightMiddayBlue
import com.mhq.fynecast.ui.theme.LilacBlue
import com.mhq.fynecast.ui.theme.MidnightBlue
import com.mhq.fynecast.ui.theme.NeonGreen
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(
    viewModel: SignupViewModel = viewModel(),
    onRegisterSubmit: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userPassword by viewModel.userPassword.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val arePasswordsVisible by viewModel.arePasswordsVisible.collectAsState()

    val isUsernameValid by viewModel.isUsernameValid.collectAsState()
    val isEmailValid by viewModel.isEmailValid.collectAsState()
    val doPasswordsMatch by viewModel.doPasswordsMatch.collectAsState()
    val isSubmitEnabled by viewModel.isSubmitEnabled.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        SignupContent(
            userName = userName,
            userEmail = userEmail,
            userPassword = userPassword,
            confirmPassword = confirmPassword,
            arePasswordsVisible = arePasswordsVisible,
            isUsernameValid = isUsernameValid,
            isEmailValid = isEmailValid,
            doPasswordsMatch = doPasswordsMatch,
            isSubmitEnabled = isSubmitEnabled,
            onUsernameChanged = viewModel::onUsernameChanged,
            onUserEmailChanged = viewModel::onUserEmailChanged,
            onUserPasswordChanged = viewModel::onUserPasswordChanged,
            onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
            onTogglePasswordVisibility = viewModel::togglePasswordsVisibility,
            onLoginClick = onLoginClick,
            onRegisterSubmit = {
                val validationError = viewModel.getFormValidationError()
                if (validationError != null) {
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = validationError,
                            duration = SnackbarDuration.Short
                        )
                    }
                } else {
                    onRegisterSubmit()
                }
            },
            modifier = modifier
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .padding(horizontal = 24.dp)
        ) { snackbarData ->
            WarningSnackbar(snackbarData)
        }
    }
}

@Composable
fun SignupContent(
    userName: String = "Hi",
    userEmail: String = "Hi",
    userPassword: String = "Hi",
    confirmPassword: String = "Hi",
    arePasswordsVisible: Boolean = true,
    isUsernameValid: Boolean = true,
    isEmailValid: Boolean = true,
    doPasswordsMatch: Boolean = true,
    isSubmitEnabled: Boolean = true,
    onUsernameChanged: (String) -> Unit = {},
    onUserEmailChanged: (String) -> Unit = {},
    onUserPasswordChanged: (String) -> Unit = {},
    onConfirmPasswordChanged: (String) -> Unit = {},
    onTogglePasswordVisibility: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onRegisterSubmit: () -> Unit = {},
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
        Box {
            Text(
                text = "FyneCast",
                style = TextStyle(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    drawStyle = Stroke(width = 5f)
                )
            )
            Text(
                text = "FyneCast",
                style = TextStyle(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    brush = Brush.verticalGradient(colors = gradientColors)
                )
            )
        }

        // Central Glassmorphic Input Group Card...
        GlassyCard(modifier = Modifier.padding(36.dp)) {
            // 1. Name Input Field
            OutlinedTextField(
                value = userName,
                onValueChange = onUsernameChanged,
                label = { Text("Name") },
                placeholder = { Text("Enter Name") },
                isError = !isUsernameValid,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MidnightBlue
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MidnightBlue,
                    unfocusedTextColor = MidnightBlue,
                    focusedLabelColor = MidnightBlue,
                    focusedBorderColor = MidnightBlue,
                    focusedContainerColor = LilacBlue,
                    unfocusedLabelColor = MidnightBlue.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.DarkGray,
                    unfocusedContainerColor = LilacBlue,
                )
            )
            // 2. Email Input Field
            OutlinedTextField(
                value = userEmail,
                onValueChange = onUserEmailChanged,
                label = { Text("Email") },
                isError = !isEmailValid,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MidnightBlue
                    )
                },
                placeholder = { Text("Enter Email") },
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MidnightBlue,
                    unfocusedTextColor = MidnightBlue,
                    focusedLabelColor = MidnightBlue,
                    focusedBorderColor = MidnightBlue,
                    focusedContainerColor = LilacBlue,
                    unfocusedLabelColor = MidnightBlue.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.DarkGray,
                    unfocusedContainerColor = LilacBlue,
                ),
                singleLine = true
            )
            // 3. Password Input Field
            OutlinedTextField(
                value = userPassword,
                onValueChange = onUserPasswordChanged,
                label = { Text("Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MidnightBlue
                    )
                },
                trailingIcon = {
                    val image =
                        if (arePasswordsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(imageVector = image, contentDescription = null, tint = MidnightBlue)
                    }
                },
                visualTransformation = if (arePasswordsVisible) VisualTransformation.None else PasswordVisualTransformation(),
                placeholder = { Text("Enter Password") },
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MidnightBlue,
                    unfocusedTextColor = MidnightBlue,
                    focusedLabelColor = MidnightBlue,
                    focusedBorderColor = MidnightBlue,
                    focusedContainerColor = LilacBlue,
                    unfocusedLabelColor = MidnightBlue.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.DarkGray,
                    unfocusedContainerColor = LilacBlue,
                ),
                singleLine = true
            )
            // 4. Confirm Password Input Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChanged,
                label = { Text("Confirm Password") },
                isError = !doPasswordsMatch,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MidnightBlue
                    )
                },
                trailingIcon = {
                    val image =
                        if (arePasswordsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(imageVector = image, contentDescription = null, tint = MidnightBlue)
                    }
                },
                visualTransformation = if (arePasswordsVisible) VisualTransformation.None else PasswordVisualTransformation(),
                placeholder = { Text("Confirm Password") },
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MidnightBlue,
                    unfocusedTextColor = MidnightBlue,
                    focusedLabelColor = MidnightBlue,
                    focusedBorderColor = MidnightBlue,
                    focusedContainerColor = LilacBlue,
                    unfocusedLabelColor = MidnightBlue.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.DarkGray,
                    unfocusedContainerColor = LilacBlue,
                ),
                singleLine = true
            )
        }

        // Submit Button
        Button(
            onClick = onRegisterSubmit,
            enabled = isSubmitEnabled,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MidnightBlue,
                contentColor = Color.White,
                disabledContainerColor = MidnightBlue.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .padding(horizontal = 36.dp, vertical = 16.dp)
                .height(56.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Sign up",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSubmitEnabled) NeonGreen else Color.Gray
            )
        }

        Row(horizontalArrangement = Arrangement.Center) {
            Text(
                text = "Or sign up with",
                fontWeight = FontWeight.Bold,
                color = MidnightBlue,
                modifier = Modifier.padding(16.dp)
            )
        }
        // Social Authentication Channels...
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            SocialMediaButton(
                iconRes = R.drawable.ic_google_primary_light,
                contentDescription = "Google Login",
                onClick = { },
            )
            Spacer(modifier = Modifier.width(16.dp))
            SocialMediaButton(
                iconRes = R.drawable.ic_facebook_primary_light,
                contentDescription = "Facebook Login",
                onClick = { },
            )
        }
        // Navigation Footer Links...
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = "Already have an account?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidnightBlue,
                    modifier = Modifier.padding(4.dp)
                )
                Text(
                    text = "Login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable { onLoginClick() })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignupScreenScreenPreview() {
    FyneCastTheme() {
        SignupContent()
    }
}