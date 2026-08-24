package com.tkksl.sleeptracker.ui.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.DeleteSweep
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import com.tkksl.sleeptracker.data.analyzer.AudioEvent
import com.tkksl.sleeptracker.data.model.SleepRecordWithEvents
import com.tkksl.sleeptracker.ui.component.AudioEventTimelineItem
import com.tkksl.sleeptracker.ui.component.SleepBriefAdviceCard
import com.tkksl.sleeptracker.ui.component.SleepInfoGrid
import com.tkksl.sleeptracker.ui.component.SleepSummaryCard
import com.tkksl.sleeptracker.utils.TimeFormatUtil
import com.tkksl.sleeptracker.viewmodel.SleepViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepDetailScreen(
    recordId: Long,
    viewModel: SleepViewModel,
    isDarkTheme: Boolean,
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
        containerColor = MaterialTheme.colorScheme.background,
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
                val totalSec = fullRecord.record.duration
                val totalTimeStr = if(totalSec < 60) "${totalSec}秒" else TimeFormatUtil.formatDurationText(totalSec)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "原始音频已清理",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "原始音频已删除，仅保留睡眠分析数据",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "总录音时长：$totalTimeStr · 本地隐私保护",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
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

            item {
                SleepSummaryCard(record = record, isDarkMode = isDarkTheme)
            }

            item {
                SleepInfoGrid(record = record, isDarkMode = isDarkTheme)
            }

            item {
                SleepBriefAdviceCard(record = record, isDarkMode = isDarkTheme)
            }

            item {
                Text(
                    text = "声响时间轴",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (eventList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "本次睡眠未检测到声响",
                            modifier = Modifier.padding(20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(eventList) { event ->
                    AudioEventTimelineItem(
                        event = event,
                        recordGlobalStartTime = record.startTime,
                        isItemPlaying = viewModel.currentPlayingSegmentStartSec == event.startSecond,
                        canPlay = event.clipPath.isNotBlank(), // 有片段文件则可播放
                        onPlayClick = {
                            viewModel.playAudioSegment(event.clipPath, event.startSecond)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
