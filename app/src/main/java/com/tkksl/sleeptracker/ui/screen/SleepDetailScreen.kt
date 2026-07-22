package com.tkksl.sleeptracker.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tkksl.sleeptracker.data.model.SleepRecordWithEvents
import com.tkksl.sleeptracker.utils.TimeFormatUtil
import com.tkksl.sleeptracker.viewmodel.SleepViewModel
import androidx.compose.foundation.layout.PaddingValues
import com.tkksl.sleeptracker.data.analyzer.AudioEvent
import com.tkksl.sleeptracker.ui.component.AudioEventTimelineItem
import com.tkksl.sleeptracker.ui.component.SleepBriefAdviceCard
import com.tkksl.sleeptracker.ui.component.SleepSummaryCard
import com.tkksl.sleeptracker.ui.component.SleepInfoGrid


// 全局页面背景色
private val PageBgLight = Color(0xFFF5F7FA)

// 和SummaryCard保持一致的配色规则
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepDetailScreen(
    recordId: Long,
    viewModel: SleepViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(recordId) {
        viewModel.loadRecordDetail(recordId)
    }

    val fullRecord: SleepRecordWithEvents? = viewModel.currentFullRecord
    val eventList: List<AudioEvent> = viewModel.detailEventList
    val isPlaying: Boolean = viewModel.isPlaying

    Scaffold(
        modifier = Modifier.fillMaxWidth(),
        containerColor = PageBgLight,
        topBar = {
            TopAppBar(
                title = { Text("睡眠报告") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (fullRecord != null) {
                val mainColor = getScoreMainColor(fullRecord.record.sleepScore)
                val lightBg = getScoreLightBgColor(fullRecord.record.sleepScore)
                val totalSec = fullRecord.record.duration
                // 格式化录音总时长
                val totalTimeStr = if(totalSec < 60) "${totalSec}秒" else TimeFormatUtil.formatDurationText(totalSec)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clickable {
                            if (isPlaying) viewModel.stopPlaying() else viewModel.playCurrentRecord()
                        },
                    border = BorderStroke(1.dp, mainColor),
                    colors = CardDefaults.cardColors(containerColor = lightBg),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = mainColor
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isPlaying) "暂停完整录音" else "播放完整录音",
                                style = MaterialTheme.typography.titleMedium,
                                color = mainColor,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "总录音时长：$totalTimeStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = mainColor.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (fullRecord == null) {
                item {
                    Text(text = "加载中...", modifier = Modifier.padding(top = 40.dp))
                }
                return@LazyColumn
            }

            val record = fullRecord.record

            // 1. 顶部睡眠汇总卡片
            item {
                SleepSummaryCard(record = record)
            }

            // 2. 分组网格化睡眠数据（替换原有竖排列表）
            item {
                SleepInfoGrid(record = record)
            }
            //3.今晚睡眠总结卡片
            item {
                SleepBriefAdviceCard(record = record)
            }

            // 4. 声响时间轴标题
            item {
                Text(
                    text = "声响时间轴",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 波形列表
            if (eventList.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "本次睡眠未检测到声响",
                            modifier = Modifier.padding(20.dp),
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(eventList) { event ->
                    AudioEventTimelineItem(
                        event = event,
                        recordGlobalStartTime = record.startTime,
                        isItemPlaying = false,
                        onPlayClick = { viewModel.playAudioSegment(event.startSecond) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}