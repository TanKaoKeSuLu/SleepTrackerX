package com.tkksl.sleeptracker.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

sealed class HistoryCardState {
    object Empty : HistoryCardState()
    data class HasLatestRecord(
        val date: String,
        val sleepHour: String,
        val bedTime: String,
        val wakeTime: String,
        val qualityColor: Color,
        val onItemClick: () -> Unit
    ) : HistoryCardState()
}

@Composable
fun HistoryCard(
    state: HistoryCardState,
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
            Text(
                text = "最近记录",
                style = MaterialTheme.typography.titleMedium,
                // 主题主文字色
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            when(state) {
                is HistoryCardState.Empty -> {
                    Text(
                        text = "暂无睡眠记录",
                        style = MaterialTheme.typography.bodyMedium,
                        // 主题次要灰色文字
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is HistoryCardState.HasLatestRecord -> {
                    HistoryItemCard(
                        recordId = 0L,
                        date = state.date,
                        sleepHour = state.sleepHour,
                        bedTime = state.bedTime,
                        wakeTime = state.wakeTime,
                        qualityColor = state.qualityColor,
                        isMultiSelect = false,
                        isChecked = false,
                        onCardClick = state.onItemClick,
                        onCheckClick = {},
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}