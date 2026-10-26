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

@Composable
fun AlertsUnavailable(
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
            blankSectionTitle = stringResource(R.string.alerts_unavailable),
            blankSectionSubtitle = stringResource(R.string.alerts_connect_to_the_internet)
        )
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F2FE
)
@Composable
fun AlertsUnavailableLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        AlertsUnavailable()
    }
}

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
fun AlertsUnavailableDarkPreview() {
    FyneCastTheme(isDarkMode = true) {
        AlertsUnavailable()
    }
}
