package com.tkksl.sleeptracker.data.analyzer

object AudioTypeDisplay {
    fun getText(type: AudioType): String = when(type) {
        AudioType.UNKNOWN -> "未知声响"
        AudioType.SNORE -> "打鼾"
        AudioType.TALK -> "梦话"
        AudioType.COUGH -> "咳嗽"
        AudioType.TURNOVER -> "翻身"
        AudioType.DOG -> "狗叫"
        AudioType.BABY -> "婴儿啼哭"
    }

    fun getEmoji(type: AudioType): String = when(type) {
        AudioType.UNKNOWN -> "🔊"
        AudioType.SNORE -> "😴"
        AudioType.TALK -> "🗣"
        AudioType.COUGH -> "🤧"
        AudioType.TURNOVER -> "🛌"
        AudioType.DOG -> "🐶"
        AudioType.BABY -> "👶"
    }
}