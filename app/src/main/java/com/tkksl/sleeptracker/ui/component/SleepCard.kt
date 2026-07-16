package com.tkksl.sleeptracker.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tkksl.sleeptracker.ui.theme.Success

// 三种卡片状态枚举
sealed class SleepCardState {
    // 无睡眠记录（首次打开APP）
    object Empty : SleepCardState()
    // 正常睡眠数据
    data class Normal(
        val sleepDuration: String,
        val sleepDesc: String,
        val sleepScore: Int,
        val bedTime: String,
        val wakeTime: String
    ) : SleepCardState()
    // 正在录音记录中
    data class Recording(val recordDuration: String) : SleepCardState()
}

@Composable
fun SleepCard(
    state: SleepCardState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        // 跟随主题动态卡片背景
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            when (state) {
                is SleepCardState.Empty -> {
                    EmptySleepView()
                }
                is SleepCardState.Normal -> {
                    NormalSleepContentView(state)
                }
                is SleepCardState.Recording -> {
                    RecordingSleepView(state)
                }
            }
        }
    }
}

// 状态1：无睡眠记录
@Composable
private fun EmptySleepView() {
    Text(
        text = "今晚还没有记录",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "点击上方按钮开始第一次睡眠记录",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// 状态2：正在录音中
@Composable
private fun RecordingSleepView(state: SleepCardState.Recording) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "正在记录中",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "录制中",
            color = Success,
            style = MaterialTheme.typography.titleMedium
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = state.recordDuration,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "设备正在后台监听夜间声响",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// 状态3：存在完整睡眠数据
@Composable
private fun NormalSleepContentView(state: SleepCardState.Normal) {
    // 顶部标题行：标题 + 睡眠评价
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "昨晚睡眠",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "😊 ${state.sleepDesc}",
            color = Success,
            style = MaterialTheme.typography.titleMedium
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 核心睡眠时长
    Text(
        text = state.sleepDuration,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(20.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    Spacer(modifier = Modifier.height(20.dp))

    // 底部明细信息
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "🛌 入睡", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = state.bedTime, color = MaterialTheme.colorScheme.onBackground)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "⏰ 起床", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = state.wakeTime, color = MaterialTheme.colorScheme.onBackground)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "⭐ 睡眠评分", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "${state.sleepScore}分", color = MaterialTheme.colorScheme.onBackground)
        }
    }
}