package com.mhq.fynecast.ui.screens.favorites.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.data.database.FavoriteEntity
import com.mhq.fynecast.ui.components.GlassyCard
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.NeonGreen

@Composable
fun FavoriteCityItem(
    favoriteCity: FavoriteEntity,
    modifier: Modifier = Modifier
) {
    GlassyCard(modifier.padding(horizontal = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                text = "${favoriteCity.cityName}, ${favoriteCity.countryName}",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = NeonGreen,
                modifier = Modifier.padding(end = 8.dp)
            )
            Spacer(
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = "https:${favoriteCity.favCityWeatherIcon}",
                        placeholder = painterResource(R.drawable.ic_google_primary_light),
                        contentDescription = stringResource(R.string.weather_icon),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(64.dp)
                    )
                    Text(
                        text = "${favoriteCity.favCityTemp}°",
                        color = NeonGreen.copy(alpha = 0.8f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun FavoriteCityItemPreview() {
    FyneCastTheme() {
        FavoriteCityItem(
            favoriteCity = FavoriteEntity(
                cityName = "Alexandria",
                countryName = "Egypt",
                latitude = 10.0,
                longitude = 10.0,
                favCityTemp = 25.0,
                favCityWeatherIcon = "Sunny"
            )
        )
    }
}