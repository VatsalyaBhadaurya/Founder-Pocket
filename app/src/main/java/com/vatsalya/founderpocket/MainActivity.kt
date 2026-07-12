package com.vatsalya.founderpocket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.vatsalya.founderpocket.ui.navigation.NavGraph
import com.vatsalya.founderpocket.ui.theme.FounderPocketTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FounderPocketTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
