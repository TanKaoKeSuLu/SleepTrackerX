package com.tkksl.sleeptracker.ui.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tkksl.sleeptracker.LocalThemeManager
import com.tkksl.sleeptracker.data.settings.RecordingQuality
import com.tkksl.sleeptracker.ui.component.TopBar
import com.tkksl.sleeptracker.viewmodel.SleepViewModel
import com.tkksl.sleeptracker.viewmodel.ThemeManager
import kotlinx.coroutines.flow.collectLatest

// 通用设置条目组件
@Composable
fun SettingItem(
    contentIcon: @Composable () -> Unit,
    title: String,
    desc: String,
    toggleContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        contentIcon()
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        toggleContent?.invoke()
    }
}

@Composable
fun SettingsScreen(
    sleepVm: SleepViewModel
) {
    val themeVm: ThemeManager = LocalThemeManager.current
    var isDarkMode by remember { mutableStateOf(themeVm.isDarkMode.value) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showQualitySelectDialog by remember { mutableStateOf(false) }

    // 修复：规范LaunchedEffect监听主题流，不在组合层直接调用launch
    LaunchedEffect(themeVm) {
        themeVm.isDarkMode.collectLatest { newDark ->
            isDarkMode = newDark
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        TopBar()
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "设置",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        // 1.显示主题切换
        SettingItem(
            contentIcon = {
                Icon(
                    imageVector = Icons.Filled.Nightlight,
                    contentDescription = "夜间模式",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = "显示主题",
            desc = if (isDarkMode) "深色（夜间）" else "浅色（日间）",
            toggleContent = {
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { themeVm.toggleTheme() }
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2.录音质量设置
        val currentQualityText = when (sleepVm.currentRecordQuality) {
            RecordingQuality.LOW -> "低音质 16000Hz（省空间）"
            RecordingQuality.NORMAL -> "标准音质 44100Hz（默认）"
            RecordingQuality.HIGH -> "高清音质 48000Hz（声音分析）"
        }
        SettingItem(
            contentIcon = {
                Icon(
                    imageVector = Icons.Filled.SettingsVoice,
                    contentDescription = "录音质量",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = "录音质量",
            desc = currentQualityText,
            onClick = { showQualitySelectDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3.麦克风权限
        SettingItem(
            contentIcon = {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "麦克风",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = "麦克风权限",
            desc = "用于夜间睡眠声响记录"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4.存储位置
        SettingItem(
            contentIcon = {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = "存储",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = "存储位置",
            desc = "本地离线存储，无云端上传"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5.清除全部记录
        SettingItem(
            contentIcon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = "清除所有记录",
            desc = "删除全部睡眠数据，删除后无法恢复",
            onClick = { showClearAllDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 6.关于App
        SettingItem(
            contentIcon = {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "关于",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = "关于 Sleep Tracker",
            desc = "纯本地离线睡眠记录工具"
        )
    }

    // 录音质量选择弹窗
    if (showQualitySelectDialog) {
        AlertDialog(
            onDismissRequest = { showQualitySelectDialog = false },
            title = { Text("选择录音质量") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QualityRadioOption(
                        text = "低音质 16000Hz\n占用存储空间最小，仅用于声音采集",
                        selected = sleepVm.currentRecordQuality == RecordingQuality.LOW,
                        onClick = { sleepVm.setRecordingQuality(RecordingQuality.LOW) }
                    )
                    QualityRadioOption(
                        text = "标准音质 44100Hz\n默认选项，平衡音质与存储",
                        selected = sleepVm.currentRecordQuality == RecordingQuality.NORMAL,
                        onClick = { sleepVm.setRecordingQuality(RecordingQuality.NORMAL) }
                    )
                    QualityRadioOption(
                        text = "高清音质 48000Hz\n音质最佳，适合后续鼾声、环境音分析",
                        selected = sleepVm.currentRecordQuality == RecordingQuality.HIGH,
                        onClick = { sleepVm.setRecordingQuality(RecordingQuality.HIGH) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualitySelectDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    // 清空记录弹窗
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = {
                Text(
                    "清空全部记录",
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Text(
                    "确定要删除所有睡眠记录吗？删除后数据永久无法找回！",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    sleepVm.eraseAllData()
                    showClearAllDialog = false
                }) {
                    Text("确认清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        )
    }
}

// 弹窗内单选条目组件
@Composable
private fun QualityRadioOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = text)
    }
}