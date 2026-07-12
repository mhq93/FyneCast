package com.mhq.fynecast.auth.ui.editprofile.ui.components

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.AbyssalVoid
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun EditProfileHeader(
    profileImageUri: String?,
    onProfileImagePicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    val imageModel = profileImageUri ?: R.drawable.user

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onProfileImagePicked(uri.toString())
        }
    }

    Box(
        modifier = Modifier
            .padding(top = 24.dp, bottom = 36.dp)
            .size(100.dp),
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
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AbyssalVoid)
                .border(1.dp, ElectricLimeGreen, CircleShape)
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = stringResource(R.string.change_photo),
                tint = ElectricLimeGreen,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun EditProfileHeaderPreview() {
    FyneCastTheme() {
        EditProfileHeader(
            profileImageUri = null,
            onProfileImagePicked = {},
        )
    }
}