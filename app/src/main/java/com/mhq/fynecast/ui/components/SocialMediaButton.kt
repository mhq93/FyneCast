package com.mhq.fynecast.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SocialMediaButton(
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(60.dp)
            .padding(8.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

//@Composable
//fun SocialMediaButton(
//    text: String,
//    iconRes: Int,
//    backgroundColor: Color,
//    contentColor: Color,
//    borderColor: Color = Color.Transparent,
//    onClick: () -> Unit
//) {
//    Surface(
//        onClick = onClick,
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(48.dp),
//        shape = RoundedCornerShape(8.dp),
//        color = backgroundColor,
//        contentColor = contentColor,
//        border = if (borderColor != Color.Transparent) BorderStroke(1.dp, borderColor) else null,
//        shadowElevation = 1.dp
//    ) {
//        Row(
//            modifier = Modifier.padding(horizontal = 12.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.Center
//        ) {
//            Image(
//                painter = painterResource(id = iconRes),
//                contentDescription = null,
//                modifier = Modifier.size(20.dp)
//            )
//            Spacer(modifier = Modifier.width(12.dp))
//            Text(
//                text = text,
//                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
//            )
//        }
//    }
//}

//@Composable
//fun SocialMediaButton(
//    iconRes: Int,
//    backgroundColor: Color = Color.White,
//    onClick: () -> Unit
//) {
//    Surface(
//        modifier = Modifier
//            .size(50.dp)
//            .clickable(onClick = onClick),
//        shape = CircleShape,
//        color = backgroundColor,
//        shadowElevation = 2.dp
//    ) {
//        Box(contentAlignment = Alignment.Center) {
//            Image(
//                painter = painterResource(id = iconRes),
//                contentDescription = null,
//                modifier = Modifier.size(24.dp)
//            )
//        }
//    }
//}
