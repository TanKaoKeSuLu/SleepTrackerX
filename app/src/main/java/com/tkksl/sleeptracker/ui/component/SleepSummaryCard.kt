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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tkksl.sleeptracker.data.model.SleepRecord
import com.tkksl.sleeptracker.utils.TimeFormatUtil

// 5档分数配色
private fun getScoreMainColor(score: Int): Color {
    return when {
        score >= 90 -> Color(0xFF2E7D32)    // 优秀-深绿
        score >= 70 -> Color(0xFF1976D2)    // 良好-深蓝
        score >= 50 -> Color(0xFFF57C00)    // 一般-橙黄
        score >= 30 -> Color(0xFFEF6C00)    // 较差-橙红
        else -> Color(0xFFD32F2F)           // 很差-深红
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

// 柔和版建议文案
private fun getSleepAdviceText(score: Int, noiseCount: Int): String {
    return when {
        score >= 90 -> "睡眠质量极佳，作息稳定，继续保持当前习惯👍"
        score >= 70 -> "睡眠状态良好，夜间干扰较少，作息比较规律"
        score >= 50 -> "睡眠质量一般，检测到${noiseCount}次声响，建议睡前减少环境噪音"
        score >= 30 -> "本次睡眠质量较差，声响较频繁，建议尽量保持卧室安静"
        else -> "本次睡眠质量不佳，休息时长不足，建议保证每天不少于7小时睡眠"
    }
}

@Composable
private fun SummaryStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelText: String,
    valueText: String,
    mainColor: Color
) {
    Column(
        modifier = Modifier
            .widthIn(min = 70.dp)
            .heightIn(min = 80.dp) // 固定最小高度，四个格子垂直对齐一致
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = icon,
            contentDescription = labelText,
            tint = mainColor,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = labelText,
            color = Color.Gray,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = valueText,
            color = mainColor,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SleepSummaryCard(
    record: SleepRecord,
    modifier: Modifier = Modifier
) {
    val score = record.sleepScore
    val mainColor = getScoreMainColor(score)
    val cardBg = getScoreLightBgColor(score)
    val adviceText = getSleepAdviceText(score, record.noiseEventCount)

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
    val durationShowText = if(totalSec < 60){
        "${totalSec}秒"
    }else{
        TimeFormatUtil.formatDurationText(totalSec)
    }
    val noiseCountStr = "${record.noiseEventCount}次"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp), // 整体内边距收紧
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp) // 整体间距缩小，卡片更紧致
        ) {
            Text(
                text = starStr,
                fontSize = MaterialTheme.typography.displayMedium.fontSize,
                fontWeight = FontWeight.Bold,
                color = mainColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                text = "$score 分",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = mainColor
            )
            Text(
                text = "睡眠质量：$qualityStr",
                style = MaterialTheme.typography.titleMedium,
                color = mainColor.copy(alpha = 0.8f)
            )

            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = mainColor.copy(alpha = 0.15f),
                thickness = 1.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatItem(icon = Icons.Filled.Bed, labelText = "睡眠时长", valueText = durationShowText, mainColor = mainColor)
                SummaryStatItem(icon = Icons.Filled.Nightlight, labelText = "入睡时间", valueText = sleepStartStr, mainColor = mainColor)
                SummaryStatItem(icon = Icons.Filled.WbSunny, labelText = "起床时间", valueText = sleepEndStr, mainColor = mainColor)
                SummaryStatItem(icon = Icons.Filled.GraphicEq, labelText = "异常声音", valueText = noiseCountStr, mainColor = mainColor)
            }

            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = mainColor.copy(alpha = 0.15f),
                thickness = 1.dp
            )

            Text(
                text = adviceText,
                color = Color.DarkGray,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}