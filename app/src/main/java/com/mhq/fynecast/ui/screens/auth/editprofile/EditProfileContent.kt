package com.mhq.fynecast.ui.screens.auth.editprofile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.ui.components.AuthActionButton
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
fun EditProfileContent(
    userName: String,
    userEmail: String,
    profileImageUri: String?,
    isUsernameValid: Boolean,
    isEmailValid: Boolean,
    isSaveEnabled: Boolean,
    onUsernameChanged: (String) -> Unit,
    onUserEmailChanged: (String) -> Unit,
    onProfileImagePicked: (String) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {

    val isLightThemeActive = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val dynamicBackgroundGradient = if (isLightThemeActive) {
        listOf(LightMiddayBlue, LightBabyBlue, LightIceBlue)
    } else {
        listOf(MidnightBlue, DuskBlue, LilacBlue, BabyBlue) // Deep Midnight spectrum
    }

    // In your EditProfileContent photo layout holder box:
    val imageModel = profileImageUri ?: R.drawable.ic_google_primary_light

    // Inside your photoPickerLauncher callback:
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // Tells the ViewModel a new image file path was selected
            onProfileImagePicked(uri.toString())
        }
    }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(dynamicBackgroundGradient))
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                // Empty spacer matching balance layout geometry weights
                Spacer(modifier = Modifier.size(48.dp))
            }

            // 2. Interactive Profile Avatar Layer
            Box(
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .size(100.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = stringResource(R.string.profile_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            CircleShape
                        )
                )
                // Camera Action overlay circle trigger button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MidnightBlue)
                        .border(1.dp, NeonGreen, CircleShape)
                        .clickable {
                            // 2. FIX: Fires intent request pulling up the native phone image selection grid
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.change_photo),
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 3. User Credentials Configuration Group packed in a GlassyCard
            GlassyCard(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                // Name Input Field
                AuthInputField(
                    value = userName,
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
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.focusRequester(focusRequester)
                )
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
                // Email Input Field
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
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }
            Spacer(
                modifier = Modifier.height(24.dp)
            )
            // 4. Primary High-Contrast Save Action Button (Matches the style of the monitor alerts button)
            AuthActionButton(
                text = stringResource(R.string.save_changes),
                isSubmitEnabled = isSaveEnabled,
                onDoAction = { onSaveClick() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    EditProfileContent(
        userName = "JohnDoe",
        userEmail = "johndoe@gmail.com",
        isUsernameValid = true,
        isEmailValid = true,
        isSaveEnabled = true,
        onUsernameChanged = {},
        onUserEmailChanged = {},
        onBackClick = {},
        onSaveClick = {},
        contentPadding = PaddingValues(0.dp),
        profileImageUri = null,
        onProfileImagePicked = {}
    )
}