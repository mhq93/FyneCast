package com.mhq.fynecast.ui.screens.auth.forgotpassword.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.R
import com.mhq.fynecast.ui.components.AppName
import com.mhq.fynecast.ui.components.AuthActionButton
import com.mhq.fynecast.ui.components.AuthFooterHyperlink
import com.mhq.fynecast.ui.components.AuthInputField
import com.mhq.fynecast.ui.components.GlassyCard
import com.mhq.fynecast.ui.theme.BabyBlue
import com.mhq.fynecast.ui.theme.DuskBlue
import com.mhq.fynecast.ui.theme.LightBabyBlue
import com.mhq.fynecast.ui.theme.LightIceBlue
import com.mhq.fynecast.ui.theme.LightMiddayBlue
import com.mhq.fynecast.ui.theme.LilacBlue
import com.mhq.fynecast.ui.theme.MidnightBlue
import com.mhq.fynecast.ui.theme.NeonGreen

@Composable
fun ForgotPasswordContent(
    email: String,
    isEmailValid: Boolean,
    isSubmitEnabled: Boolean,
    onEmailChanged: (String) -> Unit,
    onBackToLoginClick: () -> Unit,
    onSendLink: () -> Unit,
    modifier: Modifier = Modifier
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
            Text(
                text = stringResource(R.string.recover_password),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.enter_the_email_associated_with_your_account),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.we_will_send_a_link_to_reset_your_password),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

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
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.focusRequester(focusRequester)
            )
        }

        AuthActionButton(
            text = stringResource(R.string.send_link),
            isSubmitEnabled = isSubmitEnabled,
            onDoAction = { onSendLink() },
        )

        AuthFooterHyperlink(
            question = stringResource(R.string.remembered_credentials),
            answer = stringResource(R.string.login),
            onDoAction = { onBackToLoginClick() }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordContentPreview() {
    ForgotPasswordContent(
        email = "developer@fynecast.com",
        isEmailValid = true,
        isSubmitEnabled = true,
        onEmailChanged = {},
        onSendLink = {},
        onBackToLoginClick = {}
    )
}