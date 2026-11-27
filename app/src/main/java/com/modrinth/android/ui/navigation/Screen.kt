package com.modrinth.android.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ProjectDetail : Screen("project_detail/{projectId}") {
        fun createRoute(projectId: String) = "project_detail/$projectId"
    }
    object Search : Screen("search")
    object Categories : Screen("categories")
}
