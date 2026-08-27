package com.tkksl.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.tkksl.sleeptracker.data.settings.RecordingConfig
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteOrder

/**
 * 流式音频块回调；双路阶段：不影响原有PCM写文件
 * @param samples 原始16bit PCM short采样数组
 * @param relativeTimeMs 当前这批采样距离录音开始的相对毫秒
 */
fun interface AudioStreamCallback {
    fun onAudioChunk(samples: ShortArray, relativeTimeMs: Long)
}

class AudioRecorder(
    private val config: RecordingConfig,
    private val onError: ((String) -> Unit)? = null
) {
    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false
    private var recordingThread: Thread? = null

    // 新增：流式回调，可外部赋值，可为null
    var streamCallback: AudioStreamCallback? = null

    private val sampleRate = config.sampleRate
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    // 16bit PCM：2字节/采样，字节转short；保底最小1024采样，避免缓冲区过小
    private val shortBufferSize = (minBufferSize / 2).coerceAtLeast(1024)

    private var recordStartTimeMs: Long = 0L

    @SuppressLint("MissingPermission")
    fun startRecording(outputFile: File) {
        if (isRecording) return
        // 自动创建父目录，防止目录缺失写文件失败
        outputFile.parentFile?.mkdirs()
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
            recordStartTimeMs = System.currentTimeMillis()

            recordingThread = Thread {
                FileOutputStream(outputFile).use { fos ->
                    val shortBuf = ShortArray(shortBufferSize)
                    var totalReadSamples = 0L
                    while (isRecording) {
                        val readSamples = audioRecord?.read(shortBuf, 0, shortBuf.size) ?: 0
                        if (readSamples > 0) {
                            totalReadSamples += readSamples
                            // 原有逻辑：写入PCM文件，完整保留
                            val byteBuf = java.nio.ByteBuffer.allocate(readSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
                            byteBuf.asShortBuffer().put(shortBuf, 0, readSamples)
                            fos.write(byteBuf.array())

                            // ======新增：流式回调输出，双路并行======
                            val chunkCopy = shortBuf.copyOfRange(0, readSamples)
                            // 使用采样总数计算相对时间，彻底规避系统时钟漂移
                            val relativeMs = (totalReadSamples * 1000L) / sampleRate
                            streamCallback?.onAudioChunk(chunkCopy, relativeMs)
                        } else if (readSamples < 0) {
                            // AudioRecord底层发生错误，退出循环
                            onError?.invoke("AudioRecord read error code:$readSamples")
                            break
                        }
                        // readSamples ==0：无有效采样，继续循环
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
        // 1.先标记停止，线程会消费完缓冲区剩余采样再退出
        isRecording = false
        try {
            // 2.等待线程执行完毕，最长等待3秒，保证尾部数据全部处理
            recordingThread?.join(3000)
        } catch (_: Exception) {}

        // 3.线程完全结束之后，再停止硬件录音，释放资源
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        recordingThread = null
        release()
    }

    private fun release() {
        audioRecord?.release()
        audioRecord = null
    }
}
