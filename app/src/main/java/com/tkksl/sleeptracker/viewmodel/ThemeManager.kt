package com.tkksl.sleeptracker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ThemeManager(context: Context) : ViewModel() {
    private val sp = context.applicationContext.getSharedPreferences("app_config", Context.MODE_PRIVATE)
    // 先读取持久化值
    private val initDark = sp.getBoolean("dark_mode", true)
    private val _isDarkMode = MutableStateFlow(initDark)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    init {
        // 关键：构造完成后主动推送初始值，监听端能收到
        viewModelScope.launch {
            _isDarkMode.emit(initDark)
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val newVal = !_isDarkMode.value
            _isDarkMode.emit(newVal)
            sp.edit().putBoolean("dark_mode", newVal).apply()
        }
    }
}