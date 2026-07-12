package com.mhq.fynecast.home.ui.components

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
import com.mhq.fynecast.home.domain.models.WeatherStatsDomainModel

@Composable
fun WeatherStatsGrid(
    stats: WeatherStatsDomainModel,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.WaterDrop,
                    chipHeader = stringResource(R.string.humidity),
                    chipBody = stats.humidity.toString(),
                    chipSubtitle = stringResource(R.string.percent)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.Air,
                    chipHeader = stringResource(R.string.wind_speed),
                    chipBody = (if (isMetric) stats.windKph else stats.windMph).toString(),
                    chipSubtitle = if (isMetric) stringResource(R.string.kph) else stringResource(R.string.mph)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.Compress,
                    chipHeader = stringResource(R.string.pressure),
                    chipBody = (if (isMetric) stats.pressureMb else stats.pressureIn).toString(),
                    chipSubtitle = if (isMetric) stringResource(R.string.millibars) else stringResource(R.string.inches)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.WbSunny,
                    chipHeader = stringResource(R.string.uv_index),
                    chipBody = stats.uvIndex.toString(),
                    chipSubtitle = stringResource(R.string.unit)
                )
            }
        }
    }
}