package com.mhq.fynecast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.mhq.fynecast.ui.FyneCastApp
import com.mhq.fynecast.ui.screens.home.HomeScreen
import com.mhq.fynecast.ui.screens.login.LoginScreen
import com.mhq.fynecast.ui.screens.signup.SignupScreen
import com.mhq.fynecast.ui.theme.FyneCastTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FyneCastTheme {
                FyneCastApp()
            }
        }
    }
}