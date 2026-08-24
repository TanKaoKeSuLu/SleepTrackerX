package com.tkksl.sleeptracker.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tkksl.sleeptracker.ui.theme.LightCardBackground
import com.tkksl.sleeptracker.ui.theme.LightTextMain
import com.tkksl.sleeptracker.ui.theme.ErrorRed

@Composable
fun RecordButton(
    isRecording: Boolean,
    elapsedTime: String,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val (containerColor, contentColor) = when {
            // 录制中：统一红底 + 纯白色文字，日夜保持一致
            isRecording -> {
                ErrorRed to Color.White
            }
            // 夜间未录制：强制白底+黑字（复用浅色主题静态色）
            !isRecording && isDarkMode -> {
                LightCardBackground to LightTextMain
            }
            // 日间未录制：原有主题主色，保留不变
            else -> {
                MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
            }
        }

        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(72.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = if (isRecording) "结束睡眠记录" else "开始睡眠记录",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isRecording) "正在守护您的睡眠" else "今晚早点休息",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isRecording) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = elapsedTime,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
