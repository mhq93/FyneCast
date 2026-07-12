package com.mhq.fynecast.favorites.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel

@Composable
fun FavoriteCityItem(
    favoriteCityDomainModel: FavoriteCityDomainModel,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    GlassyCard(
        modifier = modifier.padding(horizontal = 8.dp),
        containerAlpha = 0.12f,
        borderAlpha = 0.3f
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left — City info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = favoriteCityDomainModel.cityName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Cursive,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = favoriteCityDomainModel.countryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right — Weather icon + temp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AsyncImage(
                    model = "https:${favoriteCityDomainModel.weatherIconUrl}",
                    placeholder = painterResource(R.drawable.ic_google_primary_light),
                    contentDescription = stringResource(R.string.weather_icon),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text =
                        if (isMetric)
                            "${favoriteCityDomainModel.temperatureC}°"
                        else
                            "${favoriteCityDomainModel.temperatureF}°",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
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
private fun FavoriteCityItemPreview() {
    FyneCastTheme() {
        FavoriteCityItem(
            FavoriteCityDomainModel(
                cityName = "Alexandria",
                countryName = "Egypt",
                latitude = 100.0,
                longitude = 100.0,
                temperatureC = 100.0,
                temperatureF = 100.0,
                weatherIconUrl = "Url"
            ),
            isMetric = true
        )
    }
}