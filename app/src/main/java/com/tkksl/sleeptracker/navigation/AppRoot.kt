package com.tkksl.sleeptracker.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.tkksl.sleeptracker.ui.component.BottomNavigationBar
import com.tkksl.sleeptracker.ui.theme.SleepTrackerTheme
import com.tkksl.sleeptracker.viewmodel.SleepViewModelFactory
import com.tkksl.sleeptracker.viewmodel.ThemeManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppRoot(navController: NavHostController) {
    val context = LocalContext.current
    val themeVm: ThemeManager = viewModel(factory = SleepViewModelFactory(context))
    // 核心：用remember包裹可变状态，配合collectLatest接收最新值
    var darkMode by remember { mutableStateOf(true) }

    // key绑定themeVm，vm实例不变时持续监听；collectLatest保证只取最新主题值
    LaunchedEffect(themeVm) {
        themeVm.isDarkMode.collectLatest { newDark ->
            darkMode = newDark
        }
    }

    // 外层SleepTrackerTheme会随darkMode变更自动重组，全局刷新配色
    SleepTrackerTheme(darkTheme = darkMode) {
        Scaffold(
            bottomBar = { BottomNavigationBar(navController = navController) }
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}