package com.tkksl.sleeptracker.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatUtil {
    // 时间戳格式化 改为 年月日中文格式：yyyy年MM月dd日 HH:mm:ss
    fun formatTimeStamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA)
        return sdf.format(Date(timestamp))
    }

    fun formatSecondToHms(totalSec: Double): String {
        val s = totalSec.toLong()
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return String.format(Locale.CHINA, "%02d:%02d:%02d", h, m, sec)
    }

    fun formatDurationText(totalSec: Long): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        return if (h > 0) "${h}小时${m}分钟" else "${m}分钟"
    }
}