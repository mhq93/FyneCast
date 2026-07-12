package com.mhq.fynecast.auth.ui.editprofile.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
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
    modifier: Modifier = Modifier
) {

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    GlassyCard(
        modifier = Modifier.padding(horizontal = 36.dp)
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
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = {
                focusManager.moveFocus(
                    FocusDirection.Down
                )
            }),
            modifier = Modifier.focusRequester(focusRequester)
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
            }),
            modifier = Modifier.focusRequester(focusRequester)
        )
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