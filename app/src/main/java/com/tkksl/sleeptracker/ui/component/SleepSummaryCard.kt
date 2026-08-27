package com.tkksl.sleeptracker.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tkksl.sleeptracker.data.model.SleepRecord
import com.tkksl.sleeptracker.utils.TimeFormatUtil

// 亮色模式 5档分数配色
private fun getScoreMainColor(score: Int): Color {
    return when {
        score >= 90 -> Color(0xFF2E7D32)    // 优秀-深绿
        score >= 70 -> Color(0xFF1976D2)    // 良好-深蓝
        score >= 50 -> Color(0xFFF6912E)    // 一般-橙黄
        score >= 30 -> Color(0xFFEF6C00)    // 较差-橙红
        else -> Color(0xFFD33E3E)           // 很差-深红
    }
}

private fun getScoreLightBgColor(score: Int): Color {
    return when {
        score >= 90 -> Color(0xFFE8F5E9)
        score >= 70 -> Color(0xFFE3F2FD)
        score >= 50 -> Color(0xFFFFF3E0)
        score >= 30 -> Color(0xFFFFE0B2)
        else -> Color(0xFFFFEBEE)
    }
}

@Composable
private fun SummaryStatItem(
    icon: ImageVector,
    labelText: String,
    valueText: String,
    isDarkMode: Boolean,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .widthIn(min = 70.dp)
            .heightIn(min = 80.dp)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = icon,
            contentDescription = labelText,
            tint = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else accentColor,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = labelText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = valueText,
            color = if (isDarkMode) MaterialTheme.colorScheme.onSurface else accentColor,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SleepSummaryCard(
    record: SleepRecord,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val score = record.sleepScore
    val originAccent = getScoreMainColor(score)
    val originBg = getScoreLightBgColor(score)

    val cardContainerColor = if (isDarkMode) {
        MaterialTheme.colorScheme.surface
    } else {
        originBg
    }
    val textHighlightColor = if (isDarkMode) {
        MaterialTheme.colorScheme.onSurface
    } else {
        originAccent
    }
    val dividerColor = if (isDarkMode) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        originAccent.copy(alpha = 0.15f)
    }

    val starStr = when {
        score >= 90 -> "★★★★★"
        score >= 70 -> "★★★★☆"
        score >= 50 -> "★★★☆☆"
        score >= 30 -> "★★☆☆☆"
        else -> "★☆☆☆☆"
    }

    val qualityStr = when {
        score >= 90 -> "优秀"
        score >= 70 -> "良好"
        score >= 50 -> "一般"
        score >= 30 -> "较差"
        else -> "很差"
    }

    val sleepStartStr = TimeFormatUtil.formatTimeStamp(record.startTime).split(" ")[1]
    val sleepEndStr = TimeFormatUtil.formatTimeStamp(record.endTime).split(" ")[1]
    val totalSec = record.duration
    val durationShowText = if (totalSec < 60) {
        "${totalSec}秒"
    } else {
        TimeFormatUtil.formatDurationText(totalSec)
    }
    val noiseCountStr = "${record.noiseEventCount}次"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = starStr,
                fontSize = MaterialTheme.typography.displayMedium.fontSize,
                fontWeight = FontWeight.Bold,
                color = textHighlightColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                text = "$score 分",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textHighlightColor
            )
            Text(
                text = "睡眠质量：$qualityStr",
                style = MaterialTheme.typography.titleMedium,
                color = textHighlightColor.copy(alpha = 0.8f)
            )

            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = dividerColor,
                thickness = 1.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatItem(icon = Icons.Filled.Bed, labelText = "睡眠时长", valueText = durationShowText, isDarkMode = isDarkMode, accentColor = originAccent)
                SummaryStatItem(icon = Icons.Filled.Nightlight, labelText = "入睡时间", valueText = sleepStartStr, isDarkMode = isDarkMode, accentColor = originAccent)
                SummaryStatItem(icon = Icons.Filled.WbSunny, labelText = "起床时间", valueText = sleepEndStr, isDarkMode = isDarkMode, accentColor = originAccent)
                SummaryStatItem(icon = Icons.Filled.GraphicEq, labelText = "异常声音", valueText = noiseCountStr, isDarkMode = isDarkMode, accentColor = originAccent)
            }

            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = dividerColor,
                thickness = 1.dp
            )
        }
    }
}
