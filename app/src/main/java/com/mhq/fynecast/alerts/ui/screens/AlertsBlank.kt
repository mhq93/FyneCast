package com.mhq.fynecast.alerts.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.BlankPagePlaceholder
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

/**
 * Empty state screen shown when there are no active weather alerts in the area.
 */
@Composable
fun AlertsBlank(
    contentPadding: PaddingValues = PaddingValues(8.dp),
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize()
    ) {
        BlankPagePlaceholder(
            blankSectionIcon = Icons.Default.Block,
            blankSectionIconDescription = null,
            blankSectionTitle = stringResource(R.string.no_active_alerts_in_this_area),
            blankSectionSubtitle = stringResource(R.string.a_great_chance_to_enjoy_the_weather_outdoors)
        )
    }
}