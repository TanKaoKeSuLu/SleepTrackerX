package com.tkksl.sleeptracker.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.tkksl.sleeptracker.data.audio.AudioPlayer
import com.tkksl.sleeptracker.data.audio.SleepRecordService
import com.tkksl.sleeptracker.data.local.DatabaseProvider
import com.tkksl.sleeptracker.data.model.SleepRecord
import com.tkksl.sleeptracker.data.repository.SleepRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class SleepViewModel(application: Application) : AndroidViewModel(application) {
    // 仅保留播放器，录音、转码逻辑全部迁移到Service
    private val audioPlayer = AudioPlayer()
    private val appContext = getApplication<Application>()

    private val sleepDao = DatabaseProvider.getDatabase(appContext).sleepDao()
    private val repo = SleepRepository(sleepDao)

    // 录音完成广播接收器
    private val recordFinishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val audioPath = intent?.getStringExtra("audioPath")
            val startTime = intent?.getLongExtra("startTime", 0L) ?: 0L
            val endTime = intent?.getLongExtra("endTime", 0L) ?: 0L

            // 数据合法性校验
            if(audioPath.isNullOrEmpty() || startTime <= 0 || endTime <= startTime) return

            val durationSec = (endTime - startTime) / 1000
            // 少于10秒直接丢弃，不入库
            if(durationSec < 10) return

            // 根据时长判定睡眠质量
            val quality = when {
                durationSec >= 6 * 3600 -> "Good"
                durationSec >= 3 * 3600 -> "Normal"
                else -> "Poor"
            }

            val newRecord = SleepRecord(
                startTime = startTime,
                endTime = endTime,
                duration = durationSec,
                quality = quality,
                audioPath = audioPath
            )

            // 协程入库并刷新列表
            viewModelScope.launch {
                repo.insertSleepRecord(newRecord)
                loadRecords()
            }
        }
    }

    var elapsedSeconds by mutableStateOf(0L)
        private set
    private var timerJob: Job? = null
    var isRecording by mutableStateOf(false)
        private set

    var latestRecord: SleepRecord? by mutableStateOf(null)
        private set
    var allRecordList: List<SleepRecord> by mutableStateOf(emptyList())
        private set

    // 详情页单独存储当前选中记录
    var currentRecord by mutableStateOf<SleepRecord?>(null)
        private set

    // 音频播放响应式状态，UI自动重组
    var isPlaying by mutableStateOf(false)
        private set

    // 多选状态
    var isMultiSelectMode by mutableStateOf(false)
        private set
    var selectedIdSet by mutableStateOf(setOf<Long>())
        private set

    private var startTs = 0L

    init {
        loadRecords()
        // 注册录音完成广播
        val filter = IntentFilter("SLEEP_RECORD_FINISHED")
        appContext.registerReceiver(
            recordFinishReceiver,
            filter,
            Context.RECEIVER_EXPORTED
        )
    }

    override fun onCleared() {
        super.onCleared()
        // 页面销毁时注销广播，防止内存泄漏
        appContext.unregisterReceiver(recordFinishReceiver)
    }

    fun toggleRecording() {
        if (!isRecording) {
            // 启动前台录音服务，录音、文件创建、转码全部交给Service
            val intent = Intent(appContext, SleepRecordService::class.java)
            intent.action = SleepRecordService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }

            isRecording = true
            elapsedSeconds = 0L
            startTs = System.currentTimeMillis()
            timerJob = viewModelScope.launch {
                while (isActive) {
                    delay(1000)
                    elapsedSeconds++
                }
            }
        } else {
            // 停止前台录音服务
            val stopIntent = Intent(appContext, SleepRecordService::class.java)
            stopIntent.action = SleepRecordService.ACTION_STOP
            appContext.startService(stopIntent)

            timerJob?.cancel()
            isRecording = false
            elapsedSeconds = 0L
        }
    }

    private fun loadRecords() {
        viewModelScope.launch {
            allRecordList = repo.getAllSleepRecords()
            latestRecord = repo.getLatestSleepRecord()
        }
    }

    // 根据ID加载单条记录，供给SleepDetail详情页使用
    fun loadRecordDetail(id: Long) {
        viewModelScope.launch {
            currentRecord = repo.getRecordById(id)
        }
    }

    fun eraseAllData() {
        viewModelScope.launch {
            repo.clearAllSleepRecords()
            allRecordList = emptyList()
            latestRecord = null
            currentRecord = null
            isPlaying = false
            selectedIdSet = emptySet()
            isMultiSelectMode = false
        }
    }

    fun switchSelectMode() {
        isMultiSelectMode = !isMultiSelectMode
        selectedIdSet = emptySet()
    }

    fun toggleRecordSelect(recordId: Long) {
        selectedIdSet = if (selectedIdSet.contains(recordId)) {
            selectedIdSet - recordId
        } else {
            selectedIdSet + recordId
        }
    }

    fun selectAllOrCancel() {
        val allIds = allRecordList.map { it.id }.toSet()
        selectedIdSet = if (selectedIdSet.size == allIds.size) emptySet() else allIds
    }

    fun deleteSelectedRecords() {
        viewModelScope.launch {
            repo.deleteSelectedRecords(selectedIdSet.toList())
            loadRecords()
            isMultiSelectMode = false
            selectedIdSet = emptySet()
        }
    }

    // 播放/暂停当前详情记录音频
    fun playCurrentRecord() {
        val path = currentRecord?.audioPath ?: return

        if (isPlaying) {
            audioPlayer.stop()
            isPlaying = false
            return
        }

        audioPlayer.play(path) {
            // 音频播放完毕自动重置状态
            isPlaying = false
        }
        isPlaying = true
    }

    // 强制停止播放
    fun stopPlaying() {
        audioPlayer.stop()
        isPlaying = false
    }

    // 底层原始播放状态查询（备用）
    fun getAudioPlayingState(): Boolean {
        return audioPlayer.isPlaying()
    }
}