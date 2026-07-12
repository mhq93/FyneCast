package com.mhq.fynecast.auth.ui.editprofile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.ui.editprofile.ui.components.EditProfileForm
import com.mhq.fynecast.auth.ui.editprofile.ui.components.EditProfileHeader
import com.mhq.fynecast.core.ui.globalcomponents.AuthActionButton

@Composable
fun EditProfileContent(
    username: String,
    isUsernameValid: Boolean,
    onUsernameChanged: (String) -> Unit,
    userEmail: String,
    isEmailValid: Boolean,
    onUserEmailChanged: (String) -> Unit,
    profileImageUri: String?,
    onProfileImagePicked: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Customized Top Action Navigation Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(R.string.edit_profile),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                    )
                Spacer(modifier = Modifier.size(48.dp))
            }
            EditProfileHeader(
                profileImageUri = profileImageUri,
                onProfileImagePicked = onProfileImagePicked,
                modifier = Modifier
            )
            EditProfileForm(
                username = username,
                userEmail = userEmail,
                isUsernameValid = isUsernameValid,
                isEmailValid = isEmailValid,
                onUsernameChanged = onUsernameChanged,
                onUserEmailChanged = onUserEmailChanged,
            )
            AuthActionButton(
                text = stringResource(R.string.save_changes),
                onDoAction = { onSaveClick() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    EditProfileContent(
        username = "JohnDoe",
        isUsernameValid = true,
        onUsernameChanged = {},
        userEmail = "johndoe@gmail.com",
        isEmailValid = true,
        onUserEmailChanged = {},
        profileImageUri = null,
        onProfileImagePicked = {},
        onSaveClick = {},
        onBackClick = {},
        contentPadding = PaddingValues(0.dp)
    )
}