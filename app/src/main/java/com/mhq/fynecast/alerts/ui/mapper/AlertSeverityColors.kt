package com.mhq.fynecast.alerts.ui.mapper

import androidx.compose.ui.graphics.Color
import com.mhq.fynecast.alerts.presentation.model.AlertSeverity
import com.mhq.fynecast.core.ui.theme.AppleRed
import com.mhq.fynecast.core.ui.theme.EditorialRoyalBlue
import com.mhq.fynecast.core.ui.theme.Tangerine
import com.mhq.fynecast.core.ui.theme.VividAmber

fun AlertSeverity.toColor(): Color = when (this) {
    AlertSeverity.Extreme -> AppleRed
    AlertSeverity.Severe -> Tangerine
    AlertSeverity.Moderate -> VividAmber
    AlertSeverity.Other -> EditorialRoyalBlue
}