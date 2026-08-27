package com.tkksl.sleeptracker.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tkksl.data.audio.AudioRecorder
import com.tkksl.data.audio.AudioStreamCallback
import com.tkksl.sleeptracker.R
import com.tkksl.sleeptracker.data.analyzer.AudioEvent
import com.tkksl.sleeptracker.data.settings.RecordingQuality
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class SleepRecordService : Service() {
    private var audioRecorder: AudioRecorder? = null
    private var streamingAnalyzer: StreamingAudioAnalyzer? = null

    private var currentFile: File? = null
    private var recordStartTime: Long = 0L
    private var recordEndTime: Long = 0L
    private var currentSampleRate: Int = 44100

    companion object {
        const val CHANNEL_ID = "sleep_record_channel"
        const val ACTION_START = "START_SLEEP_RECORD"
        const val ACTION_STOP = "STOP_SLEEP_RECORD"
        const val EXTRA_RECORD_QUALITY = "EXTRA_RECORD_QUALITY"

        const val ACTION_STREAM_ANALYSIS_RESULT = "ACTION_STREAM_ANALYSIS_RESULT"
        const val EXTRA_RECORD_START = "EXTRA_RECORD_START"
        const val EXTRA_RECORD_END = "EXTRA_RECORD_END"
        const val EXTRA_EVENT_COUNT = "EXTRA_EVENT_COUNT"
        const val EXTRA_PCM_FILE_PATH = "EXTRA_PCM_FILE_PATH"

        const val EXTRA_SAMPLE_RATE = "EXTRA_SAMPLE_RATE"
        const val EXTRA_TOTAL_NOISE_DURATION = "EXTRA_TOTAL_NOISE_DURATION"
        const val EXTRA_AVG_VOLUME = "EXTRA_AVG_VOLUME"
        const val EXTRA_MAX_WHOLE_VOLUME = "EXTRA_MAX_WHOLE_VOLUME"

        private const val MIN_RECORD_SECONDS = 10.0
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder?.stopRecording()
        streamingAnalyzer?.finishAll()
        audioRecorder = null
        streamingAnalyzer = null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(
                    1,
                    createNotification()
                )
                recordStartTime = System.currentTimeMillis()
                val file = createRecordingFile()
                currentFile = file

                val qualityName = intent.getStringExtra(EXTRA_RECORD_QUALITY) ?: RecordingQuality.NORMAL.name
                val recordQuality = RecordingQuality.valueOf(qualityName)
                val recordConfig = recordQuality.toConfig()
                currentSampleRate = recordConfig.sampleRate

                audioRecorder = AudioRecorder(recordConfig,onError = {
                    android.util.Log.e("SleepRecordService","录音出错:$it")
                    val stopIntent = Intent(this,SleepRecordService::class.java).apply {
                        action = ACTION_STOP
                    }
                    startService(stopIntent)
                })

                val clipDir = File(getExternalFilesDir(null), "clips")
                val clipWriter = EventClipWriter(recordConfig.sampleRate, clipDir)

                streamingAnalyzer = StreamingAudioAnalyzer(recordConfig.sampleRate, clipWriter)

                audioRecorder?.streamCallback = AudioStreamCallback { samples, relativeTimeMs ->
                    android.util.Log.d("StreamChunk", "chunk size=${samples.size}, relMs=$relativeTimeMs")
                    streamingAnalyzer?.processChunk(samples, relativeTimeMs)
                }

                audioRecorder?.startRecording(file)
                streamingAnalyzer?.start(recordStartTime)
            }

            ACTION_STOP -> {
                try {
                    recordEndTime = System.currentTimeMillis()

                    audioRecorder?.stopRecording()
                    streamingAnalyzer?.finishAll()

                    val events: List<AudioEvent> = streamingAnalyzer?.detectedEvents ?: emptyList()
                    val totalSec = (recordEndTime - recordStartTime) / 1000.0

                    if(totalSec < MIN_RECORD_SECONDS){
                        android.util.Log.d("SleepRecordService","录制时长 $totalSec s < ${MIN_RECORD_SECONDS}s，丢弃本次录制，清理文件")
                        // 删除原始pcm
                        currentFile?.let {pcm->
                            if(pcm.exists()) pcm.delete()
                        }
                        // 删除已经生成的clip片段
                        events.forEach {audioEvent->
                            if(audioEvent.clipPath.isNotBlank()){
                                val clip = File(audioEvent.clipPath)
                                if(clip.exists()) clip.delete()
                            }
                        }
                        audioRecorder = null
                        streamingAnalyzer = null
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        // >=10秒，正常走广播分发
                        val streamResultIntent = Intent(ACTION_STREAM_ANALYSIS_RESULT).apply {
                            setPackage(packageName)
                            putExtra(EXTRA_RECORD_START, recordStartTime)
                            putExtra(EXTRA_RECORD_END, recordEndTime)
                            putExtra(EXTRA_EVENT_COUNT, events.size)
                            putExtra(EXTRA_PCM_FILE_PATH,currentFile?.absolutePath)

                            putExtra(EXTRA_SAMPLE_RATE, currentSampleRate)
                            putExtra(EXTRA_TOTAL_NOISE_DURATION, streamingAnalyzer?.totalNoiseDuration ?:0.0)
                            putExtra(EXTRA_AVG_VOLUME, streamingAnalyzer?.averageVolume ?:0.0)
                            putExtra(EXTRA_MAX_WHOLE_VOLUME, streamingAnalyzer?.maxWholeVolume ?:0.0)

                            val eventBundles = ArrayList<Bundle>()
                            for(e in events){
                                val b = Bundle().apply {
                                    putDouble("startSecond", e.startSecond)
                                    putDouble("endSecond", e.endSecond)
                                    putDouble("peakTime", e.peakTime)
                                    putDouble("maxVolume", e.maxVolume)
                                    putInt("typeOrdinal", e.type.ordinal)
                                    putString("clipPath", e.clipPath)
                                }
                                eventBundles.add(b)
                            }
                            putParcelableArrayListExtra("eventListBundle", eventBundles)
                        }
                        sendBroadcast(streamResultIntent)

                        audioRecorder = null
                        streamingAnalyzer = null
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }

                }catch (ex:Exception){
                    android.util.Log.e("SleepRecordService","停止录音异常",ex)
                    //异常分支也要清理pcm，避免残留
                    currentFile?.let {pcm-> if(pcm.exists()) pcm.delete() }
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }

        }
        return START_NOT_STICKY
    }

    private fun createRecordingFile(): File {
        val dir = File(getExternalFilesDir(null), "recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val format = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        return File(dir, "${format.format(Date())}.pcm")
    }

    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("SleepTracker")
            .setContentText("正在记录睡眠")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)

        val notification = builder.build()
        // 移除 FLAG_IMMUTABLE，只保留常驻标记
        notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT
        return notification
    }


    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "睡眠记录",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager =
            getSystemService(NotificationManager::class.java) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
