package com.mhq.fynecast.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.ui.components.AddButton
import com.mhq.fynecast.ui.components.GlassyCard
import com.mhq.fynecast.ui.components.HeroWeatherCard
import com.mhq.fynecast.ui.components.WeatherItem
import com.mhq.fynecast.ui.theme.BabyBlue
import com.mhq.fynecast.ui.theme.DuskBlue
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.LilacBlue
import com.mhq.fynecast.ui.theme.MidnightBlue
import com.mhq.fynecast.ui.theme.NeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Brush.verticalGradient(listOf(MidnightBlue, DuskBlue, LilacBlue, BabyBlue)))
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
    ) {
        HeroWeatherCard()
        GlassyCard(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Hourly Forecast",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonGreen,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                    WeatherItem()
                }
            }
        }
        GlassyCard(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "7-Day Forecast",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonGreen,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WeatherItem(header = "Mon", modifier = Modifier.weight(1f))
                    WeatherItem(header = "Mon", modifier = Modifier.weight(1f))
                    WeatherItem(header = "Mon", modifier = Modifier.weight(1f))
                    WeatherItem(header = "Mon", modifier = Modifier.weight(1f))
                    WeatherItem(header = "Mon", modifier = Modifier.weight(1f))
                    WeatherItem(header = "Mon", modifier = Modifier.weight(1f))
                    WeatherItem(header = "Mon", modifier = Modifier.weight(1f))
                }
            }
        }
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    GlassyCard(
                        modifier = Modifier
                            .padding(8.dp)
                            .wrapContentHeight()
                    ) {
                        Column() {
                            Text(
                                text = "Humidity",
                                style = MaterialTheme.typography.headlineMedium,
                                color = NeonGreen
                            )
                            Text(
                                text = "Stats Details",
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Image(
                                painter = painterResource(id = R.drawable.sunny),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 8.dp)
                                    .size(64.dp)
                            )
                            Text(
                                text = "Some Stats",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    GlassyCard(
                        modifier = Modifier
                            .padding(8.dp)
                            .wrapContentHeight()
                    ) {
                        Column() {
                            Text(
                                text = "Wind Speed",
                                style = MaterialTheme.typography.headlineMedium,
                                color = NeonGreen
                            )
                            Text(
                                text = "Stats Details",
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Image(
                                painter = painterResource(id = R.drawable.sunny),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 8.dp)
                                    .size(64.dp)
                            )
                            Text(
                                text = "Some Stats",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    GlassyCard(
                        modifier = Modifier
                            .padding(8.dp)
                            .wrapContentHeight()
                    ) {
                        Column() {
                            Text(
                                text = "Pressure",
                                style = MaterialTheme.typography.headlineMedium,
                                color = NeonGreen
                            )
                            Text(
                                text = "Stats Details",
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Image(
                                painter = painterResource(id = R.drawable.sunny),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 8.dp)
                                    .size(64.dp)
                            )
                            Text(
                                text = "Some Stats",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    GlassyCard(
                        modifier = Modifier
                            .padding(8.dp)
                            .wrapContentHeight()
                    ) {
                        Column() {
                            Text(
                                text = "UV Index",
                                style = MaterialTheme.typography.headlineMedium,
                                color = NeonGreen
                            )
                            Text(
                                text = "Stats Details",
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Image(
                                painter = painterResource(id = R.drawable.sunny),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 8.dp)
                                    .size(64.dp)
                            )
                            Text(
                                text = "Some Stats",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    FyneCastTheme() {
        HomeScreen()
    }
}