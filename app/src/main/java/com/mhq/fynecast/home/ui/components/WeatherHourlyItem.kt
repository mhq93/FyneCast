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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.format.TemperatureFormatter
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun WeatherHourlyItem(
    header: String,
    weatherIconUrl: String,
    temperature: Double,
    isCurrentHour: Boolean,
    modifier: Modifier = Modifier
) {
    val capsuleBackground = if (isCurrentHour) {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
            )
        )
    }

    val borderStrokeColor = if (isCurrentHour) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    }

    val contentColor = if (isCurrentHour) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .width(68.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(capsuleBackground)
            .border(width = 1.dp, brush = Brush.verticalGradient(listOf(borderStrokeColor, borderStrokeColor.copy(alpha = 0.2f))), shape = RoundedCornerShape(24.dp))
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Text(
            text = header,
            color = contentColor.copy(alpha = if (isCurrentHour) 0.9f else 0.6f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isCurrentHour) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        )

        AsyncImage(
            model = "https:$weatherIconUrl",
            contentDescription = stringResource(R.string.hourly_condition),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .size(38.dp)
        )

        Text(
            text = TemperatureFormatter.format(temperature),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor
        )
    }
}


@Preview(showBackground = true)
@Composable
fun WeatherHourlyItemPreview() {
    FyneCastTheme() {
        WeatherHourlyItem(
            header = "12:00",
            weatherIconUrl = "",
            temperature = 10.0,
            isCurrentHour = true,
        )
    }
}