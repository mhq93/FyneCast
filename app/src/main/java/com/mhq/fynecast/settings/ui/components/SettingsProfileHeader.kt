package com.mhq.fynecast.settings.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

/** A compact profile card: photo, name, email and an "Edit profile" hint. The whole card is tappable. */
@Composable
fun SettingsProfileHeader(
    profileName: String,
    profileEmail: String,
    profileImageUri: String?,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageModel = profileImageUri ?: R.drawable.user
    val onBackground = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.onPrimary

    GlassyCard(
        cornerRadius = 24.dp,
        padding = 0.dp,
        containerAlpha = 0.06f,
        borderAlpha = 0.15f,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onEditProfileClick)
                .padding(18.dp)
        ) {
            AsyncImage(
                model = imageModel,
                placeholder = painterResource(R.drawable.user),
                contentDescription = stringResource(R.string.profile_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, accent.copy(alpha = 0.6f), CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profileName,
                    color = onBackground,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = profileEmail,
                    color = onBackground.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.edit_profile),
                    color = accent,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = onBackground.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun SettingsProfileHeaderPreview() {
    FyneCastTheme(isDarkMode = true) {
        SettingsProfileHeader(
            profileName = "Mohamed",
            profileEmail = "mohamed@example.com",
            profileImageUri = null,
            onEditProfileClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
