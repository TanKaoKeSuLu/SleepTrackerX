package com.tkksl.sleeptracker

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import com.tkksl.sleeptracker.data.audio.SleepRecordService
import com.tkksl.sleeptracker.viewmodel.SleepViewModel
import com.tkksl.sleeptracker.viewmodel.SleepViewModelFactory

class App : Application() {
    lateinit var globalVm: SleepViewModel

    private val streamAnalysisReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            if (intent.action != SleepRecordService.ACTION_STREAM_ANALYSIS_RESULT) return

            val startTs = intent.getLongExtra(SleepRecordService.EXTRA_RECORD_START, 0L)
            val endTs = intent.getLongExtra(SleepRecordService.EXTRA_RECORD_END, 0L)
            val sampleRate = intent.getIntExtra(SleepRecordService.EXTRA_SAMPLE_RATE,44100)
            val totalNoiseDuration = intent.getDoubleExtra(SleepRecordService.EXTRA_TOTAL_NOISE_DURATION,0.0)
            val avgVolume = intent.getDoubleExtra(SleepRecordService.EXTRA_AVG_VOLUME,0.0)
            val maxWholeVolume = intent.getDoubleExtra(SleepRecordService.EXTRA_MAX_WHOLE_VOLUME,0.0)
            val pcmFilePath = intent.getStringExtra(SleepRecordService.EXTRA_PCM_FILE_PATH)

            val eventBundleList = intent.getParcelableArrayListExtra<Bundle>("eventListBundle") ?: return

            if (startTs <= 0 || endTs <=0) return
            globalVm.handleStreamAnalysisResult(
                startTs = startTs,
                endTs = endTs,
                sampleRate = sampleRate,
                totalNoiseDuration = totalNoiseDuration,
                avgVolume = avgVolume,
                maxWholeVolume = maxWholeVolume,
                eventBundleList = eventBundleList,
                pcmFilePath = pcmFilePath
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        globalVm = SleepViewModelFactory(this).create(SleepViewModel::class.java)

        val streamFilter = IntentFilter(SleepRecordService.ACTION_STREAM_ANALYSIS_RESULT)
        // 注意：Application注册广播无法安全unregister，属于架构风险；生产建议迁移到Activity/ViewModel注册
        registerReceiver(streamAnalysisReceiver, streamFilter, Context.RECEIVER_EXPORTED)
    }
}
