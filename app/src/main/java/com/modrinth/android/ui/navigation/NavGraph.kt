package com.modrinth.android.ui.navigation

import androidx.compose.navigation.NavHostController
import androidx.compose.navigation.navigate

fun NavHostController.setupNavGraph() {
    navigate(Screen.Home.route)
    navigate(Screen.Search.route)
    navigate(Screen.Categories.route)
}
