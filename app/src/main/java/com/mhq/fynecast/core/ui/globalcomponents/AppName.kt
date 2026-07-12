package com.mhq.fynecast.core.ui.globalcomponents

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AppName(
    text: String,
    fontSize: TextUnit = 48.sp,
    fontFamily: FontFamily = FontFamily.Cursive,
    fontWeight: FontWeight = FontWeight.Bold,
    strokeWidth: Float = 5f,
    modifier: Modifier = Modifier,
) {

    Box(
        modifier = modifier
            .padding(bottom = 48.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = MaterialTheme.colorScheme.outline,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                drawStyle = Stroke(width = strokeWidth)
            )
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = fontFamily
            )
        )
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

@Preview(showBackground = true)
@Composable
fun AppNamePreview() {
    FyneCastTheme() {
        AppName(
            text = "FyneCast"
        )
    }
}