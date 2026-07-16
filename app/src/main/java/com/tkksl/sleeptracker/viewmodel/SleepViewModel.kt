package com.tkksl.sleeptracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.tkksl.sleeptracker.data.audio.AudioRecorder
import com.tkksl.sleeptracker.data.audio.WavConverter
import com.tkksl.sleeptracker.data.audio.AudioPlayer
import com.tkksl.sleeptracker.data.local.DatabaseProvider
import com.tkksl.sleeptracker.data.model.SleepRecord
import com.tkksl.sleeptracker.data.repository.SleepRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SleepViewModel(application: Application) : AndroidViewModel(application) {
    private val audioRecorder = AudioRecorder()
    // 新增wav转换器实例
    private val wavConverter = WavConverter()
    // 新增播放器实例
    private val audioPlayer = AudioPlayer()
    private val appContext = getApplication<Application>()

    private val sleepDao = DatabaseProvider.getDatabase(appContext).sleepDao()
    private val repo = SleepRepository(sleepDao)

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
    private var currentAudioFile: File? = null

    init {
        loadRecords()
    }

    /**
     * 创建外置私有录音文件目录 + 时间戳pcm文件
     */
    private fun createRecordingFile(): File {
        val parentDir = File(appContext.getExternalFilesDir(null), "recordings")
        if (!parentDir.exists()) parentDir.mkdirs()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val fileName = "${dateFormat.format(Date())}.pcm"
        return File(parentDir, fileName)
    }

    fun toggleRecording() {
        if (!isRecording) {
            // 开始录音
            val audioFile = createRecordingFile()
            currentAudioFile = audioFile
            audioRecorder.startRecording(audioFile)

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
            // 停止录音
            audioRecorder.stopRecording()
            timerJob?.cancel()
            isRecording = false

            val endTs = System.currentTimeMillis()
            val durationSec = elapsedSeconds
            val pcmFile = currentAudioFile

            // 少于10秒直接丢弃音频，不入库、不生成wav
            if (durationSec < 10) {
                elapsedSeconds = 0L
                pcmFile?.delete()
                currentAudioFile = null
                return
            }

            // 由pcm生成同文件名wav
            var wavFile: File? = null
            pcmFile?.let { pcm ->
                wavFile = File(pcm.parent, pcm.nameWithoutExtension + ".wav")
                wavConverter.convert(pcm, wavFile!!)
            }
            // 数据库存储wav路径
            val audioPath = wavFile?.absolutePath

            val quality = when {
                durationSec >= 6 * 3600 -> "Good"
                durationSec >= 3 * 3600 -> "Normal"
                else -> "Poor"
            }

            val record = SleepRecord(
                startTime = startTs,
                endTime = endTs,
                duration = durationSec,
                quality = quality,
                audioPath = audioPath
            )

            viewModelScope.launch {
                repo.insertSleepRecord(record)
                loadRecords()
            }

            elapsedSeconds = 0L
            currentAudioFile = null
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