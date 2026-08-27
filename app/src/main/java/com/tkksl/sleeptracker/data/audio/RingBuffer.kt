package com.tkksl.sleeptracker.data.audio

/**
 * short 环形缓冲区，用于保存最近N秒PCM采样
 * @param sampleRate 采样率
 * @param keepSeconds 需要在内存保留的音频时长，固定3.0
 */
class RingBuffer(
    private val sampleRate: Int,
    private val keepSeconds: Double = 3.0
) {
    private val capacity: Int = (sampleRate * keepSeconds).toInt()
    private val buffer = ShortArray(capacity)

    // 下一个写入位置
    private var writePos = 0
    // 当前有效采样数量
    private var validCount = 0

    /**
     * 写入一块short采样
     */
    fun write(chunk: ShortArray) {
        for (s in chunk) {
            buffer[writePos] = s
            writePos = (writePos + 1) % capacity
            if (validCount < capacity) {
                validCount++
            }
        }
    }

    /**
     * 读取缓冲区全部有效历史采样
     * 返回顺序：时间从旧 → 新
     */
    fun readAll(): ShortArray {
        if (validCount == 0) return ShortArray(0)
        val result = ShortArray(validCount)
        if (validCount <= writePos) {
            // 没有环绕
            buffer.copyInto(result, 0, 0, validCount)
        } else {
            // 已经绕环，分两段拷贝
            val part1Len = capacity - writePos
            buffer.copyInto(result, 0, writePos, capacity)
            buffer.copyInto(result, part1Len, 0, writePos)
        }
        return result
    }

    /**
     * 清空缓冲区，录音开始调用
     */
    fun clear() {
        writePos = 0
        validCount = 0
    }
}
