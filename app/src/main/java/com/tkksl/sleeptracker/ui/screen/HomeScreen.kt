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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.tkksl.sleeptracker.navigation.Screen
import com.tkksl.sleeptracker.ui.component.HistoryCard
import com.tkksl.sleeptracker.ui.component.HistoryCardState
import com.tkksl.sleeptracker.ui.component.RecordButton
import com.tkksl.sleeptracker.ui.component.SleepCard
import com.tkksl.sleeptracker.ui.component.SleepCardState
import com.tkksl.sleeptracker.ui.component.TopBar
import com.tkksl.sleeptracker.viewmodel.SleepViewModel
import com.tkksl.sleeptracker.utils.formatTimeStamp
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: SleepViewModel,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val latestRecord = viewModel.latestRecord

    val elapsedTime = remember(viewModel.elapsedSeconds.value) {
        val sec = viewModel.elapsedSeconds.value
        val hour = sec / 3600
        val minute = (sec % 3600) / 60
        val second = sec % 60
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hour, minute, second)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        TopBar()

        Spacer(modifier = Modifier.height(32.dp))

        RecordButton(
            isRecording = viewModel.isRecording.value,
            elapsedTime = elapsedTime,
            isDarkMode = isDarkTheme,
            onClick = {
                viewModel.toggleRecording()
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        SleepCard(
            state = when {
                viewModel.isRecording.value -> {
                    SleepCardState.Recording(
                        recordDuration = elapsedTime
                    )
                }
                latestRecord.value != null -> {
                    val record = latestRecord.value!!
                    val recHour = record.duration / 3600
                    val recMin = (record.duration % 3600) / 60
                    val recSec = record.duration % 60
                    val durationStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", recHour, recMin, recSec)

                    val desc = when(record.quality) {
                        "Good" -> "优质睡眠"
                        "Normal" -> "一般睡眠"
                        else -> "浅度睡眠"
                    }
                    // 直接读取数据库保存好的睡眠评分，移除前端重复计算逻辑
                    val score = record.sleepScore
                    SleepCardState.Normal(
                        sleepDuration = durationStr,
                        sleepDesc = desc,
                        sleepScore = score,
                        bedTime = formatTimeStamp(record.startTime),
                        wakeTime = formatTimeStamp(record.endTime)
                    )
                }
                else -> SleepCardState.Empty
            },
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (latestRecord.value != null) {
            val record = latestRecord.value!!
            val qualityColor = when(record.quality) {
                "Good" -> Color(0xFF81C784)
                "Normal" -> Color(0xFFFFC107)
                else -> Color(0xFFF44336)
            }
            val fullStart = formatTimeStamp(record.startTime)
            val fullEnd = formatTimeStamp(record.endTime)
            val recordDate = fullStart.split(" ")[0]
            val bedShort = fullStart.split(" ")[1]
            val wakeShort = fullEnd.split(" ")[1]
            val h = record.duration / 3600
            val m = (record.duration % 3600) / 60
            val shortSleepHour = "${h}h${m}m"

            HistoryCard(
                state = HistoryCardState.HasLatestRecord(
                    date = recordDate,
                    sleepHour = shortSleepHour,
                    bedTime = bedShort,
                    wakeTime = wakeShort,
                    qualityColor = qualityColor,
                    onItemClick = {
                        navController.navigate(Screen.Detail.createRoute(record.id))
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
