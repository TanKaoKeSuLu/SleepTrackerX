package com.tkksl.sleeptracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_record")
data class SleepRecord(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 开始睡眠时间（时间戳）
    val startTime: Long,

    // 结束睡眠时间（时间戳）
    val endTime: Long,

    // 睡眠时长（秒）
    val duration: Long,

    // 睡眠质量（Good、Normal、Poor）
    val quality: String,

    // 录音文件路径（第一版可以为空）
    val audioPath: String? = null

)