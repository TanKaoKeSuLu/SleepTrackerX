package com.tkksl.sleeptracker.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tkksl.sleeptracker.data.analyzer.AudioEvent
import com.tkksl.sleeptracker.data.analyzer.getShowName
import com.tkksl.sleeptracker.utils.TimeFormatUtil
import kotlin.math.roundToInt

// 相对秒数 → 真实 HH:mm:ss
private fun getRealTimeText(recordStartTime: Long, relativeSec: Double): String {
    val realTimeMs = recordStartTime + (relativeSec * 1000).toLong()
    return TimeFormatUtil.formatTimeStamp(realTimeMs).split(" ")[1]
}

@Composable
fun AudioEventTimelineItem(
    event: AudioEvent,
    recordGlobalStartTime: Long,
    isItemPlaying: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F3))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ========== 第一层：开始时间 + 波形 + 结束时间 ==========
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = getRealTimeText(recordGlobalStartTime, event.startSecond))

                AudioWaveform(
                    waveformData = event.waveform,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Text(text = getRealTimeText(recordGlobalStartTime, event.endSecond))
            }

            // ========== 第二层：纯留白间距（只靠 spacedBy 控制距离，不用额外控件） ==========

            // ========== 第三层：左侧播放按钮 ｜ 右侧本段时长 ==========
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isItemPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = "播放片段",
                        tint = Color(0xFFEF6C00)
                    )
                }
                Text(text = "持续 ${event.duration.roundToInt()} 秒")
            }

            // 最底行：声响类型 + 峰值音量
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "声响类型：${event.type.getShowName()}")
                Text(text = "峰值音量：${event.maxVolume.roundToInt()}", color = Color.Gray)
            }
        }
    }
}