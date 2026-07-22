package com.tkksl.sleeptracker.data.settings

/**
 * 录音底层参数配置实体
 */
data class RecordingConfig(
    val sampleRate: Int,
    val bitDepth: Int = 16,
    val channelCount: Int = 1
)

/**
 * 录音质量档位枚举
 */
enum class RecordingQuality {
    LOW,
    NORMAL,
    HIGH;

    /** 根据档位映射对应底层录音参数 */
    fun toConfig(): RecordingConfig = when(this) {
        LOW -> RecordingConfig(sampleRate = 16000)
        NORMAL -> RecordingConfig(sampleRate = 44100)
        HIGH -> RecordingConfig(sampleRate = 48000)
    }
}