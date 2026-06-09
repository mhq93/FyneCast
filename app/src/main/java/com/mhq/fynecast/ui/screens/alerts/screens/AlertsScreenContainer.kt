package com.mhq.fynecast.ui.screens.alerts.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.ui.theme.BabyBlue
import com.mhq.fynecast.ui.theme.DuskBlue
import com.mhq.fynecast.ui.theme.LilacBlue
import com.mhq.fynecast.ui.theme.MidnightBlue
import com.mhq.fynecast.ui.theme.NeonGreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AlertsScreenContainer(
    viewModel: AlertsViewModel,
    onNavigateHome: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is AlertsUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MidnightBlue,
                                DuskBlue,
                                LilacBlue,
                                BabyBlue
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonGreen)
            }
        }
        is AlertsUiState.Empty -> {
            AlertsScreenBlank(
                title = stringResource(R.string.no_location_data_found),
                subtitle = stringResource(R.string.please_select_a_city_on_the_home_screen_first),
                onNavigateHome = onNavigateHome,
                contentPadding = contentPadding,
                modifier = modifier
            )
        }
        is AlertsUiState.NoAlerts -> {
            AlertsScreenBlank(
                title = stringResource(R.string.no_active_alerts_in_this_area),
                subtitle = stringResource(R.string.a_great_chance_to_enjoy_the_weather_outdoors),
                onNavigateHome = onNavigateHome,
                contentPadding = contentPadding,
                modifier = modifier
            )
        }
        is AlertsUiState.Success -> {
            AlertsScreen(
                alertsList = state.alerts,
                contentPadding = contentPadding,
                modifier = modifier
            )
        }
    }
}