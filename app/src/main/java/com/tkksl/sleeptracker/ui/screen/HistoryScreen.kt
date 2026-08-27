package com.tkksl.sleeptracker.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tkksl.sleeptracker.navigation.Screen
import com.tkksl.sleeptracker.ui.component.HistoryItemCard
import com.tkksl.sleeptracker.ui.component.SleepQualityColor
import com.tkksl.sleeptracker.utils.formatTimeStamp
import com.tkksl.sleeptracker.viewmodel.SleepViewModel

@Composable
fun HistoryScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SleepViewModel
) {
    // 使用 by remember 订阅State，自动解包并支持页面响应刷新
    val recordList by remember { viewModel.allRecordList }
    val isMultiSelectMode by remember { viewModel.isMultiSelectMode }
    val selectedIdSet by remember { viewModel.selectedIdSet }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxWidth(),
        bottomBar = {
            if (isMultiSelectMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "已选中 ${selectedIdSet.size} 条记录",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.selectAllOrCancel() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (selectedIdSet.size == recordList.size) "取消全选" else "全选")
                        }
                        Button(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedIdSet.isNotEmpty()
                        ) {
                            Text("删除选中记录")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "睡眠历史",
                    style = MaterialTheme.typography.headlineMedium
                )
                TextButton(onClick = { viewModel.switchSelectMode() }) {
                    Text(if (isMultiSelectMode) "完成" else "选择")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (recordList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "暂无睡眠记录，前往首页记录睡眠")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(recordList, key = { it.id }) { record ->
                        val fullStart = formatTimeStamp(record.startTime)
                        val fullEnd = formatTimeStamp(record.endTime)
                        val dateStr = fullStart.substringBefore(" ")
                        val bedStr = fullStart.substringAfter(" ")
                        val wakeStr = fullEnd.substringAfter(" ")

                        val h = record.duration / 3600
                        val m = (record.duration % 3600) / 60
                        val timeText = "${h}h${m}m"
                        val color = when (record.quality) {
                            "Good" -> SleepQualityColor.Good
                            "Normal" -> SleepQualityColor.Normal
                            else -> SleepQualityColor.Bad
                        }

                        HistoryItemCard(
                            recordId = record.id,
                            date = dateStr,
                            sleepHour = timeText,
                            bedTime = bedStr,
                            wakeTime = wakeStr,
                            qualityColor = color,
                            isMultiSelect = isMultiSelectMode,
                            isChecked = selectedIdSet.contains(record.id),
                            onCardClick = {
                                navController.navigate(Screen.Detail.createRoute(record.id))
                            },
                            onCheckClick = { viewModel.toggleRecordSelect(record.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除提示") },
            text = { Text("确定要删除选中的睡眠记录吗？删除后无法恢复！") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSelectedRecords()
                    showDeleteDialog = false
                }) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
