package com.mhq.fynecast.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.MidnightBlue
import com.mhq.fynecast.ui.theme.NeonGreen

@Composable
fun AuthActionButton(
    text: String,
    isSubmitEnabled: Boolean,
    onDoAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onDoAction,
        enabled = isSubmitEnabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MidnightBlue,
            contentColor = Color.White,
            disabledContainerColor = MidnightBlue.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .padding(horizontal = 36.dp, vertical = 16.dp)
            .height(56.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSubmitEnabled) NeonGreen else Color.Gray
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthActionButtonPreview() {
    FyneCastTheme() {
        AuthActionButton(
            text = "Sign Up",
            isSubmitEnabled = true,
            onDoAction = {}
        )
    }
}