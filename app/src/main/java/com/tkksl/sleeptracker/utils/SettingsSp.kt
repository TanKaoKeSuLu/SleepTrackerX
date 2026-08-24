package com.tkksl.sleeptracker.utils

import android.content.Context
import android.content.SharedPreferences
import com.tkksl.sleeptracker.data.settings.RecordingQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SettingsSp {
    private const val SP_NAME = "sleep_settings"
    private const val KEY_RECORD_QUALITY = "record_quality"
    private const val KEY_DARK_MODE = "dark_mode"

    private var _darkModeFlow = MutableStateFlow(false)
    val darkModeFlow: Flow<Boolean> = _darkModeFlow.asStateFlow()

    private fun getSp(context: Context): SharedPreferences {
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    init {
        // 初始化读取本地存储的深色状态
        val appContext: Context? = null
        appContext?.let {
            val isDark = getSp(it).getBoolean(KEY_DARK_MODE, false)
            _darkModeFlow.value = isDark
        }
    }

    // 保存深色模式
    fun saveDarkMode(context: Context, isDark: Boolean) {
        getSp(context).edit()
            .putBoolean(KEY_DARK_MODE, isDark)
            .apply()
        _darkModeFlow.value = isDark
    }

    // 读取深色模式
    fun getDarkMode(context: Context): Boolean {
        return getSp(context).getBoolean(KEY_DARK_MODE, false)
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