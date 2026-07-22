package com.tkksl.sleeptracker.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.tkksl.sleeptracker.data.settings.RecordingConfig
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteOrder
import java.nio.ShortBuffer

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
    private val shortBufferSize = minBufferSize / 2 // 16bit = 2字节/采样，ShortArray长度减半

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
                            // 每次读取新建ByteBuffer，彻底清除上一轮残留字节
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
        isRecording = false
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        try {
            recordingThread?.join(1000)
        } catch (_: Exception) {}
        recordingThread = null
        release()
    }

    private fun release() {
        audioRecord?.release()
        audioRecord = null
    }
}