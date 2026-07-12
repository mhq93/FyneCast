package com.mhq.fynecast.core.ui.globalcomponents

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun AuthActionButton(
    text: String,
    onDoAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onDoAction,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
            .padding(start = 36.dp, top = 16.dp, end = 36.dp)
            .height(56.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthActionButtonPreview() {
    FyneCastTheme() {
        AuthActionButton(
            text = "Sign Up",
            onDoAction = {}
        )
    }
}