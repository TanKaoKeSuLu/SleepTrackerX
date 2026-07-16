package com.tkksl.sleeptracker.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// 深色主题配色
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    background = Background,
    surface = CardBackground,
    onBackground = TextWhite,
    onSurface = TextWhite,
    outline = Divider,
    error = ErrorRed
)

// 浅色主题配色
private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryBlue,
    background = LightBackground,
    surface = LightCardBackground,
    onBackground = LightTextMain,
    onSurface = LightTextMain,
    outline = LightDivider,
    error = LightErrorRed
)

@Composable
fun SleepTrackerTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    androidx.compose.material3.MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}