package com.mhq.fynecast.ui.screens.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mhq.fynecast.ui.components.FavoriteCityItem
import com.mhq.fynecast.ui.theme.BabyBlue
import com.mhq.fynecast.ui.theme.DuskBlue
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.LilacBlue
import com.mhq.fynecast.ui.theme.MidnightBlue

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier
){

    val favoriteCities: List<FavoriteCityModel> = emptyList<FavoriteCityModel>()

    Column(
        modifier = Modifier
    ){
        LazyColumn(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(MidnightBlue, DuskBlue, LilacBlue, BabyBlue)))
                .padding(horizontal = 8.dp, vertical = 36.dp)
                .fillMaxSize(),
        ) {
            items(20){
                FavoriteCityItem()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FavoritesScreenPreview(){
    FyneCastTheme() {
        FavoritesScreen()
    }
}