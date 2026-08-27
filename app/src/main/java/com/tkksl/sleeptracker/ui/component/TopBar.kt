package com.tkksl.sleeptracker.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.util.Calendar

// 时间图标色彩常量
private object TimeIconPalette {
    val morningSun = Color(0xFFFFA500)      // 早上橙黄
    val noonSun = Color(0xFFFF4500)         // 中午橙红
    val afternoonSun = Color(0xFFFFD700)    // 下午浅黄
    val nightMoonBase = Color(0xFFFFF176)   // 夜晚月亮黄色
}

/**
 * 计算月相 0.0f新月 ~ 1.0f满月
 */
private fun calcMoonPhase(): Float {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)

    fun julianDay(y: Int, m: Int, d: Int): Double {
        var yy = y.toDouble()
        var mm = m.toDouble()
        val dd = d.toDouble()
        if (mm < 3) {
            yy -= 1.0
            mm += 12.0
        }
        val A = (yy / 100).toInt().toDouble()
        val B = 2 - A + (A / 4).toInt().toDouble()
        return (365.25 * (yy + 4716)).toInt().toDouble() +
                (30.6001 * (mm + 1)).toInt().toDouble() +
                dd + B - 1524.5
    }

    val jd = julianDay(year, month, day)
    val newMoonJd = 2451550.1
    val lunarCycle = 29.53058867
    val delta = jd - newMoonJd
    val phase = (delta % lunarCycle) / lunarCycle
    return phase.coerceIn(0.0, 1.0).toFloat()
}

/**
 * 根据小时获取太阳tint颜色
 */
private fun getSunTint(hour: Int): Color {
    return when (hour) {
        in 7..10 -> TimeIconPalette.morningSun
        in 11..13 -> TimeIconPalette.noonSun
        in 14..19 -> TimeIconPalette.afternoonSun
        else -> TimeIconPalette.morningSun
    }
}

@Composable
fun TopBar(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "Sleep Tracker",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        val hour = LocalTime.now().hour
        val (greetingText, greetingIcon, iconTint) = when {
            hour in 7..10 -> Triple("早上好", Icons.Filled.WbSunny, getSunTint(hour))
            hour in 11..13 -> Triple("中午好", Icons.Filled.WbSunny, getSunTint(hour))
            hour in 14..19 -> Triple("下午好", Icons.Filled.WbSunny, getSunTint(hour))
            else -> {
                val phase = calcMoonPhase()
                // 月相控制月亮透明度：新月暗，满月最亮
                val moonAlpha = 0.25f + phase * 0.75f
                val moonColor = TimeIconPalette.nightMoonBase.copy(alpha = moonAlpha)
                Triple("晚上好", Icons.Filled.Nightlight, moonColor)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = greetingIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconTint
            )
            Text(
                text = greetingText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "本地 · 离线 · 隐私保护",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
