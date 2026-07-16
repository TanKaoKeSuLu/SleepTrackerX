package com.tkksl.sleeptracker.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tkksl.sleeptracker.ui.theme.PrimaryBlue
import com.tkksl.sleeptracker.ui.theme.RecordingRed

@Composable
fun RecordButton(
    isRecording: Boolean,
    elapsedTime: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 颜色过渡动画
        val buttonColor by animateColorAsState(
            targetValue = if (isRecording) RecordingRed else PrimaryBlue,
            label = "buttonColor"
        )

        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(72.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = if (isRecording) "结束睡眠记录" else "开始睡眠记录",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 基础提示文字
        Text(
            text = if (isRecording) "正在守护您的睡眠" else "今晚早点休息",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 仅录制时显示计时
        if (isRecording) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "⏱ $elapsedTime",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}