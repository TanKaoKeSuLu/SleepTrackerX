package com.tkksl.sleeptracker.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream

class AudioRecorder {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        channelConfig,
        audioFormat
    )

    @SuppressLint("MissingPermission")
    fun startRecording(outputFile: File) {
        if (isRecording) return

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        recordingThread = Thread {
            val buffer = ByteArray(bufferSize)
            val outputStream = FileOutputStream(outputFile)

            while (isRecording) {
                val readSize = audioRecord?.read(
                    buffer,
                    0,
                    buffer.size
                ) ?: 0

                if (readSize > 0) {
                    outputStream.write(buffer, 0, readSize)
                }
            }

            outputStream.flush()
            outputStream.close()
        }
        recordingThread?.start()
    }

    fun stopRecording() {
        if (!isRecording) return

        // 1. 标记循环退出
        isRecording = false
        // 2. 先停止AudioRecord硬件读取，read会立刻返回，线程快速退出
        audioRecord?.stop()
        // 3. 等待录音线程收尾写完剩余数据、关闭文件流
        recordingThread?.join()
        recordingThread = null
        // 4. 最后释放硬件资源
        audioRecord?.release()
        audioRecord = null
    }
}