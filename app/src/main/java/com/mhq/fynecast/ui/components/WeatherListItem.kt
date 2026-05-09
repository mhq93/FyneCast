package com.mhq.fynecast.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.ui.theme.FyneCastTheme

@Composable
fun WeatherItem(
    header: String = "12:00",
    weatherIcon: Int = R.drawable.sunny,
    highTemperature: Int = 20,
    lowTemperature: Int = 10,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = header,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 4.dp)
        )
        Image(
            painter = painterResource(weatherIcon),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = "H: ${highTemperature} C°",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .padding(top = 4.dp)
        )
        Text(
            text = "L: ${lowTemperature} C°",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .padding(top = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WeatherItemPreview() {
    FyneCastTheme() {
        WeatherItem()
    }
}