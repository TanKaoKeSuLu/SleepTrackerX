package com.tkksl.sleeptracker.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.tkksl.sleeptracker.utils.formatDuration
import com.tkksl.sleeptracker.utils.formatQualityText
import com.tkksl.sleeptracker.utils.formatTimeStamp
import com.tkksl.sleeptracker.viewmodel.SleepViewModel
import com.tkksl.sleeptracker.viewmodel.SleepViewModelFactory

@Composable
fun SleepDetailScreen(
    navController: NavHostController,
    targetRecordId: Long
) {
    val context = LocalContext.current
    val sleepVm: SleepViewModel = viewModel(factory = SleepViewModelFactory(context))

    // 切换记录时停止原有音频
    LaunchedEffect(targetRecordId) {
        sleepVm.stopPlaying()
        sleepVm.loadRecordDetail(targetRecordId)
    }

    val record = sleepVm.currentRecord
    val isPlaying = sleepVm.isPlaying

    // 按钮颜色动画：播放红色 / 常态主题蓝色
    val btnColor = animateColorAsState(
        targetValue = if (isPlaying) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,
        label = "播放按钮颜色动画"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "🌙 睡眠详情",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (record == null) {
            Text("加载中或无此条睡眠记录")
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "睡眠记录",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    val startText = formatTimeStamp(record.startTime)
                    val endText = formatTimeStamp(record.endTime)
                    val durationText = formatDuration(record.duration)
                    val qualityText = formatQualityText(record.quality)

                    DetailItem(title = "开始时间", value = startText)
                    DetailItem(title = "结束时间", value = endText)
                    DetailItem(title = "持续时间", value = durationText)
                    DetailItem(title = "睡眠质量", value = qualityText)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 播放/暂停按钮
            Button(
                onClick = { sleepVm.playCurrentRecord() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !record.audioPath.isNullOrBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = btnColor.value)
            ) {
                Text(text = if (isPlaying) "⏸ 停止播放" else "▶ 播放录音")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 返回按钮，退出页面停止音频
            Button(
                onClick = {
                    sleepVm.stopPlaying()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("返回")
            }
        }
    }
}

@Composable
private fun DetailItem(
    title: String,
    value: String
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}