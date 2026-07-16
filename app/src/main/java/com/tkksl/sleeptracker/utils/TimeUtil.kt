package com.tkksl.sleeptracker.utils

// 秒数 → 时分秒 格式 00:00:00
fun formattedTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

// 秒数 → X小时X分钟（详情/历史卡片展示用）
fun formatDuration(seconds: Long): String {
    val hour = seconds / 3600
    val minute = (seconds % 3600) / 60
    return "${hour}小时${minute}分钟"
}

// 时间戳 → 年月日时分字符串
fun formatTimeStamp(ms: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(ms)
}

// 睡眠质量码转带表情中文文案
fun formatQualityText(quality: String?): String {
    return when (quality) {
        "Good" -> "😊 良好"
        "Normal" -> "😐 一般"
        "Poor" -> "😞 较差"
        else -> "--"
    }
}