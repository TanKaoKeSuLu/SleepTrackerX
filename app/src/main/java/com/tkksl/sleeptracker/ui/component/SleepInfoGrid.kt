package com.tkksl.sleeptracker.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tkksl.sleeptracker.data.model.SleepRecord
import com.tkksl.sleeptracker.utils.TimeFormatUtil
import com.tkksl.sleeptracker.utils.volumeToHumanLevel

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
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SleepInfoGrid(
    record: SleepRecord,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "睡眠详细数据",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        val outerCardBg = if (isDarkMode) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = outerCardBg
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "录音参数",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                GridTwoItemRow(
                    leftLabel = "总时长",
                    leftValue = if (record.duration < 60) "${record.duration}秒" else TimeFormatUtil.formatDurationText(record.duration),
                    rightLabel = "采样率",
                    rightValue = "${record.sampleRate} Hz"
                )

                Text(
                    text = "分析数据",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val avgLevel = volumeToHumanLevel(record.avgVolume)
                val maxWholeLevel = volumeToHumanLevel(record.maxWholeVolume)
                GridTwoItemRow(
                    leftLabel = "平均声响等级",
                    leftValue = "$avgLevel / 100",
                    rightLabel = "全局峰值等级",
                    rightValue = "$maxWholeLevel / 100"
                )
            }
        }
    }
}
