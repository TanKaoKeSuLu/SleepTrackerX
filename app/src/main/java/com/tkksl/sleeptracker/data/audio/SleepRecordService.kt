package com.tkksl.sleeptracker.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tkksl.sleeptracker.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SleepRecordService : Service() {
    private var audioRecorder: AudioRecorder? = null
    private var currentFile: File? = null

    private var currentWavFile: File? = null
    private var recordStartTime: Long = 0L

    companion object {
        const val CHANNEL_ID = "sleep_record_channel"
        const val ACTION_START = "START_SLEEP_RECORD"
        const val ACTION_STOP = "STOP_SLEEP_RECORD"
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
                // 记录本次录音起始时间戳
                recordStartTime = System.currentTimeMillis()
                val file = createRecordingFile()
                currentFile = file
                audioRecorder = AudioRecorder()
                audioRecorder?.startRecording(file)
            }

            ACTION_STOP -> {
                audioRecorder?.stopRecording()
                audioRecorder = null

                currentFile?.let { pcm ->
                    val wavFile = createWavFile(pcm)
                    WavConverter().convert(pcm, wavFile)
                    currentWavFile = wavFile
                    println("WAV生成成功:${wavFile.absolutePath}")

                    // 发送录音完成广播，传递数据给ViewModel
                    val finishIntent = Intent("SLEEP_RECORD_FINISHED").apply {
                        putExtra("audioPath", wavFile.absolutePath)
                        putExtra("startTime", recordStartTime)
                        putExtra("endTime", System.currentTimeMillis())
                    }
                    sendBroadcast(finishIntent)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "睡眠记录",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager =
                getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}