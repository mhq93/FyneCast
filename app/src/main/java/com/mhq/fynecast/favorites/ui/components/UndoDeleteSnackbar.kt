package com.mhq.fynecast.favorites.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun UndoDeleteSnackbar(
    snackbarData: SnackbarData?
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .padding(horizontal = 36.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            // Display message
            snackbarData?.visuals?.message?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Display "Undo" Action Button if present
            snackbarData?.visuals?.actionLabel?.let { label ->
                Text(
                    text = label.uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { snackbarData.performAction() }
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true, backgroundColor = 0xFFE0F2FE)
@Composable
fun UndoDeleteSnackbarLightPreview() {
    FyneCastTheme(isDarkMode = false) {
        UndoDeleteSnackbar(
            snackbarData = remember {
                object : SnackbarData {
                    override val visuals = object : SnackbarVisuals {
                        override val message = "Alexandria removed from favorites"
                        override val actionLabel = "Undo"
                        override val duration = SnackbarDuration.Short
                        override val withDismissAction = false
                    }
                    override fun dismiss() {}
                    override fun performAction() {}
                }
            }
        )
    }
}

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A
)
@Composable
fun UndoDeleteSnackbarDarkPreview() {
    FyneCastTheme(isDarkMode = true) {
        UndoDeleteSnackbar(
            snackbarData = remember {
                object : SnackbarData {
                    override val visuals = object : SnackbarVisuals {
                        override val message = "Alexandria removed from favorites"
                        override val actionLabel = "Undo"
                        override val duration = SnackbarDuration.Short
                        override val withDismissAction = false
                    }
                    override fun dismiss() {}
                    override fun performAction() {}
                }
            }
        )
    }
}
