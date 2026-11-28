@file:OptIn(ExperimentalMaterial3Api::class)

package com.modrinth.android.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.modrinth.android.ui.categories.CategoriesScreen
import com.modrinth.android.ui.home.HomeScreen
import com.modrinth.android.ui.mine.MineScreen
import com.modrinth.android.ui.project.ProjectDetailScreen
import com.modrinth.android.ui.search.SearchScreen

/**
 * 单一 NavController 导航图：
 * - 底部 3 tab（首页/分类/我的），tab 间切换清栈
 * - search（可带初始查询词）、project_detail（带 projectId）为二级页
 */
private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    TabItem(Screen.Home.route, "首页", Icons.Filled.Home),
    TabItem(Screen.Categories.route, "分类", Icons.Filled.Star),
    TabItem(Screen.Mine.route, "我的", Icons.Filled.Person)
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        // 三个 tab 互相切换时清掉其它 tab 的栈（start 用 home）
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                    onCategoryClick = { category ->
                        navController.navigate(Screen.Search.createRoute(category))
                    },
                    onProjectClick = { id ->
                        navController.navigate(Screen.ProjectDetail.createRoute(id))
                    }
                )
            }
            composable(Screen.Categories.route) {
                CategoriesScreen(
                    onCategoryClick = { category ->
                        navController.navigate(Screen.Search.createRoute(category))
                    }
                )
            }
            composable(Screen.Mine.route) {
                MineScreen()
            }
            composable(
                route = Screen.Search.route,
                arguments = listOf(
                    navArgument("query") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entry ->
                val initialQuery = entry.arguments?.getString("query").orEmpty()
                SearchScreen(
                    initialQuery = initialQuery,
                    onBack = { navController.popBackStack() },
                    onProjectClick = { id ->
                        navController.navigate(Screen.ProjectDetail.createRoute(id))
                    }
                )
            }
            composable(
                route = Screen.ProjectDetail.route,
                arguments = listOf(
                    navArgument(Screen.ProjectDetail.ARG_PROJECT_ID) {
                        type = NavType.StringType
                    }
                )
            ) {
                ProjectDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
