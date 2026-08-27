package com.tkksl.sleeptracker.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.material3.Scaffold
import com.tkksl.sleeptracker.ui.component.BottomNavigationBar
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
        // ========== Tab平级页面：内部包裹Scaffold，显示底部导航，无切换动画 ==========
        composable(
            Screen.Home.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            Scaffold(
                bottomBar = { BottomNavigationBar(navController = navController) }
            ) { innerPadding ->
                HomeScreen(
                    navController = navController,
                    viewModel = sharedSleepVm,
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        composable(
            Screen.History.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            Scaffold(
                bottomBar = { BottomNavigationBar(navController = navController) }
            ) { innerPadding ->
                HistoryScreen(
                    navController = navController,
                    viewModel = sharedSleepVm,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        composable(
            Screen.Settings.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            Scaffold(
                bottomBar = { BottomNavigationBar(navController = navController) }
            ) { innerPadding ->
                SettingsScreen(
                    sleepVm = sharedSleepVm,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // ========== 详情二级页面：无Scaffold，不渲染底部导航，保留滑入动画 ==========
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("recordId") {
                    type = NavType.LongType
                }
            ),
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(150))
            },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(150))
            }
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
