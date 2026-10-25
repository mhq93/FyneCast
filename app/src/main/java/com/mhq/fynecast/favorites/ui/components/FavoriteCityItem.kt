package com.mhq.fynecast.favorites.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.format.TemperatureFormatter
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.globalcomponents.unlessArabic
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.favorites.presentation.model.FavoriteCityPresentationModel

/** Corner radius shared with the swipe-to-delete background so the red layer matches the card. */
val FavoriteCardCornerRadius = 24.dp

/**
 * One favorite city, about 68 dp tall: icon bubble, city and country, big light temperature.
 * The tap is handled inside the card so the ripple follows its rounded corners.
 */
@Composable
fun FavoriteCityItem(
    favorite: FavoriteCityPresentationModel,
    isMetric: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onBackground = MaterialTheme.colorScheme.onBackground

    GlassyCard(
        cornerRadius = FavoriteCardCornerRadius,
        padding = 0.dp,
        containerAlpha = 0.06f,
        borderAlpha = 0.15f,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(onBackground.copy(alpha = 0.06f))
            ) {
                AsyncImage(
                    model = "https:${favorite.weatherIconUrl}",
                    placeholder = painterResource(R.drawable.ic_launcher_foreground),
                    error = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.weather_icon),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = favorite.cityName,
                    color = onBackground,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp.unlessArabic()
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = favorite.countryName.uppercase(),
                    color = onBackground.copy(alpha = 0.45f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp.unlessArabic()
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = TemperatureFormatter.format(
                    if (isMetric) favorite.temperatureC else favorite.temperatureF
                ),
                color = onBackground,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.W300,
                maxLines = 1
            )
        }
    }
}

private val previewFavorite = FavoriteCityPresentationModel(
    id = "31.2,29.92",
    cityName = "Alexandria",
    countryName = "Egypt",
    temperatureC = 24.0,
    temperatureF = 75.2,
    weatherIconUrl = "Url"
)

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE)
@Composable
private fun FavoriteCityItemLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        FavoriteCityItem(
            favorite = previewFavorite,
            isMetric = true,
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
private fun FavoriteCityItemDarkPreview() {
    FyneCastTheme(isDarkMode = true) {
        FavoriteCityItem(
            favorite = previewFavorite,
            isMetric = true,
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
