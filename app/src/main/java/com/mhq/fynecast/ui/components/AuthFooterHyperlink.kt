package com.mhq.fynecast.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.MidnightBlue
import com.mhq.fynecast.ui.theme.NeonGreen

@Composable
fun AuthFooterHyperlink(
    question: String,
    answer: String,
    onDoAction: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 36.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Text(
                text = question,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MidnightBlue,
                modifier = Modifier.padding(4.dp)
            )
            Text(
                text = answer,
                color = NeonGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(4.dp)
                    .clickable { onDoAction() }
            )
        }
    }
}

@Preview
@Composable
private fun AuthFooterHyperlinkPreview() {
    FyneCastTheme() {
        AuthFooterHyperlink(
            question = "Already have an account?",
            answer = "Login",
            onDoAction = {}
        )
    }
}