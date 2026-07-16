package com.tkksl.sleeptracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.tkksl.sleeptracker.navigation.AppRoot
import com.tkksl.sleeptracker.ui.theme.SleepTrackerTheme
import com.tkksl.sleeptracker.viewmodel.SleepViewModelFactory
import com.tkksl.sleeptracker.viewmodel.ThemeManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.collectLatest

// 全局共享ThemeManager容器
val LocalThemeManager = staticCompositionLocalOf<ThemeManager> {
    error("ThemeManager未初始化，请在MainActivity顶层提供")
}

class MainActivity : ComponentActivity() {
    // 麦克风权限申请启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 用户授予麦克风权限，后续可正常录音
        } else {
            // 用户拒绝麦克风权限，录音功能禁用
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 启动时检测麦克风权限，无权限则弹出申请框
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            val context = LocalContext.current
            val globalThemeVm: ThemeManager = viewModel(factory = SleepViewModelFactory(context))
            var isDark by remember { mutableStateOf(true) }

            LaunchedEffect(globalThemeVm) {
                globalThemeVm.isDarkMode.collectLatest { newDark ->
                    isDark = newDark
                }
            }

            SleepTrackerTheme(darkTheme = isDark) {
                CompositionLocalProvider(LocalThemeManager provides globalThemeVm) {
                    val navController = rememberNavController()
                    AppRoot(navController = navController)
                }
            }
        }
    }
}