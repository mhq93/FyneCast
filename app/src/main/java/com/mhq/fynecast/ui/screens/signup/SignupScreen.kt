package com.mhq.fynecast.ui.screens.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.R
import com.mhq.fynecast.ui.components.GlassyCard
import com.mhq.fynecast.ui.components.SocialMediaButton
import com.mhq.fynecast.ui.theme.BabyBlue
import com.mhq.fynecast.ui.theme.DuskBlue
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.LilacBlue
import com.mhq.fynecast.ui.theme.MidnightBlue
import com.mhq.fynecast.ui.theme.NeonGreen

@Composable
fun SignupScreen(
    modifier: Modifier = Modifier,
    viewModel: SignupViewModel = viewModel()
) {

    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userPassword by viewModel.userPassword.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val arePasswordsVisible by viewModel.arePasswordsVisible.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val gradientColors = listOf(NeonGreen, NeonGreen)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MidnightBlue, DuskBlue, LilacBlue, BabyBlue)))
    ) {
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
        GlassyCard(
            modifier = Modifier.padding(36.dp)
        ) {
            OutlinedTextField(
                value = userName,
                onValueChange = { viewModel.onUsernameChanged(it) },
                label = { Text("Name") },
                placeholder = { Text("Enter Name") },
                isError = viewModel.validateUsername(userName),
                singleLine = true,
                visualTransformation = if (!arePasswordsVisible) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Name Icon",
                        tint = MidnightBlue
                    )
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NeonGreen,
                    focusedLabelColor = NeonGreen,
                    focusedBorderColor = MidnightBlue,
                    focusedContainerColor = LilacBlue,
                    unfocusedTextColor = NeonGreen,
                    unfocusedLabelColor = NeonGreen,
                    unfocusedBorderColor = Color.DarkGray,
                    unfocusedContainerColor = LilacBlue,
                )
            )
            OutlinedTextField(
                value = userEmail,
                onValueChange = { viewModel.onUserEmailChanged(it) },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Icon",
                        tint = MidnightBlue
                    )
                },
                placeholder = { Text("Enter Email") },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NeonGreen,
                    focusedLabelColor = NeonGreen,
                    focusedBorderColor = MidnightBlue,
                    focusedContainerColor = LilacBlue,
                    unfocusedTextColor = NeonGreen,
                    unfocusedLabelColor = NeonGreen,
                    unfocusedBorderColor = Color.DarkGray,
                    unfocusedContainerColor = LilacBlue,
                ),
                singleLine = true
            )
            OutlinedTextField(
                value = userPassword,
                onValueChange = { viewModel.onUserPasswordChanged(it) },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password Icon",
                        tint = MidnightBlue
                    )
                },
                trailingIcon = {
                    val image =
                        if (arePasswordsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    val description = if (arePasswordsVisible) "Hide password" else "Show password"

                    IconButton(
                        onClick = { viewModel.togglePasswordsVisibility() }
                    ) {
                        Icon(
                            imageVector = image,
                            contentDescription = description,
                            tint = MidnightBlue
                        )
                    }
                },
                visualTransformation = if (arePasswordsVisible) VisualTransformation.None else PasswordVisualTransformation(),
                placeholder = { Text("Enter Password") },
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NeonGreen,
                    focusedLabelColor = NeonGreen,
                    focusedBorderColor = MidnightBlue,
                    focusedContainerColor = LilacBlue,
                    unfocusedTextColor = NeonGreen,
                    unfocusedLabelColor = NeonGreen,
                    unfocusedBorderColor = Color.DarkGray,
                    unfocusedContainerColor = LilacBlue,
                ),
                singleLine = true
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                label = { Text("Confirm Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password Icon",
                        tint = MidnightBlue
                    )
                },
                trailingIcon = {
                    val image =
                        if (arePasswordsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    val description = if (arePasswordsVisible) "Hide password" else "Show password"

                    IconButton(
                        onClick = { viewModel.togglePasswordsVisibility() }
                    ) {
                        Icon(
                            imageVector = image,
                            contentDescription = description,
                            tint = MidnightBlue
                        )
                    }
                },
                visualTransformation = if (arePasswordsVisible) VisualTransformation.None else PasswordVisualTransformation(),
                placeholder = { Text("Confirm Password") },
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NeonGreen,
                    focusedLabelColor = NeonGreen,
                    focusedBorderColor = MidnightBlue,
                    focusedContainerColor = LilacBlue,
                    unfocusedTextColor = NeonGreen,
                    unfocusedLabelColor = NeonGreen,
                    unfocusedBorderColor = Color.DarkGray,
                    unfocusedContainerColor = LilacBlue,
                ),
                singleLine = true
            )
        }

        Button(
            onClick = {viewModel.validateUsername(userName)},
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MidnightBlue,
                contentColor = Color.White
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
                color = NeonGreen
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Or sign up with",
                fontWeight = FontWeight.Bold,
                color = MidnightBlue,
                modifier = Modifier.padding(16.dp)
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            SocialMediaButton(
                iconRes = R.drawable.ic_google_primary_light,
                contentDescription = "Google Login",
                onClick = { /* Handle Login */ }
            )
            SocialMediaButton(
                iconRes = R.drawable.ic_facebook_primary_light,
                contentDescription = "Facebook Login",
                onClick = { /* Handle Login */ }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            SocialMediaButton(
//                text = "Sign in with Google",
//                iconRes = R.drawable.ic_google_primary_light,
//                backgroundColor = Color.White,
//                contentColor = Color(0xFF1F1F1F),
//                borderColor = Color(0xFF747775),
//                onClick = { /* Handle Google Login */ }
//            )
//            SocialMediaButton(
//                text = "Sign in with Facebook",
//                iconRes = R.drawable.ic_facebook_primary_light,
//                backgroundColor = Color(0xFF1877F2),
//                contentColor = Color.White,
//                onClick = { /* Handle Facebook Login */ }
//            )
            Row() {
                Text(
                    text = "Already have an account?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidnightBlue,
                    modifier = Modifier
                        .padding(4.dp)
                )
                Text(
                    text = "Login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable { /* Handle click */ }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignupScreenScreenPreview() {
    FyneCastTheme() {
        SignupScreen()
    }
}