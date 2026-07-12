package com.mhq.fynecast.map.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.core.ui.theme.AbyssalVoid
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen
import com.mhq.fynecast.home.domain.models.CitySuggestionModel

@Composable
fun LocationConfirmCard(
    location: CitySuggestionModel,
    onLocationPicked: (CitySuggestionModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AbyssalVoid.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = location.fullName,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(
                modifier = Modifier.height(12.dp)
            )
            Button(
                onClick = { onLocationPicked(location) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricLimeGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.confirm_location),
                    color = AbyssalVoid
                )
            }
        }
    }
}

@Preview
@Composable
private fun LocationConfirmCardPreview() {
    FyneCastTheme(){
        LocationConfirmCard(
            location = CitySuggestionModel(
                fullName = "City Name",
                latitude = 100.0,
                longitude = 100.0
            ),
            onLocationPicked = {}
        )
    }
}