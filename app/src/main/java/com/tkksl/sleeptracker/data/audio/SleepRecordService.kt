package com.tkksl.sleeptracker.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import androidx.core.app.NotificationCompat
import com.tkksl.sleeptracker.R
import com.tkksl.sleeptracker.data.settings.RecordingConfig
import com.tkksl.sleeptracker.data.settings.RecordingQuality
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SleepRecordService : Service() {
    private var audioRecorder: AudioRecorder? = null
    private var currentFile: File? = null
    private var currentWavFile: File? = null
    private var recordStartTime: Long = 0L
    // 新增：缓存当前录音配置
    private var currentConfig: RecordingConfig? = null
    // 防重标记，防止多次停止多次发广播
    private var hasSendFinishBroadcast = false

    companion object {
        const val CHANNEL_ID = "sleep_record_channel"
        const val ACTION_START = "START_SLEEP_RECORD"
        const val ACTION_STOP = "STOP_SLEEP_RECORD"
        const val EXTRA_RECORD_QUALITY = "EXTRA_RECORD_QUALITY"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
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
                // 缓存本次录音配置
                currentConfig = recordConfig

                audioRecorder = AudioRecorder(recordConfig)
                audioRecorder?.startRecording(file)
                hasSendFinishBroadcast = false
            }

            ACTION_STOP -> {
                if (hasSendFinishBroadcast) return@onStartCommand START_NOT_STICKY
                audioRecorder?.stopRecording()
                audioRecorder = null

                // 新增：等待IO缓冲区写入完整PCM文件
                runBlocking { delay(500) }

                currentFile?.let { pcm ->
                    val wavFile = createWavFile(pcm)
                    // 判空安全调用，传入录音配置
                    currentConfig?.let { config ->
                        // 构造实例时传入config，convert只传2个文件
                        WavConverter(config).convert(pcm, wavFile)
                    }
                    currentWavFile = wavFile

                    val finishIntent = Intent("SLEEP_RECORD_FINISHED").apply {
                        putExtra("audioPath", wavFile.absolutePath)
                        putExtra("pcmPath", pcm.absolutePath)
                        putExtra("startTime", recordStartTime)
                        putExtra("endTime", System.currentTimeMillis())
                    }
                    sendBroadcast(finishIntent)
                    hasSendFinishBroadcast = true
                }

                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
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
    private fun createWavFile(pcmFile: File): File {
        return File(
            pcmFile.parent,
            pcmFile.nameWithoutExtension + ".wav"
        )
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("SleepTracker")
            .setContentText("正在记录睡眠")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "睡眠记录",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager =
            getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}