package com.mhq.fynecast.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

//@Composable
//fun GlassyCard(
//    modifier: Modifier = Modifier,
//    content: @Composable ColumnScope.() -> Unit
//) {
//    Card(
//        modifier = modifier,
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = Color.White.copy(alpha = 0.15f)
//        ),
//        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
//        elevation = cardElevation(0.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            content = content
//        )
//    }
//}

@Composable
fun GlassyCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    padding: Dp = 16.dp,
    containerAlpha: Float = 0.15f,
    borderAlpha: Float = 0.25f,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = containerAlpha)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = borderAlpha),
                    Color.White.copy(alpha = borderAlpha / 2)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(padding),
            content = content
        )
    }
}
