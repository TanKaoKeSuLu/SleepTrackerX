package com.tkksl.sleeptracker.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.tkksl.sleeptracker.data.analyzer.SleepAnalysis
import com.tkksl.sleeptracker.data.analyzer.SleepAnalyzer
import com.tkksl.sleeptracker.data.analyzer.SleepScoreCalculator
import com.tkksl.sleeptracker.data.audio.WavConverter
import com.tkksl.sleeptracker.data.settings.RecordingConfig
import com.tkksl.sleeptracker.utils.SettingsSp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

class SleepViewModel(application: Application) : AndroidViewModel(application) {
    private val audioPlayer = AudioPlayer()
    private val appContext = getApplication<Application>()
    private val sleepAnalyzer = SleepAnalyzer()

    private val sleepDao = DatabaseProvider.getDatabase(appContext).sleepDao()
    private val repo = SleepRepository(sleepDao)

    // 防重：已处理文件路径缓存，5秒内同一文件不再重复入库
    private val handledFileCache = ConcurrentHashMap<String, Long>()

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

    // ========== Compose State 统一规范写法 ==========
    private val _currentRecordQuality = mutableStateOf(SettingsSp.getRecordQuality(appContext))
    val currentRecordQuality: RecordingQuality
        get() = _currentRecordQuality.value

    private val _elapsedSeconds = mutableLongStateOf(0L)
    val elapsedSeconds: Long
        get() = _elapsedSeconds.value

    private val _isRecording = mutableStateOf(false)
    val isRecording: Boolean
        get() = _isRecording.value

    private val _latestRecord = mutableStateOf<SleepRecord?>(null)
    val latestRecord: SleepRecord?
        get() = _latestRecord.value

    private val _allRecordList = mutableStateOf<List<SleepRecord>>(emptyList())
    val allRecordList: List<SleepRecord>
        get() = _allRecordList.value

    // Room原始查询结果（数据库实体，仅内部中转）
    private val _currentFullRecord = mutableStateOf<SleepRecordWithEvents?>(null)
    // 新增对外只读属性，给SleepDetailScreen页面访问
    val currentFullRecord: SleepRecordWithEvents?
        get() = _currentFullRecord.value

    // UI专用：转换完成、带波形数据的AudioEvent列表
    private val _detailEventList = mutableStateOf<List<AudioEvent>>(emptyList())
    val detailEventList: List<AudioEvent>
        get() = _detailEventList.value

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: Boolean
        get() = _isPlaying.value

    // 新增：当前播放的片段起始秒，null代表没有播放片段
    private val _currentPlayingSegmentStartSec = mutableStateOf<Double?>(null)
    val currentPlayingSegmentStartSec: Double?
        get() = _currentPlayingSegmentStartSec.value

    private val _isMultiSelectMode = mutableStateOf(false)
    val isMultiSelectMode: Boolean
        get() = _isMultiSelectMode.value

    private val _selectedIdSet = mutableStateOf(setOf<Long>())
    val selectedIdSet: Set<Long>
        get() = _selectedIdSet.value

    private var timerJob: Job? = null
    private var startTs = 0L

    init {
        loadRecords()
    }

    override fun onCleared() {
        audioPlayer.release()
    }

    // 外部全局广播回调入口（仅Application调用）
    fun handleRecordFinish(audioPath: String, pcmPath: String, startTime: Long, endTime: Long) {
        val now = System.currentTimeMillis()
        val lastHandleTime = handledFileCache[audioPath] ?: 0L
        if (now - lastHandleTime < 5000) return
        handledFileCache[audioPath] = now

        if (pcmPath.isEmpty() || startTime <= 0 || endTime <= startTime) return
        val durationSecTotal = (endTime - startTime) / 1000.0

        // 小于10秒直接丢弃，同时删除PCM文件
        if (durationSecTotal < 10.0) {
            File(pcmPath).takeIf { it.exists() }?.delete()
            return
        }

        // IO heavy 任务全部调度至IO线程，增加全局异常兜底
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sampleRate = when (_currentRecordQuality.value) {
                    RecordingQuality.LOW -> 16000
                    RecordingQuality.NORMAL -> 44100
                    RecordingQuality.HIGH -> 48000
                }

                val pcmFile = File(pcmPath)
                val fileSize = if (pcmFile.exists()) pcmFile.length() else 0L

                // 步骤1：执行音频分析
                val (analysisResult, cutOffset) = if (pcmFile.exists()) {
                    sleepAnalyzer.analyze(pcmFile = pcmFile, sampleRate)
                } else {
                    Pair(
                        SleepAnalysis(
                            averageVolume = 0.0,
                            maxVolume = 0.0,
                            noiseCount = 0,
                            totalNoiseDuration = 0.0,
                            score = 100,
                            events = emptyList()
                        ),
                        0.0
                    )
                }

                // 修正事件时间偏移
                val fixedEvents = analysisResult.events.map { e ->
                    e.copy(
                        startSecond = e.startSecond - cutOffset,
                        endSecond = e.endSecond - cutOffset,
                        peakTime = e.peakTime - cutOffset
                    )
                }

                val noiseCount = fixedEvents.size
                val avgVol = analysisResult.averageVolume
                val maxVolWhole = analysisResult.maxVolume

                val sleepScore = SleepScoreCalculator.calculate(
                    totalSecond = durationSecTotal,
                    noiseDuration = analysisResult.totalNoiseDuration,
                    noiseEventCount = noiseCount,
                    maxVolume = maxVolWhole
                )

                // ========== 批量生成事件片段 ==========
                val clipsDir = File(pcmFile.parent, "clips")
                if (!clipsDir.exists()) clipsDir.mkdirs()

                val recordConfig = RecordingConfig(
                    sampleRate = sampleRate,
                    bitDepth = 16,
                    channelCount = 1
                )
                val wavConverter = WavConverter(recordConfig)
                val clipTotalDuration = 15.0 // 每个片段总时长15秒
                val preOffset = 2.0 // 事件起点前预留2秒

                val eventEntities = mutableListOf<AudioEventEntity>()
                for (audioEvent in fixedEvents) {
                    try {
                        // 计算片段起始时间，不小于0
                        val clipStart = maxOf(0.0, audioEvent.startSecond - preOffset)
                        // 生成片段文件名，用时间戳保证唯一
                        val clipFileName = "clip_${(startTime + clipStart * 1000).toLong()}.wav"
                        val clipFile = File(clipsDir, clipFileName)

                        // 截取片段并转WAV
                        wavConverter.extractClip(
                            pcmFile = pcmFile,
                            outputWav = clipFile,
                            startSecond = clipStart,
                            durationSecond = clipTotalDuration
                        )

                        // 波形JSON序列化
                        val jsonArr = JSONArray()
                        audioEvent.waveform.forEach { jsonArr.put(it) }
                        val waveJson = jsonArr.toString()

                        eventEntities.add(
                            AudioEventEntity(
                                sleepRecordId = 0,
                                startSecond = audioEvent.startSecond,
                                endSecond = audioEvent.endSecond,
                                maxVolume = audioEvent.maxVolume,
                                peakTime = audioEvent.peakTime,
                                type = audioEvent.type,
                                waveformJson = waveJson,
                                clipPath = clipFile.absolutePath
                            )
                        )
                    } catch (e: Exception) {
                        // 单个片段截取失败：跳过当前片段，不阻断整体流程
                        e.printStackTrace()
                    }
                }

                val record = SleepRecord(
                    startTime = startTime,
                    endTime = endTime,
                    duration = durationSecTotal.toLong(),
                    quality = when {
                        durationSecTotal >= 6 * 3600 -> "Good"
                        durationSecTotal >= 3 * 3600 -> "Normal"
                        else -> "Poor"
                    },
                    audioPath = "", // 不保存完整录音路径
                    fileSize = fileSize,
                    sampleRate = sampleRate,
                    sleepScore = sleepScore,
                    noiseEventCount = noiseCount,
                    avgVolume = avgVol,
                    maxWholeVolume = maxVolWhole
                )

                val insertSuccess = repo.insertFullSleepRecord(record, eventEntities)
                if (insertSuccess) {
                    // 入库成功后删除原始大PCM，仅保留小片段
                    pcmFile.takeIf { it.exists() }?.delete()
                }
                loadRecords()
            } catch (globalEx: Exception) {
                // 顶层兜底：哪怕整体解析全部异常，也要创建一条空事件记录，不丢失整夜睡眠基础记录
                globalEx.printStackTrace()
                val record = SleepRecord(
                    startTime = startTime,
                    endTime = endTime,
                    duration = durationSecTotal.toLong(),
                    quality = "Poor",
                    audioPath = "",
                    fileSize = File(pcmPath).length(),
                    sampleRate = 0,
                    sleepScore = 50,
                    noiseEventCount = 0,
                    avgVolume = 0.0,
                    maxWholeVolume = 0.0
                )
                repo.insertFullSleepRecord(record, emptyList())
                File(pcmPath).takeIf { it.exists() }?.delete()
                loadRecords()
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
        val intent = Intent(appContext, SleepRecordService::class.java).apply {
            action = SleepRecordService.ACTION_START
            putExtra(SleepRecordService.EXTRA_RECORD_QUALITY, _currentRecordQuality.value.name)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appContext.startForegroundService(intent)
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

        val stopIntent = Intent(appContext, SleepRecordService::class.java).apply {
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

    // 音频播放控制
    fun playCurrentRecord() {
        val fullRecord = _currentFullRecord.value ?: return
        val path = fullRecord.record.audioPath ?: return
        if (path.isBlank()) return

        if (_isPlaying.value) {
            audioPlayer.stop()
            _isPlaying.value = false
            _currentPlayingSegmentStartSec.value = null
            return
        }
        _currentPlayingSegmentStartSec.value = null
        audioPlayer.play(path) {
            _isPlaying.value = false
            _currentPlayingSegmentStartSec.value = null
        }
        _isPlaying.value = true
    }

    fun stopPlaying() {
        audioPlayer.stop()
        _isPlaying.value = false
        _currentPlayingSegmentStartSec.value = null
    }

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

        audioPlayer.play(clipPath) {
            // 回调里增加校验：只有和当前播放项一致，才清空状态，避免旧回调干扰
            if (_currentPlayingSegmentStartSec.value == startSecond) {
                _isPlaying.value = false
                _currentPlayingSegmentStartSec.value = null
            }
        }
    }
}
