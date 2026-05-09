package com.mhq.fynecast.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.R
import com.mhq.fynecast.ui.theme.FyneCastTheme

//@Composable
//fun HeroWeatherCard(
//    modifier: Modifier = Modifier,
//    city: String = "Barcelona",
//    description: String = "Sunny",
//    temperature: Int = 20,
//    high: Int = 30,
//    low: Int = 10,
//    iconCode: Int = R.drawable.sunny
//) {
//    Row(
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.Top,
//        modifier = Modifier
//            .background(LilacBlue)
//            .padding(24.dp)
//            .fillMaxWidth(),
//        ) {
//        Column {
//            Text(
//                text = city,
//                color = Color.White,
//                fontSize = 24.sp,
//                fontWeight = FontWeight.Bold
//            )
//            Text(
//                text = description,
//                color = Color.White.copy(alpha = 0.8f),
//                fontSize = 16.sp
//            )
//        }
//        Image(
//            painter = painterResource(id = R.drawable.sunny),
//            contentDescription = description,
//            modifier = Modifier.size(128.dp)
//        )
//    }
//    Row(
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.spacedBy(12.dp),
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        Text(
//            text = "${temperature}°",
//            color = Color.White,
//            fontSize = 72.sp,
//            fontWeight = FontWeight.Bold
//        )
//        Text(
//            text = "H:${high}° L:${low}°",
//            color = Color.White.copy(alpha = 0.8f),
//            fontSize = 14.sp
//        )
//    }
//}

@Composable
fun HeroWeatherCard(
    modifier: Modifier = Modifier,
    city: String = "Barcelona",
    description: String = "Sunny",
    temperature: Int = 30,
    high: Int = 20,
    low: Int = 10,
    iconCode: Int = R.drawable.sunny
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
            ) {
            Column {
                Text(
                    text = city,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    )
            }
            Image(
                painter = painterResource(id = iconCode),
                contentDescription = description,
                modifier = Modifier
                    .size(64.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(start = 16.dp)
        ) {
            Text(
                text = "${temperature}°",
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold
            )
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "H: ${high}°",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp
                )
                Text(
                    text = "L: ${low}°",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp
                )
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun HeroWeatherCardPreview(){
    FyneCastTheme() {
        HeroWeatherCard()
    }
}