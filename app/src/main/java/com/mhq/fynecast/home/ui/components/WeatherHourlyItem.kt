package com.mhq.fynecast.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.format.TemperatureFormatter
import com.mhq.fynecast.core.ui.globalcomponents.unlessArabic
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

/** One compact hour chip. The current hour gets the accent colour on its label and border. */
@Composable
fun WeatherHourlyItem(
    header: String,
    weatherIconUrl: String,
    temperature: Double,
    isCurrentHour: Boolean,
    modifier: Modifier = Modifier
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.onPrimary
    val shape = RoundedCornerShape(20.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .width(48.dp)
            .clip(shape)
            .background(
                if (isCurrentHour) accent.copy(alpha = 0.10f) else onBackground.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = if (isCurrentHour) accent.copy(alpha = 0.55f) else onBackground.copy(alpha = 0.08f),
                shape = shape
            )
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Text(
            text = header,
            color = if (isCurrentHour) accent else onBackground.copy(alpha = 0.55f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isCurrentHour) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.3.sp.unlessArabic()
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        AsyncImage(
            model = "https:$weatherIconUrl",
            contentDescription = stringResource(R.string.hourly_condition),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(vertical = 2.dp)
                .size(30.dp)
        )

        Text(
            text = TemperatureFormatter.format(temperature),
            color = onBackground,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun WeatherHourlyItemPreview() {
    FyneCastTheme(isDarkMode = true) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeatherHourlyItem("Now", "", 26.0, isCurrentHour = true)
            WeatherHourlyItem("4 PM", "", 25.0, isCurrentHour = false)
        }
    }
}
