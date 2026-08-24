package com.tkksl.sleeptracker.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tkksl.sleeptracker.data.analyzer.AudioEvent
import com.tkksl.sleeptracker.data.analyzer.getShowName
import com.tkksl.sleeptracker.data.analyzer.peakDecibel
import com.tkksl.sleeptracker.utils.TimeFormatUtil
import kotlin.math.roundToInt

private fun getRealTimeText(recordStartTime: Long, relativeSec: Double): String {
    val realTimeMs = recordStartTime + (relativeSec * 1000).toLong()
    return TimeFormatUtil.formatTimeStamp(realTimeMs).split(" ")[1]
}

@Composable
fun AudioEventTimelineItem(
    event: AudioEvent,
    recordGlobalStartTime: Long,
    isItemPlaying: Boolean,
    canPlay: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 固定卡片底色，不再随播放切换
    val cardBg = MaterialTheme.colorScheme.surfaceVariant
    // 文字固定使用常态配色，不随播放改变
    val mainTextColor = MaterialTheme.colorScheme.onSurface
    val hintTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    // 波形线条：播放时高亮主色，普通弱透明
    val waveformLineColor = if (isItemPlaying) {
        MaterialTheme.colorScheme.primary
    } else {
        hintTextColor.copy(alpha = 0.4f)
    }

    // 播放状态显示主题色边框
    val cardBorder = if (isItemPlaying) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 左侧播放指示竖条：播放时显示主题色高亮条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        color = if (isItemPlaying) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 18.dp, horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = getRealTimeText(recordGlobalStartTime, event.startSecond), color = mainTextColor)

                    AudioWaveform(
                        waveformData = event.waveform,
                        lineColor = waveformLineColor,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Text(text = getRealTimeText(recordGlobalStartTime, event.endSecond), color = mainTextColor)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onPlayClick,
                        enabled = canPlay,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isItemPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = "播放片段",
                            tint = if (canPlay) mainTextColor else mainTextColor.copy(alpha = 0.35f)
                        )
                    }
                    Text(text = "持续 ${event.duration.roundToInt()} 秒", color = mainTextColor)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "声响类型：${event.type.getShowName()}", color = mainTextColor)
                    Text(text = "峰值：${event.peakDecibel} dB", color = hintTextColor)
                }
            }
        }
    }
}