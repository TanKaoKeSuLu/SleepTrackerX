package com.tkksl.sleeptracker.utils

import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * 音频音量转换工具
 * 注意：volume 输入均为归一化 0‑1 的RMS振幅
 */

/**
 * 【算法内部使用】归一化0‑1振幅 → 相对分贝，范围 -60 ~ 0 dB
 * 仅用于阈值、噪音基线计算，不要给UI展示
 */
fun volumeToInternalDb(volume: Double): Int {
    val vol = volume.coerceAtLeast(0.001)
    val db = 20 * log10(vol)
    return db.coerceAtLeast(-60.0).roundToInt()
}

/**
 * 将内部[-60,0]分贝映射为用户友好 0‑100 声响等级
 * -60dB →0（几乎听不到），0dB→100（最大）
 */
fun dbToHumanLevel(internalDb: Int): Int {
    val clamped = internalDb.coerceIn(-60, 0)
    val level = ((clamped + 60) / 60.0) * 100
    return level.roundToInt()
}

/**
 * 直接传入归一化0‑1音量，得到UI展示用0‑100等级
 */
fun volumeToHumanLevel(volume: Double): Int {
    val db = volumeToInternalDb(volume)
    return dbToHumanLevel(db)
}
