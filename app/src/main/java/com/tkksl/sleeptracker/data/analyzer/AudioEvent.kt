package com.tkksl.sleeptracker.data.analyzer

import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * 归一化0‑1音量换算成分贝，下限‑60dB兜底
 */
fun volumeToDb(volume: Double): Int {
    val vol = volume.coerceAtLeast(0.001)
    val db = 20 * log10(vol)
    return db.coerceAtLeast(-60.0).roundToInt()
}

data class AudioEvent(
    // 相对录音起始的偏移秒数，支持小数
    val startSecond: Double,
    val endSecond: Double,
    // 峰值音量出现的时间点
    val peakTime: Double = 0.0,
    // 该事件内峰值音量 归一化 0‑1
    val maxVolume: Double,
    // 归一化波形采样点，0~1浮点数组，默认空
    val waveform: List<Float> = emptyList(),
    val type: AudioType = AudioType.UNKNOWN,
    // 移入构造函数：片段文件路径，可赋值传递
    val clipPath: String = ""
) {
    // 事件持续时长
    val duration: Double get() = endSecond - startSecond
}

val AudioEvent.peakDecibel: Int
    get() = volumeToDb(maxVolume)