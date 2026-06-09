package com.mhq.fynecast.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.ui.theme.FyneCastTheme

@Composable
fun AuthSocialMediaButton(
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent, // 1. Set to transparent to remove artificial borders
        modifier = modifier
            .size(48.dp) // 2. standard material circle action target size
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop, // 3. Crops the brand asset file into a full circle
            modifier = Modifier.fillMaxSize() // 4. Fills the circular shape completely edge-to-edge
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuthSocialMediaButtonPreview(){
    FyneCastTheme() {
        AuthSocialMediaButton(
            iconRes = R.drawable.ic_google_primary_light,
            contentDescription = null,
            onClick = {}
        )
    }
}
