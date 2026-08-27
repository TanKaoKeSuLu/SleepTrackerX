package com.tkksl.sleeptracker.viewmodel

import android.app.Application
import android.os.Bundle
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tkksl.sleeptracker.data.audio.AudioPlayer
import com.tkksl.sleeptracker.data.audio.SleepRecordService
import com.tkksl.sleeptracker.data.local.DatabaseProvider
import com.tkksl.sleeptracker.data.model.AudioEventEntity
import com.tkksl.sleeptracker.data.model.SleepRecord
import com.tkksl.sleeptracker.data.model.SleepRecordWithEvents
import com.tkksl.sleeptracker.data.repository.SleepRepository
import com.tkksl.sleeptracker.data.settings.RecordingQuality
import com.tkksl.sleeptracker.data.analyzer.AudioEvent
import com.tkksl.sleeptracker.data.analyzer.AudioType
import com.tkksl.sleeptracker.data.analyzer.SleepScoreCalculator
import com.tkksl.sleeptracker.data.settings.RecordingConfig
import com.tkksl.sleeptracker.utils.SettingsSp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import kotlin.time.Duration.Companion.seconds
import android.util.Log

class SleepViewModel(application: Application) : AndroidViewModel(application) {
    private val audioPlayer = AudioPlayer()
    private val appContext: Application = getApplication()

    private val sleepDao = DatabaseProvider.getDatabase(appContext).sleepDao()
    private val repo = SleepRepository(sleepDao)

    // 波形JSON字符串解析为List<Float>
    private fun parseWaveformJson(json: String): List<Float> {
        if (json.isBlank()) return emptyList()
        val arr = JSONArray(json)
        val result = mutableListOf<Float>()
        for (i in 0 until arr.length()) {
            result.add(arr.getDouble(i).toFloat())
        }
        return result
    }

    // ========== Compose State 标准规范：私有MutableState，对外暴露只读State ==========
    private val _currentRecordQuality = mutableStateOf(SettingsSp.getRecordQuality(appContext))
    val currentRecordQuality: State<RecordingQuality> = _currentRecordQuality

    private val _elapsedSeconds = mutableLongStateOf(0L)
    val elapsedSeconds: State<Long> = _elapsedSeconds

    private val _isRecording = mutableStateOf(false)
    val isRecording: State<Boolean> = _isRecording

    private val _latestRecord = mutableStateOf<SleepRecord?>(null)
    val latestRecord: State<SleepRecord?> = _latestRecord

    private val _allRecordList = mutableStateOf<List<SleepRecord>>(emptyList())
    val allRecordList: State<List<SleepRecord>> = _allRecordList

    private val _currentFullRecord = mutableStateOf<SleepRecordWithEvents?>(null)
    val currentFullRecord: State<SleepRecordWithEvents?> = _currentFullRecord

    private val _detailEventList = mutableStateOf<List<AudioEvent>>(emptyList())
    val detailEventList: State<List<AudioEvent>> = _detailEventList

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _currentPlayingSegmentStartSec = mutableStateOf<Double?>(null)
    val currentPlayingSegmentStartSec: State<Double?> = _currentPlayingSegmentStartSec

    private val _isMultiSelectMode = mutableStateOf(false)
    val isMultiSelectMode: State<Boolean> = _isMultiSelectMode

    private val _selectedIdSet = mutableStateOf(setOf<Long>())
    val selectedIdSet: State<Set<Long>> = _selectedIdSet

    private var timerJob: Job? = null
    private var startTs = 0L

    init {
        loadRecords()
    }

    override fun onCleared() {
        audioPlayer.release()
    }

    /**
     * 【新链路】流式分析结果广播入口
     */
    fun handleStreamAnalysisResult(
        startTs: Long,
        endTs: Long,
        sampleRate: Int,
        totalNoiseDuration: Double,
        avgVolume: Double,
        maxWholeVolume: Double,
        eventBundleList: ArrayList<Bundle>,
        pcmFilePath: String?
    ) {
        val durationSecTotal = (endTs - startTs) / 1000.0
        Log.d("StreamAnalyze", "收到流式结果 duration=$durationSecTotal, eventCount=${eventBundleList.size}")

        if (durationSecTotal < 10.0) {
            Log.d("StreamAnalyze", "时长不足10s，丢弃本次记录")
            return
        }
        if (eventBundleList.isNullOrEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val eventEntities = mutableListOf<AudioEventEntity>()
                for (b in eventBundleList) {
                    val startSec = b.getDouble("startSecond")
                    val endSec = b.getDouble("endSecond")
                    val peakTime = b.getDouble("peakTime")
                    val maxVol = b.getDouble("maxVolume")
                    val typeOrdinal = b.getInt("typeOrdinal")
                    val audioType = AudioType.values()[typeOrdinal]
                    val clipPath = b.getString("clipPath") ?: ""

                    eventEntities.add(
                        AudioEventEntity(
                            sleepRecordId = 0,
                            startSecond = startSec,
                            endSecond = endSec,
                            peakTime = peakTime,
                            maxVolume = maxVol,
                            type = audioType,
                            waveformJson = "",
                            clipPath = clipPath
                        )
                    )
                }

                val noiseCount = eventEntities.size
                // 真实睡眠分数计算
                val sleepScore = SleepScoreCalculator.calculate(
                    totalSecond = durationSecTotal,
                    noiseDuration = totalNoiseDuration,
                    noiseEventCount = noiseCount,
                    maxVolume = maxWholeVolume
                )

                val record = SleepRecord(
                    startTime = startTs,
                    endTime = endTs,
                    duration = durationSecTotal.toLong(),
                    quality = when {
                        durationSecTotal >= 6 * 3600 -> "Good"
                        durationSecTotal >= 3 * 3600 -> "Normal"
                        else -> "Poor"
                    },
                    audioPath = "",
                    fileSize = 0,
                    sampleRate = sampleRate,
                    sleepScore = sleepScore,
                    noiseEventCount = noiseCount,
                    avgVolume = avgVolume,
                    maxWholeVolume = maxWholeVolume
                )

                val ok = repo.insertFullSleepRecord(record, eventEntities)
                Log.d("StreamAnalyze", "流式入库 ok=$ok score=$sleepScore")
                if (ok) {
                    //入库成功才删除原始pcm
                    pcmFilePath?.let { path ->
                        val f = File(path)
                        if (f.exists()) f.delete()
                    }
                    loadRecords()
                } else {
                    Log.w("StreamAnalyze", "数据库插入失败，保留pcm文件")
                }

            } catch (e: Exception) {
                Log.e("StreamAnalyze", "流式入库异常", e)
            }
        }
    }


    // 切换录音质量，持久化保存
    fun setRecordingQuality(quality: RecordingQuality) {
        _currentRecordQuality.value = quality
        SettingsSp.saveRecordQuality(appContext, quality)
    }

    // 启动/停止录音服务：停止时优先更新UI，再通知后台
    fun toggleRecording() = if (!_isRecording.value) {
        val intent = android.content.Intent(appContext, SleepRecordService::class.java).apply {
            action = SleepRecordService.ACTION_START
            putExtra(SleepRecordService.EXTRA_RECORD_QUALITY, _currentRecordQuality.value.name)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) appContext.startForegroundService(intent)
        else appContext.startService(intent)

        _isRecording.value = true
        _elapsedSeconds.value = 0L
        startTs = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1.seconds)
                _elapsedSeconds.value++
            }
        }
    } else {
        // UI 优先响应：先停计时器、更新状态，再通知后台服务停止
        timerJob?.cancel()
        _isRecording.value = false
        _elapsedSeconds.value = 0L

        val stopIntent = android.content.Intent(appContext, SleepRecordService::class.java).apply {
            action = SleepRecordService.ACTION_STOP
        }
        appContext.startService(stopIntent)
    }

    // 加载首页列表（轻量化，不带声响事件）
    private fun loadRecords() {
        viewModelScope.launch {
            repo.getAllSleepRecordsFlow().collect { list ->
                _allRecordList.value = list
                _latestRecord.value = repo.getLatestSleepRecord()
            }
        }
    }

    // 详情页加载完整数据：睡眠记录 + 全部声响事件
    fun loadRecordDetail(recordId: Long) {
        viewModelScope.launch {
            val rawRecordWrap = repo.getRecordWithEvents(recordId) ?: return@launch
            // 存入原始Room实体包装类
            _currentFullRecord.value = rawRecordWrap
            // 数据库实体 -> 解析波形JSON，转为UI可用AudioEvent，同步携带片段路径
            val uiEventList = rawRecordWrap.events.map { entity ->
                val waveData = parseWaveformJson(entity.waveformJson)
                AudioEvent(
                    startSecond = entity.startSecond,
                    endSecond = entity.endSecond,
                    peakTime = entity.peakTime,
                    maxVolume = entity.maxVolume,
                    waveform = waveData,
                    type = entity.type,
                    clipPath = entity.clipPath
                )
            }
            _detailEventList.value = uiEventList
        }
    }

    // 一键清空所有数据（文件+数据库双清空）
    fun eraseAllData() {
        viewModelScope.launch {
            repo.clearAllSleepRecords()
            loadRecords()
            _currentFullRecord.value = null
            _detailEventList.value = emptyList()
            _latestRecord.value = null
            _allRecordList.value = emptyList()
            stopPlaying()
            _selectedIdSet.value = emptySet()
            _isMultiSelectMode.value = false
        }
    }

    // 多选模式相关
    fun switchSelectMode() {
        _isMultiSelectMode.value = !_isMultiSelectMode.value
        _selectedIdSet.value = emptySet()
    }

    fun toggleRecordSelect(recordId: Long) {
        val newSet = if (_selectedIdSet.value.contains(recordId)) {
            _selectedIdSet.value - recordId
        } else {
            _selectedIdSet.value + recordId
        }
        _selectedIdSet.value = newSet
    }

    fun selectAllOrCancel() {
        val allIds = _allRecordList.value.map { it.id }.toSet()
        val newSet = if (_selectedIdSet.value.size == allIds.size) emptySet<Long>() else allIds
        _selectedIdSet.value = newSet
    }

    fun deleteSelectedRecords() {
        viewModelScope.launch {
            repo.deleteSelectedRecords(_selectedIdSet.value.toList())
            loadRecords()
            _isMultiSelectMode.value = false
            _selectedIdSet.value = emptySet()
        }
    }

    // 停止播放（片段播放共用）
    fun stopPlaying() {
        audioPlayer.stop()
        _isPlaying.value = false
        _currentPlayingSegmentStartSec.value = null
    }

    // 播放单个声响事件片段（方案A：双参数 clipPath + startSecond）
    fun playAudioSegment(clipPath: String, startSecond: Double) {
        if (clipPath.isBlank()) return
        val clipFile = File(clipPath)
        if (!clipFile.exists()) return

        // 场景1：点击的是当前正在播放的项 → 停止播放，清空状态
        if (_isPlaying.value && _currentPlayingSegmentStartSec.value == startSecond) {
            audioPlayer.stop()
            _isPlaying.value = false
            _currentPlayingSegmentStartSec.value = null
            return
        }

        // 场景2：点击新的项 → 先停止旧播放，再切换到新片段
        audioPlayer.stop()

        // 先更新UI状态，确保高亮立刻显示
        _currentPlayingSegmentStartSec.value = startSecond
        _isPlaying.value = true

        try {
            audioPlayer.play(clipPath) {
                // 回调里增加校验：只有和当前播放项一致，才清空状态，避免旧回调干扰
                if (_currentPlayingSegmentStartSec.value == startSecond) {
                    _isPlaying.value = false
                    _currentPlayingSegmentStartSec.value = null
                }
            }
        } catch (ex: Exception) {
            Log.e("PlaySegment", "播放片段发生异常", ex)
            stopPlaying()
        }
    }
}
