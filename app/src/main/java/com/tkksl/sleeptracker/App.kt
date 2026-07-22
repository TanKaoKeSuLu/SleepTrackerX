package com.tkksl.sleeptracker

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.tkksl.sleeptracker.viewmodel.SleepViewModel
import com.tkksl.sleeptracker.viewmodel.SleepViewModelFactory

class App : Application() {
    lateinit var globalVm: SleepViewModel

    private val recordFinishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val audioPath = intent?.getStringExtra("audioPath") ?: return
            val pcmPath = intent?.getStringExtra("pcmPath") ?: return
            val start = intent.getLongExtra("startTime", 0L)
            val end = intent.getLongExtra("endTime", 0L)
            globalVm.handleRecordFinish(audioPath, pcmPath, start, end)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // 全局唯一ViewModel
        globalVm = SleepViewModelFactory(this).create(SleepViewModel::class.java)
        // 全局注册录音完成广播
        val filter = IntentFilter("SLEEP_RECORD_FINISHED")
        registerReceiver(recordFinishReceiver, filter, Context.RECEIVER_EXPORTED)
    }
}