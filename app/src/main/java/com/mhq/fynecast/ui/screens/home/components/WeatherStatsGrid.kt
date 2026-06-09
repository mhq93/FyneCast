package com.mhq.fynecast.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.data.network.weather.WeatherApiResponse

@Composable
fun WeatherStatsGrid(
    responseData: WeatherApiResponse,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // ROW 1: Humidity and Wind Speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.WaterDrop,
                    chipHeader = stringResource(R.string.humidity),
                    chipBody = responseData.current.humidity.toString(),
                    chipSubtitle = " "
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.Air,
                    chipHeader = stringResource(R.string.wind_speed),
                    chipBody = (if (isMetric) responseData.current.windKph else responseData.current.windMph).toString(),
                    chipSubtitle = if (isMetric) stringResource(R.string.kph) else stringResource(R.string.mph)
                )
            }
        }

        // ROW 2: Barometric Pressure and UV Index Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.Compress,
                    chipHeader = stringResource(R.string.pressure),
                    chipBody = (if (isMetric) responseData.current.pressureMb else responseData.current.pressureIn).toString(),
                    chipSubtitle = if (isMetric) stringResource(R.string.millibars) else stringResource(R.string.inches)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.WbSunny,
                    chipHeader = stringResource(R.string.uv_index),
                    chipBody = responseData.current.uv.toString(),
                    chipSubtitle = " "
                )
            }
        }
    }
}