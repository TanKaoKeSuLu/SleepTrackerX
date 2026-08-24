package com.tkksl.sleeptracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tkksl.sleeptracker.ui.screen.HomeScreen
import com.tkksl.sleeptracker.ui.screen.HistoryScreen
import com.tkksl.sleeptracker.ui.screen.SettingsScreen
import com.tkksl.sleeptracker.ui.screen.SleepDetailScreen
import com.tkksl.sleeptracker.viewmodel.SleepViewModel
import com.tkksl.sleeptracker.viewmodel.SleepViewModelFactory

@Composable
fun AppNavHost(
    navController: NavHostController,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    // 全局唯一共享ViewModel
    val appContext = LocalContext.current.applicationContext
    val sharedSleepVm: SleepViewModel = viewModel(factory = SleepViewModelFactory(appContext))

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                viewModel = sharedSleepVm,
                isDarkTheme = isDarkTheme
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                navController = navController,
                viewModel = sharedSleepVm
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(sleepVm = sharedSleepVm)
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("recordId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
            SleepDetailScreen(
                recordId = recordId,
                viewModel = sharedSleepVm,
                isDarkTheme = isDarkTheme,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
