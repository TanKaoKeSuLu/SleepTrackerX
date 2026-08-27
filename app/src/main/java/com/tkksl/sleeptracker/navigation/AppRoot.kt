package com.tkksl.sleeptracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
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
    var darkMode by remember { mutableStateOf(true) }

    LaunchedEffect(themeVm) {
        themeVm.isDarkMode.collectLatest { newDark ->
            darkMode = newDark
        }
    }

    SleepTrackerTheme(darkTheme = darkMode) {
        AppNavHost(
            navController = navController,
            isDarkTheme = darkMode
        )
    }
}
