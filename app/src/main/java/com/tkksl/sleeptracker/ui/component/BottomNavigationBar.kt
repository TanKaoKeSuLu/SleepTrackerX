package com.tkksl.sleeptracker.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tkksl.sleeptracker.navigation.BottomNavItem
import com.tkksl.sleeptracker.viewmodel.SleepViewModelFactory
import com.tkksl.sleeptracker.viewmodel.ThemeManager
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun BottomNavigationBar(
    navController: NavHostController
) {
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route
    val context = LocalContext.current
    val themeVm: ThemeManager = viewModel(factory = SleepViewModelFactory(context))
    var isDark by remember { mutableStateOf(false) }

    LaunchedEffect(themeVm) {
        themeVm.isDarkMode.collectLatest { dark ->
            isDark = dark
        }
    }

    // 自动判断：深色浅灰，浅色主题用原生蓝色
    val selectedColor = if (isDark) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.primary

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.History,
        BottomNavItem.Settings
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        item.title,
                        color = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}