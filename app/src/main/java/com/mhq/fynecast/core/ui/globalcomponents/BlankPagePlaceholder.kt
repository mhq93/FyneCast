package com.mhq.fynecast.core.ui.globalcomponents

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun BlankPagePlaceholder(
    blankSectionIcon: ImageVector,
    blankSectionIconDescription: String?,
    blankSectionTitle: String,
    blankSectionSubtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.wrapContentSize()
    ) {
        Icon(
            imageVector = blankSectionIcon,
            contentDescription = blankSectionIconDescription,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            modifier = Modifier.size(120.dp)
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        Text(
            text = blankSectionTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = blankSectionSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(
            modifier = Modifier.height(24.dp)
        )
        Text(
            text = stringResource(R.string.tab_below_to_go_back_to_home_page),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
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
private fun BlankPagePlaceholderPreview() {
    FyneCastTheme() {
        BlankPagePlaceholder(
            blankSectionIcon = Icons.Default.AccountCircle,
            blankSectionIconDescription = null,
            blankSectionTitle = "blankSectionTitle",
            blankSectionSubtitle = "blankSectionSubtitle"
        )
    }
}