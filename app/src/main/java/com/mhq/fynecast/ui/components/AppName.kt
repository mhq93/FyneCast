package com.mhq.fynecast.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.LightBabyBlue
import com.mhq.fynecast.ui.theme.LightIceBlue
import com.mhq.fynecast.ui.theme.LightMiddayBlue

@Composable
fun AppName(
    text: String,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    strokeWidth: Float,
    strokeColor: Color,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                color = strokeColor,
                drawStyle = Stroke(width = strokeWidth)
            )
        )
        Text(
            text = text,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                brush = Brush.verticalGradient(colors = gradientColors)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AppNamePreview() {
    FyneCastTheme() {
        AppName(
            text = "FyneCast",
            fontSize = 48.sp,
            fontFamily = FontFamily.Cursive,
            fontWeight = FontWeight.Bold,
            strokeWidth = 5f,
            strokeColor = Color.Black,
            gradientColors = listOf(LightMiddayBlue, LightBabyBlue, LightIceBlue),
        )
    }
}