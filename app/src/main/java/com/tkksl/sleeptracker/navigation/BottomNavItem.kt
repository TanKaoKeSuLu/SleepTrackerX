package com.tkksl.sleeptracker.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {

    object Home : BottomNavItem(
        "home",
        "首页",
        Icons.Default.Home
    )

    object History : BottomNavItem(
        "history",
        "历史",
        Icons.Default.History
    )

    object Settings : BottomNavItem(
        "settings",
        "设置",
        Icons.Default.Settings
    )

}