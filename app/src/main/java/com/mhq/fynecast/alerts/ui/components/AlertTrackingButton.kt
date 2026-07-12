package com.mhq.fynecast.alerts.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AlertTrackingButton(
    shape: RoundedCornerShape,
    isTracked: Boolean,
    onTrackToggle: () -> Unit,
    modifier: Modifier = Modifier
) {

    val containerColor =
        if (isTracked)
            MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
        else
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)

    val contentColor =
        if (isTracked)
            MaterialTheme.colorScheme.error
        else
            MaterialTheme.colorScheme.onPrimary

    val borderColor =
        if (isTracked)
            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable { onTrackToggle() }
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Icon(
            imageVector =
                if (isTracked)
                    Icons.Default.NotificationsOff
                else
                    Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = contentColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text =
                if (isTracked)
                    stringResource(R.string.cancel_tracking)
                else
                    stringResource(R.string.track_this_alert),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
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
private fun AlertTrackingButtonPreview() {
    FyneCastTheme() {
        AlertTrackingButton(
            isTracked = true,
            onTrackToggle = {},
            shape = RoundedCornerShape(16.dp)
        )
    }
}