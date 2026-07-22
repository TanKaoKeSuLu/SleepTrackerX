package com.tkksl.sleeptracker.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tkksl.sleeptracker.data.model.SleepRecord
import com.tkksl.sleeptracker.utils.TimeFormatUtil
import java.util.Locale

@Composable
private fun GridTwoItemRow(leftLabel: String, leftValue: String, rightLabel: String, rightValue: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GridSingleItem(modifier = Modifier.weight(1f), label = leftLabel, value = leftValue)
        GridSingleItem(modifier = Modifier.weight(1f), label = rightLabel, value = rightValue)
    }
}

@Composable
private fun GridSingleItem(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .background(Color(0xFFF5F7FA), RoundedCornerShape(8.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )
    }
}

@Composable
fun SleepInfoGrid(record: SleepRecord, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "睡眠详细数据",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA).copy(alpha = 0.6f))) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 分组1：睡眠基础
                Text(text = "睡眠基础", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                GridTwoItemRow(
                    leftLabel = "入睡时间", leftValue = TimeFormatUtil.formatTimeStamp(record.startTime),
                    rightLabel = "醒来时间", rightValue = TimeFormatUtil.formatTimeStamp(record.endTime)
                )

                // 分组2：录音参数
                Text(text = "录音参数", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                GridTwoItemRow(
                    leftLabel = "总时长", leftValue = if(record.duration <60) "${record.duration}秒" else TimeFormatUtil.formatDurationText(record.duration),
                    rightLabel = "采样率", rightValue = "${record.sampleRate} Hz"
                )

                // 分组3：分析数据（移除了声响总数，只保留音量相关数据）
                Text(text = "分析数据", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                GridSingleItem(
                    modifier = Modifier.fillMaxWidth(),
                    label = "平均音量",
                    value = String.format(Locale.CHINA, "%.1f", record.avgVolume)
                )
                GridSingleItem(
                    modifier = Modifier.fillMaxWidth(),
                    label = "峰值音量",
                    value = String.format(Locale.CHINA, "%.1f", record.maxWholeVolume)
                )
            }
        }
    }
}