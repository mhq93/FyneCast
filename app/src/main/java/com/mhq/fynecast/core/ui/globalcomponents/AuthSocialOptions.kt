package com.mhq.fynecast.core.ui.globalcomponents

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AuthSocialOptions(
    onGoogleAuthenticate: () -> Unit,
    onFacebookAuthenticate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 24.dp)
    ){
        Text(
            text = stringResource(R.string.or_with),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row {
            AuthSocialMediaButton(
                iconRes = R.drawable.ic_google_primary_light,
                contentDescription = stringResource(R.string.google_login),
                onClick = { onGoogleAuthenticate() },
                modifier = modifier.padding(end = 8.dp)
            )
            AuthSocialMediaButton(
                iconRes = R.drawable.ic_facebook_primary_light,
                contentDescription = stringResource(R.string.facebook_login),
                onClick = { onFacebookAuthenticate() },
                modifier = modifier.padding(start = 8.dp)
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
private fun AuthSocialOptionsPreview() {
    FyneCastTheme() {
        AuthSocialOptions(
            onGoogleAuthenticate = {},
            onFacebookAuthenticate = {}
        )
    }
}