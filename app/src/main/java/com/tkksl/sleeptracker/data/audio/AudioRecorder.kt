package com.tkksl.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.tkksl.sleeptracker.data.settings.RecordingConfig
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteOrder

class AudioRecorder(
    private val config: RecordingConfig,
    private val onError: ((String) -> Unit)? = null
) {
    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false
    private var recordingThread: Thread? = null

    private val sampleRate = config.sampleRate
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val shortBufferSize = minBufferSize / 2 // 16bit = 2字节/采样

    @SuppressLint("MissingPermission")
    fun startRecording(outputFile: File) {
        if (isRecording) return
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize
            )
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError?.invoke("AudioRecord初始化失败")
                audioRecord?.release()
                audioRecord = null
                return
            }
            audioRecord?.startRecording()
            isRecording = true
            recordingThread = Thread {
                FileOutputStream(outputFile).use { fos ->
                    val shortBuf = ShortArray(shortBufferSize)
                    while (isRecording) {
                        val readSamples = audioRecord?.read(shortBuf, 0, shortBuf.size) ?: 0
                        if (readSamples > 0) {
                            val byteBuf = java.nio.ByteBuffer.allocate(readSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
                            byteBuf.asShortBuffer().put(shortBuf, 0, readSamples)
                            fos.write(byteBuf.array())
                        }
                    }
                    fos.flush()
                }
            }
            recordingThread?.start()
        } catch (e: Exception) {
            onError?.invoke(e.message ?: "启动录音失败")
            release()
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        try {
            // 第一步：先停止硬件录音，唤醒阻塞的 read()，让线程快速退出循环
            audioRecord?.stop()
        } catch (_: Exception) {}
        // 第二步：再置停止标记
        isRecording = false
        try {
            // 第三步：延长等待至3秒，确保尾部数据全部写入文件，避免长录音截断
            recordingThread?.join(3000)
        } catch (_: Exception) {}
        recordingThread = null
        // 第四步：最后释放资源
        release()
    }

    private fun release() {
        audioRecord?.release()
        audioRecord = null
    }
}