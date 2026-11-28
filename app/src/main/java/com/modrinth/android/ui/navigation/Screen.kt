package com.modrinth.android.ui.navigation

/**
 * 路由定义。
 *
 * 底部导航 3 个 tab（home / categories / mine）各自是独立的一级目的地，
 * tab 切换时清栈（popUpTo 到 startDestination 并 inclusive）；
 * project_detail / search 是普通 navigate 路由。
 */
sealed class Screen(val route: String, val label: String) {

    /** 首页 tab */
    object Home : Screen("home", "首页")

    /** 分类 tab */
    object Categories : Screen("categories", "分类")

    /** 我的 tab（本地账户 + 收藏） */
    object Mine : Screen("mine", "我的")

    /** 搜索页（顶部搜索框 / 分类 chip 进入，带可选的初始查询词） */
    object Search : Screen("search?query={query}", "搜索") {
        fun createRoute(query: String = "") =
            "search?query=${android.net.Uri.encode(query)}"
    }

    /** 项目详情（navArgument 传 projectId） */
    object ProjectDetail : Screen("project_detail/{projectId}", "详情") {
        const val ARG_PROJECT_ID = "projectId"
        fun createRoute(projectId: String) = "project_detail/$projectId"
    }
}
