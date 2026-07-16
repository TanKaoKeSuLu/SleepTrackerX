package com.tkksl.sleeptracker.data.audio

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class WavConverter {
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

    private fun writeWavHeader(
        output: FileOutputStream,
        pcmSize: Long
    ) {
        val sampleRate = 44100
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = pcmSize + 36
        val header = ByteArray(44)

        // RIFF
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        writeInt(header, 4, totalDataLen.toInt())

        // WAVE
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // fmt
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        writeInt(header, 16, 16)

        // PCM format
        writeShort(header, 20, 1)
        // channels
        writeShort(header, 22, channels)
        // sample rate
        writeInt(header, 24, sampleRate)
        // byte rate
        writeInt(header, 28, byteRate)
        // block align
        writeShort(header, 32, channels * bitsPerSample / 8)
        // bits
        writeShort(header, 34, bitsPerSample)

        // data
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        writeInt(header, 40, pcmSize.toInt())

        output.write(header)
    }

    private fun writeInt(
        buffer: ByteArray,
        offset: Int,
        value: Int
    ) {
        buffer[offset] = (value and 0xff).toByte()
        buffer[offset + 1] = (value shr 8 and 0xff).toByte()
        buffer[offset + 2] = (value shr 16 and 0xff).toByte()
        buffer[offset + 3] = (value shr 24 and 0xff).toByte()
    }

    private fun writeShort(
        buffer: ByteArray,
        offset: Int,
        value: Int
    ) {
        buffer[offset] = (value and 0xff).toByte()
        buffer[offset + 1] = (value shr 8 and 0xff).toByte()
    }
}