package com.mhq.fynecast.auth.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R

/** A hairline divider with a short label, then Google and Facebook side by side. */
@Composable
fun AuthSocialSection(
    onGoogleAuthenticate: () -> Unit,
    onFacebookAuthenticate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onBackground = MaterialTheme.colorScheme.onBackground

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = onBackground.copy(alpha = 0.12f)
            )
            Text(
                text = stringResource(R.string.or_with),
                color = onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = onBackground.copy(alpha = 0.12f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SocialButton(
                iconRes = R.drawable.ic_google_primary_light,
                contentDescription = stringResource(R.string.google_login),
                onClick = onGoogleAuthenticate,
                modifier = Modifier.weight(1f)
            )
            SocialButton(
                iconRes = R.drawable.ic_facebook_primary_light,
                contentDescription = stringResource(R.string.facebook_login),
                onClick = onFacebookAuthenticate,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SocialButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onBackground = MaterialTheme.colorScheme.onBackground

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = onBackground.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, onBackground.copy(alpha = 0.12f)),
        modifier = modifier.height(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
