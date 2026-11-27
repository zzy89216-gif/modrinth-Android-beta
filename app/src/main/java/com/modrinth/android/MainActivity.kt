package com.modrinth.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.navigation.NavHostController
import androidx.compose.navigation.rememberNavHostController
import com.modrinth.android.ui.navigation.Screen
import com.modrinth.android.ui.home.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavHostController()
            
            // TODO: 完善 NavGraph
            HomeScreen(
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) }
            )
        }
    }
}
