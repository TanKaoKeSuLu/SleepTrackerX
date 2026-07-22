package com.tkksl.sleeptracker.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tkksl.sleeptracker.data.model.SleepRecord

private fun getScoreMainColor(score: Int): Color {
    return when {
        score >= 90 -> Color(0xFF2E7D32)
        score >= 70 -> Color(0xFF1976D2)
        score >= 50 -> Color(0xFFF57C00)
        score >= 30 -> Color(0xFFEF6C00)
        else -> Color(0xFFD32F2F)
    }
}

@Composable
fun SleepBriefAdviceCard(record: SleepRecord, modifier: Modifier = Modifier) {
    val mainColor = getScoreMainColor(record.sleepScore)
    val totalSec = record.duration
    val noiseCount = record.noiseEventCount
    val contentText = when {
        // 高分
        record.sleepScore >=70 -> "本次休息状态稳定，夜间干扰较少，保持现有的作息习惯即可"
        // 分数一般
        record.sleepScore >=50 -> "一共检测到${noiseCount}次声响干扰，建议睡前关闭周边噪音源"
        // 低分短时睡眠
        totalSec < 60 -> "本次休息时长过短，仅${totalSec}秒，无法完成有效休整，建议保证充足休息时间"
        else -> "频繁的声响打断了睡眠过程，尽量营造安静、避光的卧室环境提升睡眠质量"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = mainColor.copy(alpha = 0.08f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "💡 今晚睡眠总结",
                color = mainColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = contentText,
                modifier = Modifier.padding(top = 6.dp),
                color = Color.DarkGray
            )
        }
    }
}