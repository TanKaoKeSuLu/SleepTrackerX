package com.tkksl.sleeptracker.data.analyzer

enum class AudioType {
    UNKNOWN,
    SNORE,    // 打鼾
    TALK,     // 梦话
    COUGH,    // 咳嗽
    TURNOVER, // 翻身
    DOG,      // 狗叫
    BABY      // 婴儿啼哭
}

// 枚举 -> 中文展示名称
fun AudioType.getShowName(): String {
    return when (this) {
        AudioType.UNKNOWN -> "未知声音"
        AudioType.SNORE -> "打鼾"
        AudioType.TALK -> "梦话"
        AudioType.COUGH -> "咳嗽"
        AudioType.TURNOVER -> "翻身响动"
        AudioType.DOG -> "犬吠声"
        AudioType.BABY -> "婴儿啼哭"
    }
}