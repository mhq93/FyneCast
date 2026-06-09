package com.mhq.fynecast.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.MidnightBlue

@Composable
fun AuthSocialOptions(
    onGoogleAuthenticate: () -> Unit,
    onFacebookAuthenticate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = stringResource(R.string.or_with),
            fontWeight = FontWeight.Bold,
            color = MidnightBlue,
            modifier = Modifier.padding(8.dp)
        )
        Row(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            AuthSocialMediaButton(
                iconRes = R.drawable.ic_google_primary_light,
                contentDescription = stringResource(R.string.google_login),
                onClick = { onGoogleAuthenticate() },
            )
            Spacer(modifier = Modifier.width(16.dp))
            AuthSocialMediaButton(
                iconRes = R.drawable.ic_facebook_primary_light,
                contentDescription = stringResource(R.string.facebook_login),
                onClick = { onFacebookAuthenticate() },
            )
        }
    }
}

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