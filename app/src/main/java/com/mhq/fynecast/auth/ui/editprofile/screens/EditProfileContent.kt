package com.mhq.fynecast.auth.ui.editprofile.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.ui.editprofile.components.EditProfileForm
import com.mhq.fynecast.auth.ui.editprofile.components.EditProfileHeader
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun EditProfileContent(
    username: String,
    isUsernameValid: Boolean,
    onUsernameChanged: (String) -> Unit,
    userEmail: String,
    isEmailValid: Boolean,
    onUserEmailChanged: (String) -> Unit,
    profileImageUri: String?,
    showPasswordFields: Boolean = false,
    newPassword: String = "",
    isNewPasswordValid: Boolean = true,
    onNewPasswordChanged: (String) -> Unit = {},
    confirmPassword: String = "",
    doPasswordsMatch: Boolean = true,
    onConfirmPasswordChanged: (String) -> Unit = {},
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: () -> Unit = {},
    onProfileImagePicked: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val usernameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        usernameFocusRequester.requestFocus()
    }

    // The keyboard is subtracted before measuring, so the page height follows it.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        val bottomInset = contentPadding.calculateBottomPadding()
        // The height the page can use. When the content is shorter, the photo and form card sit
        // centred on the page under the top bar; when it is taller, the page scrolls.
        val pageHeight = (maxHeight - bottomInset - 24.dp).coerceAtLeast(0.dp)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = bottomInset + 16.dp
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = pageHeight)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // The whole 44 dp square is the tap target; the visible circle is smaller.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = onBackClick)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(onBackground.copy(alpha = 0.06f))
                                .border(1.dp, onBackground.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = onBackground,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.edit_profile),
                        color = onBackground,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.size(44.dp))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    EditProfileHeader(
                        profileImageUri = profileImageUri,
                        onProfileImagePicked = onProfileImagePicked
                    )
                    EditProfileForm(
                        username = username,
                        userEmail = userEmail,
                        isUsernameValid = isUsernameValid,
                        isEmailValid = isEmailValid,
                        onUsernameChanged = onUsernameChanged,
                        onUserEmailChanged = onUserEmailChanged,
                        showPasswordFields = showPasswordFields,
                        newPassword = newPassword,
                        isNewPasswordValid = isNewPasswordValid,
                        onNewPasswordChanged = onNewPasswordChanged,
                        confirmPassword = confirmPassword,
                        doPasswordsMatch = doPasswordsMatch,
                        onConfirmPasswordChanged = onConfirmPasswordChanged,
                        isPasswordVisible = isPasswordVisible,
                        onTogglePasswordVisibility = onTogglePasswordVisibility,
                        onSaveClick = onSaveClick,
                        usernameFocusRequester = usernameFocusRequester
                    )
                }

                // As tall as the top bar, so the photo and card end up centred on the page.
                Spacer(modifier = Modifier.height(44.dp))
            }
        }
    }
}

@Composable
private fun EditProfilePreviewContent(isDarkMode: Boolean) {
    FyneCastTheme(isDarkMode = isDarkMode) {
        EditProfileContent(
            username = "Mohamed",
            isUsernameValid = true,
            onUsernameChanged = {},
            userEmail = "mohamed@example.com",
            isEmailValid = true,
            onUserEmailChanged = {},
            profileImageUri = null,
            onProfileImagePicked = {},
            onSaveClick = {},
            onBackClick = {},
            contentPadding = PaddingValues(0.dp)
        )
    }
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE, heightDp = 800)
@Composable
private fun EditProfileLightPreview() = EditProfilePreviewContent(isDarkMode = false)

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A,
    heightDp = 800
)
@Composable
private fun EditProfileDarkPreview() = EditProfilePreviewContent(isDarkMode = true)
