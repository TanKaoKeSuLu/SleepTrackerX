package com.tkksl.sleeptracker.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tkksl.sleeptracker.data.model.SleepRecord

/**
 * 和 SleepSummaryCard 完全同源配色，档位严格对齐
 */
private fun getAdviceCardDayColor(score: Int) = when {
    score >= 90 -> Color(0xFFE8F5E9)
    score >= 70 -> Color(0xFFE3F2FD)
    score >= 50 -> Color(0xFFFFF3E0)
    score >= 30 -> Color(0xFFFFE0B2)
    else -> Color(0xFFFFEBEE)
}

private fun getAdviceCardDayOnColor(score: Int) = when {
    score >= 90 -> Color(0xFF2E7D32)    // 优秀‑深绿
    score >= 70 -> Color(0xFF1976D2)    // 良好‑深蓝
    score >= 50 -> Color(0xFFF57C00)    // 一般‑橙黄
    score >= 30 -> Color(0xFFEF6C00)    // 较差‑橙红
    else -> Color(0xFFD32F2F)           // 很差‑深红
}

@Composable
fun SleepBriefAdviceCard(
    record: SleepRecord,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val totalSec = record.duration
    val noiseCount = record.noiseEventCount
    val score = record.sleepScore

    val contentText = when {
        score >=70 -> "本次休息状态稳定，夜间干扰较少，保持现有的作息习惯即可"
        score >=50 -> "一共检测到${noiseCount}次声响干扰，建议睡前关闭周边噪音源"
        totalSec < 60 -> "本次休息时长过短，仅${totalSec}秒，无法完成有效休整，建议保证充足休息时间"
        else -> "频繁的声响打断了睡眠过程，尽量营造安静、避光的卧室环境提升睡眠质量"
    }

    val (cardContainer, cardOnColor) = if (isDarkMode) {
        // 夜间：全部灰调，关闭业务彩色
        Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        // 日间：与上方SleepSummaryCard背景/强调色一一对应
        Pair(
            getAdviceCardDayColor(score),
            getAdviceCardDayOnColor(score)
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行：矢量图标 + 文字，横向Row对齐，和页面其他组件风格统一
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Insights,
                    contentDescription = null,
                    tint = cardOnColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = " 今晚睡眠总结",
                    color = cardOnColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = contentText,
                modifier = Modifier.padding(top = 8.dp),
                color = cardOnColor.copy(alpha = 0.85f)
            )
        }
    }
}
