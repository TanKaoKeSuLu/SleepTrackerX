package com.tkksl.sleeptracker.data.analyzer

object SleepScoreCalculator {
    /**
     * V3 睡眠评分模型
     * @param totalSecond 总睡眠时长(秒)
     * @param noiseDuration 所有声响事件总持续时长(秒)
     * @param noiseEventCount 声响事件总次数
     * @param maxVolume 整晚最大音量峰值
     * @return 睡眠评分 0~100
     */
    fun calculate(
        totalSecond: Double,
        noiseDuration: Double,
        noiseEventCount: Int,
        maxVolume: Double
    ): Int {
        var totalScore = 0

        // 1. 睡眠时长 满分40
        totalScore += when {
            totalSecond >= 8 * 3600 -> 40
            totalSecond >= 6 * 3600 -> 35
            totalSecond >= 4 * 3600 -> 25
            else -> 10
        }

        // 2. 声音干扰占比 满分30
        val noiseRatio = if (totalSecond <= 0) 1.0 else noiseDuration / totalSecond
        totalScore += when {
            noiseRatio < 0.01 -> 30
            noiseRatio < 0.03 -> 25
            noiseRatio < 0.08 -> 18
            noiseRatio < 0.15 -> 10
            else -> 5
        }

        // 3. 声音强度 满分15
        totalScore += when {
            maxVolume < 1500 -> 15
            maxVolume < 3000 -> 12
            maxVolume < 5000 -> 8
            else -> 3
        }

        // 4. 睡眠稳定性(事件数量) 满分15
        totalScore += when {
            noiseEventCount <= 20 -> 15
            noiseEventCount <= 50 -> 12
            noiseEventCount <= 100 -> 8
            else -> 3
        }

        return totalScore.coerceIn(0, 100)
    }
}