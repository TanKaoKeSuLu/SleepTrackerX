package com.tkksl.sleeptracker.data.audio

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 将short PCM采样写出为16bit单声道Wav片段
 */
class EventClipWriter(
    private val sampleRate: Int,
    private val outputDir: File
) {
    init {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
    }

    /**
     * @param clipSamples 完整片段short采样数组（包含前置3秒历史+事件本体+后延静音）
     * @param clipFileName 输出文件名，例如 clip_001.wav
     * @return 生成的文件；发生IO异常返回null
     */
    fun writeWavClip(clipSamples: ShortArray, clipFileName: String): File? {
        if (clipSamples.isEmpty()) return null
        val outFile = File(outputDir, clipFileName)
        return try {
            FileOutputStream(outFile).use { fos ->
                val bytePerSample = 2
                val channels = 1
                val byteRate = sampleRate * channels * bytePerSample
                val totalAudioLen = clipSamples.size * bytePerSample
                val totalDataLen = totalAudioLen + 36

                // WAV header
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                header.put("RIFF".toByteArray())
                header.putInt(totalDataLen)
                header.put("WAVE".toByteArray())
                header.put("fmt ".toByteArray())
                header.putInt(16)
                header.putShort(1)
                header.putShort(channels.toShort())
                header.putInt(sampleRate)
                header.putInt(byteRate)
                header.putShort((channels * bytePerSample).toShort())
                header.putShort((bytePerSample * 8).toShort())
                header.put("data".toByteArray())
                header.putInt(totalAudioLen)
                fos.write(header.array())

                // PCM body
                val bodyBuf = ByteBuffer.allocate(clipSamples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                bodyBuf.asShortBuffer().put(clipSamples)
                fos.write(bodyBuf.array())
            }
            outFile
        } catch (e: Exception) {
            android.util.Log.e("ClipWriterDebug", "写clip失败", e)
            null
        }
    }
}
