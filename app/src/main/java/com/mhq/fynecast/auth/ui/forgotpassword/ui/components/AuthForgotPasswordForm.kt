package com.mhq.fynecast.auth.ui.forgotpassword.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.AuthInputField
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AuthForgotPasswordForm(
    email: String,
    isEmailValid: Boolean,
    onEmailChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    GlassyCard(
        modifier = modifier.padding(horizontal = 36.dp)
    ) {
        Text(
            text = stringResource(R.string.recover_password),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(
            modifier = Modifier.height(8.dp)
        )
        Text(
            text = stringResource(R.string.enter_the_email_associated_with_your_account),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.we_will_send_a_link_to_reset_your_password),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
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
private fun AuthForgotPasswordFormPreview() {
    FyneCastTheme() {
        AuthForgotPasswordForm(
            email = "",
            isEmailValid = true,
            onEmailChanged = {}
        )
    }
}