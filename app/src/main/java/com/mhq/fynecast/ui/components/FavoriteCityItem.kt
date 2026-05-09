package com.mhq.fynecast.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.NeonGreen

@Composable
fun FavoriteCityItem(modifier: Modifier = Modifier){
    GlassyCard(modifier.padding(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                text = "Favorite City",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = NeonGreen,
                modifier = Modifier.padding(end = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ){
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = Color.Yellow,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(24.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.padding(start = 8.dp)
                ){
                    Text(
                        text = "H: 20°",
                        color = NeonGreen.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(
                        modifier = Modifier
                        .padding(4.dp)
                    )
                    Text(
                        text = "L: 10°",
                        color = NeonGreen.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
//                    Text(
//                        text = "H 20C°",
//                        fontWeight = FontWeight.Bold,
//                        style = MaterialTheme.typography.labelSmall,
//                        color = NeonGreen
//                    )
//                    Text(
//                        text = "L 10C°",
//                        fontWeight = FontWeight.Bold,
//                        style = MaterialTheme.typography.labelSmall,
//                        color = NeonGreen
//                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun FavoriteCityItemPreview(){
    FyneCastTheme() {
        FavoriteCityItem()
    }
}