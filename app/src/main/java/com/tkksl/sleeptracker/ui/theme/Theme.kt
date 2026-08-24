package com.tkksl.sleeptracker.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


// ====================== 深色主题 ======================

private val DarkColorScheme = darkColorScheme(

    primary = Color(0xFFE0E0E0),

    background = Background,

    surface = CardBackground,

    surfaceVariant = SurfaceVariant,

    onBackground = TextWhite,

    onSurface = TextWhite,

    onSurfaceVariant = TextGray,

    outline = Outline,

    outlineVariant = Divider,

    error = ErrorRed
)


// ====================== 浅色主题 ======================

private val LightColorScheme = lightColorScheme(

    primary = LightPrimaryBlue,

    background = LightBackground,

    surface = LightCardBackground,

    surfaceVariant = LightSurfaceVariant,

    onBackground = LightTextMain,

    onSurface = LightTextMain,

    onSurfaceVariant = LightTextGray,

    outline = LightDivider,

    outlineVariant = LightDivider,

    error = LightErrorRed
)


@Composable
fun SleepTrackerTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {

    val colorScheme =
        if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        }

    androidx.compose.material3.MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}