package com.tkksl.sleeptracker.data.audio

import com.tkksl.sleeptracker.data.settings.RecordingConfig
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

class WavConverter(
    private val config: RecordingConfig
) {
    // 完整PCM转WAV（保留原有方法）
    fun convert(
        pcmFile: File,
        wavFile: File
    ) {
        FileInputStream(pcmFile).use { input ->
            FileOutputStream(wavFile).use { output ->
                val pcmSize = pcmFile.length()
                writeWavHeader(output, pcmSize)
                val buffer = ByteArray(1024)
                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    output.write(buffer, 0, read)
                }
            }
        }
    }

    // 截取指定时间段PCM生成WAV片段
    fun extractClip(
        pcmFile: File,
        outputWav: File,
        startSecond: Double,
        durationSecond: Double
    ): Boolean {
        if (!pcmFile.exists() || durationSecond <= 0) return false

        val bytesPerSecond = config.sampleRate * config.channelCount * config.bitDepth / 8
        val startByte = (startSecond * bytesPerSecond).toLong().coerceAtLeast(0L)
        var clipByteLength = (durationSecond * bytesPerSecond).toLong()

        // 起始位置超出文件，直接返回失败
        if (startByte >= pcmFile.length()) return false
        // 防止读取超出文件末尾
        clipByteLength = clipByteLength.coerceAtMost(pcmFile.length() - startByte)
        if (clipByteLength <= 0) return false

        return try {
            FileInputStream(pcmFile).use { fis ->
                val channel = fis.channel
                // FileChannel 随机定位，替代skip，大文件性能更稳
                channel.position(startByte)

                val buffer = ByteArray(8192)
                var remaining = clipByteLength

                FileOutputStream(outputWav).use { fos ->
                    writeWavHeader(fos, clipByteLength)
                    while (remaining > 0) {
                        val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                        val read = fis.read(buffer, 0, toRead)
                        if (read <= 0) break
                        fos.write(buffer, 0, read)
                        remaining -= read
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun writeWavHeader(
        output: FileOutputStream,
        pcmSize: Long
    ){
        val sampleRate = config.sampleRate
        val channels = config.channelCount
        val bitsPerSample = config.bitDepth

        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val totalDataLen = pcmSize + 36
        val header = ByteArray(44)

        header[0]='R'.code.toByte()
        header[1]='I'.code.toByte()
        header[2]='F'.code.toByte()
        header[3]='F'.code.toByte()
        writeInt(header, 4, totalDataLen.toInt())

        header[8]='W'.code.toByte()
        header[9]='A'.code.toByte()
        header[10]='V'.code.toByte()
        header[11]='E'.code.toByte()

        header[12]='f'.code.toByte()
        header[13]='m'.code.toByte()
        header[14]='t'.code.toByte()
        header[15]=' '.code.toByte()
        writeInt(header, 16, 16)

        writeShort(header, 20, 1)
        writeShort(header, 22, channels)
        writeInt(header, 24, sampleRate)
        writeInt(header, 28, byteRate)
        writeShort(header, 32, blockAlign)
        writeShort(header, 34, bitsPerSample)

        header[36]='d'.code.toByte()
        header[37]='a'.code.toByte()
        header[38]='t'.code.toByte()
        header[39]='a'.code.toByte()
        writeInt(header, 40, pcmSize.toInt())

        output.write(header)
    }

    private fun writeInt(buffer:ByteArray, offset:Int, value:Int){
        buffer[offset]=(value and 0xff).toByte()
        buffer[offset+1]=(value shr 8 and 0xff).toByte()
        buffer[offset+2]=(value shr 16 and 0xff).toByte()
        buffer[offset+3]=(value shr 24 and 0xff).toByte()
    }

    private fun writeShort(buffer:ByteArray, offset:Int, value:Int){
        buffer[offset]=(value and 0xff).toByte()
        buffer[offset+1]=(value shr 8 and 0xff).toByte()
    }
}
