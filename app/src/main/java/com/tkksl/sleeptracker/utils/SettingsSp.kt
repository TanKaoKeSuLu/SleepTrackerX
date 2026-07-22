package com.tkksl.sleeptracker.utils

import android.content.Context
import android.content.SharedPreferences
import com.tkksl.sleeptracker.data.settings.RecordingQuality

object SettingsSp {
    private const val SP_NAME = "sleep_settings"
    private const val KEY_RECORD_QUALITY = "record_quality"

    private fun getSp(context: Context): SharedPreferences {
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    // 保存录音质量
    fun saveRecordQuality(context: Context, quality: RecordingQuality) {
        getSp(context).edit()
            .putString(KEY_RECORD_QUALITY, quality.name)
            .apply()
    }

    // 读取录音质量，默认标准音质
    fun getRecordQuality(context: Context): RecordingQuality {
        val str = getSp(context).getString(KEY_RECORD_QUALITY, RecordingQuality.NORMAL.name)
        return RecordingQuality.valueOf(str ?: RecordingQuality.NORMAL.name)
    }
}