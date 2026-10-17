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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.home.domain.models.WeatherStatsDomainModel
import java.text.NumberFormat

@Composable
fun WeatherStatsGrid(
    stats: WeatherStatsDomainModel,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val numberFormat = remember(locale) {
        NumberFormat.getInstance(locale).apply { maximumFractionDigits = 1 }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.WaterDrop,
                    chipHeader = stringResource(R.string.humidity),
                    chipBody = numberFormat.format(stats.humidity.toDouble()),
                    chipSubtitle = stringResource(R.string.percent)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.Air,
                    chipHeader = stringResource(R.string.wind_speed),
                    chipBody = numberFormat.format(if (isMetric) stats.windKph else stats.windMph),
                    chipSubtitle = if (isMetric) stringResource(R.string.kph) else stringResource(R.string.mph)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.Compress,
                    chipHeader = stringResource(R.string.pressure),
                    chipBody = numberFormat.format(if (isMetric) stats.pressureMb else stats.pressureIn),
                    chipSubtitle = if (isMetric) stringResource(R.string.millibars) else stringResource(
                        R.string.inches
                    )
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                WeatherStatChip(
                    chipIcon = Icons.Default.WbSunny,
                    chipHeader = stringResource(R.string.uv_index),
                    chipBody = numberFormat.format(stats.uvIndex),
                    chipSubtitle = stringResource(R.string.unit)
                )
            }
        }
    }
}