package com.mhq.fynecast.auth.ui.editprofile.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

/** The profile photo with an accent ring and a round camera button to change it. */
@Composable
fun EditProfileHeader(
    profileImageUri: String?,
    onProfileImagePicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val imageModel = profileImageUri ?: R.drawable.user
    val accent = MaterialTheme.colorScheme.onPrimary

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onProfileImagePicked(uri.toString())
        }
    }

    Box(
        modifier = modifier
            .padding(vertical = 8.dp)
            .size(120.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        AsyncImage(
            model = imageModel,
            placeholder = painterResource(R.drawable.user),
            contentDescription = stringResource(R.string.profile_photo),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(2.dp, accent.copy(alpha = 0.7f), CircleShape)
        )
        // The whole 44 dp square is the tap target; the visible badge is smaller.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button) {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.change_photo),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun EditProfileHeaderPreview() {
    FyneCastTheme(isDarkMode = true) {
        EditProfileHeader(
            profileImageUri = null,
            onProfileImagePicked = {}
        )
    }
}
