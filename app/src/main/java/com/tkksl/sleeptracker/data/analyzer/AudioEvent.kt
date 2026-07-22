package com.tkksl.sleeptracker.data.analyzer

data class AudioEvent(
    // 相对录音起始的偏移秒数，支持小数
    val startSecond: Double,
    val endSecond: Double,
    // 峰值音量出现的时间点
    val peakTime: Double = 0.0,
    // 该事件内峰值音量
    val maxVolume: Double,
    // 新增：归一化波形采样点，0~1浮点数组，默认空
    val waveform: List<Float> = emptyList(),
    val type: AudioType = AudioType.UNKNOWN
) {
    // 事件持续时长
    val duration: Double get() = endSecond - startSecond
}