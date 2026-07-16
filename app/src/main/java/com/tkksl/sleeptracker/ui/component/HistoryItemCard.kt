package com.tkksl.sleeptracker.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

object SleepQualityColor {
    val Good = Color(0xFF4CAF50)
    val Normal = Color(0xFFFFC107)
    val Bad = Color(0xFFF44336)
}

@Composable
fun HistoryItemCard(
    recordId: Long,
    date: String,
    sleepHour: String,
    bedTime: String,
    wakeTime: String,
    qualityColor: Color,
    isMultiSelect: Boolean,
    isChecked: Boolean,
    onCardClick: () -> Unit,
    onCheckClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isMultiSelect) onCheckClick() else onCardClick()
            },
        shape = RoundedCornerShape(24.dp),
        // 动态卡片背景，跟随主题
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (isMultiSelect) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = if (isChecked) SleepQualityColor.Good else Color.Transparent,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column {
                Text(
                    text = date,
                    // 动态主文字色
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$bedTime ~ $wakeTime",
                    // 动态次要文字色
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = sleepHour,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.size(12.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(16.dp, 40.dp)
                        .padding(vertical = 2.dp)
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
                            .background(qualityColor, RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}