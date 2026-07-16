package com.tkksl.sleeptracker.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SleepViewModelFactory(private val ctx: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = ctx.applicationContext as Application
        return when {
            modelClass.isAssignableFrom(SleepViewModel::class.java) -> SleepViewModel(app) as T
            modelClass.isAssignableFrom(ThemeManager::class.java) -> ThemeManager(app) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}