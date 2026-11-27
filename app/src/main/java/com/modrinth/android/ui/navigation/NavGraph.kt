package com.modrinth.android.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.navHostController
import androidx.compose.runtime.Composable

import androidx.navigation.NavHost
import androidx.navigation.compose.composable
import com.modrinth.android.ui.categories.CategoriesScreen
import com.modrinth.android.ui.home.HomeScreen
import com.modrinth.android.ui.project.ProjectDetailScreen
import com.modrinth.android.ui.search.SearchScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen()
        }

        composable(
            route = Screen.ProjectDetail.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            ProjectDetailScreen(projectId = projectId)
        }

        composable(Screen.Categories.route) {
            CategoriesScreen()
        }
    }
}
