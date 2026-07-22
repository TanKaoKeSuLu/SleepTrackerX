package com.tkksl.sleeptracker.data.analyzer

data class SleepAnalysis(
    //整晚平均音量
    val averageVolume: Double,
    //整晚最大音量
    val maxVolume: Double,
    //声音事件数量
    val noiseCount: Int,
    //声音总持续时间(单位：秒)
    val totalNoiseDuration: Double,
    //评分
    val score: Int,
    //事件列表
    val events: List<AudioEvent>
)