package com.tkksl.sleeptracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val quality: String, // Good / Normal / Poor 后续迭代换枚举
    val audioPath: String?,
    val fileSize: Long,
    val sampleRate: Int,
    // 必须手动计算传入，无默认值
    val sleepScore: Int,
    val noiseEventCount: Int,
    val avgVolume: Double,
    val maxWholeVolume: Double
)