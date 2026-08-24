package com.tkksl.sleeptracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tkksl.sleeptracker.data.analyzer.AudioType

@Entity(
    tableName = "audio_events",
    foreignKeys = [
        ForeignKey(
            entity = SleepRecord::class,
            parentColumns = ["id"],
            childColumns = ["sleepRecordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sleepRecordId"])]
)
data class AudioEventEntity(
    @PrimaryKey(autoGenerate = true) val eventId: Long = 0,
    val sleepRecordId: Long,
    val startSecond: Double,
    val endSecond: Double,
    val maxVolume: Double,
    val peakTime: Double = 0.0,
    val type: AudioType = AudioType.UNKNOWN,
    // 波形浮点数组JSON字符串
    val waveformJson: String = "",
    // 新增：单条事件对应的音频片段文件路径
    val clipPath: String = ""
) {
    val duration: Double
        get() = endSecond - startSecond
}