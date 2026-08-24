package com.tkksl.sleeptracker.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.tkksl.sleeptracker.navigation.Screen
import com.tkksl.sleeptracker.ui.component.HistoryCard
import com.tkksl.sleeptracker.ui.component.HistoryCardState
import com.tkksl.sleeptracker.ui.component.RecordButton
import com.tkksl.sleeptracker.ui.component.SleepCard
import com.tkksl.sleeptracker.ui.component.SleepCardState
import com.tkksl.sleeptracker.ui.component.TopBar
import com.tkksl.sleeptracker.viewmodel.SleepViewModel
import com.tkksl.sleeptracker.viewmodel.SleepViewModelFactory
import com.tkksl.sleeptracker.utils.formatTimeStamp

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: SleepViewModel,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val sleepViewModel: SleepViewModel = viewModel(factory = SleepViewModelFactory(context))

    val latestRecord = sleepViewModel.latestRecord

    // 新增：秒数转 HH:mm:ss，仅秒数变化才重算
    val elapsedTime = remember(sleepViewModel.elapsedSeconds) {
        val hour = sleepViewModel.elapsedSeconds / 3600
        val minute = (sleepViewModel.elapsedSeconds % 3600) / 60
        val second = sleepViewModel.elapsedSeconds % 60
        String.format("%02d:%02d:%02d", hour, minute, second)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        TopBar()

        Spacer(modifier = Modifier.height(32.dp))

        // 使用本地格式化后的 elapsedTime
        RecordButton(
            isRecording = sleepViewModel.isRecording,
            elapsedTime = elapsedTime,
            isDarkMode = isDarkTheme,
            onClick = {
                sleepViewModel.toggleRecording()
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        SleepCard(
            state = when {
                sleepViewModel.isRecording -> {
                    SleepCardState.Recording(
                        recordDuration = elapsedTime
                    )
                }
                latestRecord != null -> {
                    // 历史记录时长同样格式化
                    val recHour = latestRecord.duration / 3600
                    val recMin = (latestRecord.duration % 3600) / 60
                    val recSec = latestRecord.duration % 60
                    val durationStr = String.format("%02d:%02d:%02d", recHour, recMin, recSec)

                    val desc = when(latestRecord.quality) {
                        "Good" -> "优质睡眠"
                        "Normal" -> "一般睡眠"
                        else -> "浅度睡眠"
                    }
                    val score = when {
                        latestRecord.duration >= 6 * 3600 -> 90
                        latestRecord.duration >= 4 * 3600 -> 70
                        else -> 40
                    }
                    SleepCardState.Normal(
                        sleepDuration = durationStr,
                        sleepDesc = desc,
                        sleepScore = score,
                        bedTime = formatTimeStamp(latestRecord.startTime),
                        wakeTime = formatTimeStamp(latestRecord.endTime)
                    )
                }
                else -> SleepCardState.Empty
            },
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (latestRecord != null) {
            val qualityColor = when(latestRecord.quality) {
                "Good" -> Color(0xFF81C784)
                "Normal" -> Color(0xFFFFC107)
                else -> Color(0xFFF44336)
            }
            // 拆分时间戳，缩短文字防止布局错乱
            val fullStart = formatTimeStamp(latestRecord.startTime)
            val fullEnd = formatTimeStamp(latestRecord.endTime)
            val recordDate = fullStart.split(" ")[0]
            val bedShort = fullStart.split(" ")[1]
            val wakeShort = fullEnd.split(" ")[1]
            val h = latestRecord.duration / 3600
            val m = (latestRecord.duration % 3600) / 60
            val shortSleepHour = "${h}h${m}m"

            HistoryCard(
                state = HistoryCardState.HasLatestRecord(
                    date = recordDate,
                    sleepHour = shortSleepHour,
                    bedTime = bedShort,
                    wakeTime = wakeShort,
                    qualityColor = qualityColor,
                    // 点击跳转到详情页
                    onItemClick = {
                        navController.navigate(Screen.Detail.createRoute(latestRecord.id))
                    }
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        } else {
            HistoryCard(
                state = HistoryCardState.Empty,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
