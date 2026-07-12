package com.mhq.fynecast.settings.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun SettingsProfileHeader(
    profileName: String,
    profileEmail: String,
    profileImageUri: String?,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val imageModel = profileImageUri ?: R.drawable.user

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = 32.dp,
                bottom = 16.dp
            )
    ) {
        AsyncImage(
            model = imageModel,
            placeholder = painterResource(R.drawable.user),
            contentDescription = stringResource(R.string.profile_photo),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .border(
                    1.5.dp,
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    CircleShape
                )
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        Text(
            text = profileName,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = profileEmail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        Button(
            onClick = onEditProfileClick,
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 0.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.1f
                )
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.25f
                )
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = stringResource(R.string.edit_profile),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
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
private fun SettingsProfileHeaderPreview() {
    FyneCastTheme() {
        SettingsProfileHeader(
            profileName = "name",
            profileEmail = "email",
            profileImageUri = null,
            onEditProfileClick = {},
        )
    }
}