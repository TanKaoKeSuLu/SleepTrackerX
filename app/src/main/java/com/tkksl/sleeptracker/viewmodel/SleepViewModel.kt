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
import com.tkksl.sleeptracker.utils.SettingsSp
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

        if(audioPath.isEmpty() || startTime <= 0 || endTime <= startTime) return
        val durationSecTotal = (endTime - startTime) / 1000.0
        if(durationSecTotal < 3) return

        val sampleRate = when(_currentRecordQuality.value) {
            RecordingQuality.LOW -> 16000
            RecordingQuality.NORMAL -> 44100
            RecordingQuality.HIGH -> 48000
        }

        // 补全缺失文件变量定义
        val wavFile = File(audioPath)
        val fileSize = if(wavFile.exists()) wavFile.length() else 0L
        val originalPcm = File(pcmPath)

        val (analysisResult, cutOffset) = if(originalPcm.exists()) {
            sleepAnalyzer.analyze(pcmFile = originalPcm, sampleRate)
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

        // 统一修正事件时间偏移
        val fixedEvents = analysisResult.events.map { e ->
            e.copy(
                startSecond = e.startSecond - cutOffset,
                endSecond = e.endSecond - cutOffset,
                peakTime = e.peakTime - cutOffset
            )
        }

        val noiseCount = fixedEvents.size
        // 直接使用分析结果自带音量，不再遍历事件集合计算
        val avgVol = analysisResult.averageVolume
        val maxVolWhole = analysisResult.maxVolume

        // 调用外部评分工具类
        val sleepScore = SleepScoreCalculator.calculate(
            totalSecond = durationSecTotal,
            noiseDuration = analysisResult.totalNoiseDuration,
            noiseEventCount = noiseCount,
            maxVolume = maxVolWhole
        )

        // 移除 .name，直接传枚举对象匹配AudioEventEntity.type: AudioType
        val eventEntities = fixedEvents.map { audioEvent ->
            // 将波形List<Float>转为JSON字符串存入waveformJson
            val jsonArr = JSONArray()
            audioEvent.waveform.forEach { jsonArr.put(it) }
            val waveJson = jsonArr.toString()

            AudioEventEntity(
                sleepRecordId = 0,
                startSecond = audioEvent.startSecond,
                endSecond = audioEvent.endSecond,
                maxVolume = audioEvent.maxVolume,
                peakTime = audioEvent.peakTime,
                type = audioEvent.type,
                waveformJson = waveJson
            )
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
            audioPath = audioPath,
            fileSize = fileSize,
            sampleRate = sampleRate,
            sleepScore = sleepScore,
            noiseEventCount = noiseCount, // 修正参数名称
            avgVolume = avgVol,
            maxWholeVolume = maxVolWhole
        )

        viewModelScope.launch {
            repo.insertFullSleepRecord(record, eventEntities)
            loadRecords()
        }
    }

    // 切换录音质量，持久化保存
    fun setRecordingQuality(quality: RecordingQuality) {
        _currentRecordQuality.value = quality
        SettingsSp.saveRecordQuality(appContext, quality)
    }

    // 启动/停止录音服务
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
        val stopIntent = Intent(appContext, SleepRecordService::class.java).apply {
            action = SleepRecordService.ACTION_STOP
        }
        appContext.startService(stopIntent)
        timerJob?.cancel()
        _isRecording.value = false
        _elapsedSeconds.value = 0L
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
            // 数据库实体 -> 解析波形JSON，转为UI可用AudioEvent
            val uiEventList = rawRecordWrap.events.map { entity ->
                val waveData = parseWaveformJson(entity.waveformJson)
                AudioEvent(
                    startSecond = entity.startSecond,
                    endSecond = entity.endSecond,
                    peakTime = entity.peakTime,
                    maxVolume = entity.maxVolume,
                    waveform = waveData,
                    type = entity.type
                )
            }
            // UI直接读取这个state，自带完整波形数据
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

        if (_isPlaying.value) {
            audioPlayer.stop()
            _isPlaying.value = false
            return
        }
        audioPlayer.play(path) {
            _isPlaying.value = false
        }
        _isPlaying.value = true
    }

    fun stopPlaying() {
        audioPlayer.stop()
        _isPlaying.value = false
    }
    fun playAudioSegment(targetSecond: Double) {
        val fullRecord = _currentFullRecord.value ?: return
        val audioPath = fullRecord.record.audioPath ?: return
        audioPlayer.playSegment(audioPath, (targetSecond * 1000).toLong(), 15000) {
            _isPlaying.value = false
        }
        _isPlaying.value = true
    }
}