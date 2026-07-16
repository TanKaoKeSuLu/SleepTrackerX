package com.tkksl.sleeptracker.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object History : Screen("history")
    object Settings : Screen("settings")

    object Detail : Screen("detail/{recordId}") {
        fun createRoute(recordId: Long) = "detail/$recordId"
    }
}