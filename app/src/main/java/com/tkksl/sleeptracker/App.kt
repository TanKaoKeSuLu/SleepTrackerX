package com.tkksl.sleeptracker

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.tkksl.sleeptracker.viewmodel.SleepViewModel
import com.tkksl.sleeptracker.viewmodel.SleepViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {
    lateinit var globalVm: SleepViewModel
    private val appScope = CoroutineScope(Dispatchers.Default)

    private val recordFinishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // 统一判空，避免空指针
            if (intent == null) return

            val audioPath = intent.getStringExtra("audioPath") ?: return
            val pcmPath = intent.getStringExtra("pcmPath") ?: return
            val startTime = intent.getLongExtra("startTime", 0L)
            val endTime = intent.getLongExtra("endTime", 0L)

            // 时间戳有效性校验，过滤异常数据
            if (startTime <= 0 || endTime <= startTime) return

            // 核心修复：耗时分析切到后台线程，避免阻塞主线程引发ANR
            appScope.launch {
                globalVm.handleRecordFinish(audioPath, pcmPath, startTime, endTime)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // 全局唯一ViewModel实例，全应用共用
        globalVm = SleepViewModelFactory(this).create(SleepViewModel::class.java)
        // 注册录音完成广播
        val filter = IntentFilter("SLEEP_RECORD_FINISHED")
        registerReceiver(recordFinishReceiver, filter, Context.RECEIVER_EXPORTED)
    }
}