package com.modrinth.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.navigation.navHostController
import com.modrinth.android.ui.navigation.AppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = navHostController()
            AppNavGraph(navController = navController)
        }
    }
}
