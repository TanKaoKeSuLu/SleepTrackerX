package com.tkksl.sleeptracker.data.analyzer

import android.util.Log
import java.io.File
import java.io.FileInputStream
import kotlin.math.sqrt


class SleepAnalyzer {
    private val TAG = "SleepAnalyzerDebug"
    /**
     * 最低检测阈值（大幅降低，更容易捕捉微弱声响）
     */
    private val MIN_THRESHOLD = 150.0

    /**
     * 最高检测阈值
     */
    private val MAX_THRESHOLD = 5000.0

    /**
     * 声音持续时间低于这个值认为无意义（缩短，捕捉短声响）
     */
    private val MIN_EVENT_DURATION = 0.2

    /**
     * 两个声音间隔小于这个值进行合并
     */
    private val MERGE_GAP = 1.5

    /**
     * 每个声响事件压缩后保留的波形采样点数
     */
    private val WAVEFORM_POINTS = 40

    /**
     * PCM分析
     *
     * 返回:
     * SleepAnalysis
     * 开头静音时间
     */
    fun analyze(
        pcmFile: File,
        sampleRate: Int
    ): Pair<SleepAnalysis, Double> {
        val volumes = mutableListOf<Pair<Double, Double>>()

        var totalVolume = 0.0
        var sampleCount = 0L
        var maxVolume = 0.0

        /*
         * 第一阶段:
         * 读取PCM
         */
        FileInputStream(pcmFile).use { input ->
            val buffer = ByteArray(2048)
            var frameIndex = 0L

            while (true) {
                val read = input.read(buffer)
                if (read <= 0)
                    break

                val volume = calculateVolume(buffer, read)
                val time = frameIndex * (read / 2.0) / sampleRate

                volumes.add(Pair(time, volume))
                totalVolume += volume
                sampleCount++

                if (volume > maxVolume) {
                    maxVolume = volume
                }
                frameIndex++
            }
        }

        Log.d(TAG, "PCM读取完成，总采样帧数：${volumes.size}，全局最大音量：$maxVolume")
        if (volumes.isEmpty()) {
            Log.e(TAG, "PCM文件为空，无任何音量数据")
            // 空数据，补全totalNoiseDuration，全部具名参数
            return Pair(
                SleepAnalysis(
                    averageVolume = 0.0,
                    maxVolume = 0.0,
                    noiseCount = 0,
                    totalNoiseDuration = 0.0,
                    score = 100,
                    events = emptyList()
                ),
                0.0
            )
        }

        /*
         * 第二阶段:
         * 计算动态阈值
         */
        val threshold = calculateDynamicThreshold(volumes)
        Log.d(TAG, "动态检测阈值：$threshold")

        /*
         * 第三阶段:
         * 检测声音事件
         */
        val rawEvents = detectEvents(volumes, threshold)
        Log.d(TAG, "原始检测声响数量：${rawEvents.size}")

        /*
         * 第四阶段:
         * 合并事件（当前暂不处理合并波形，下一阶段修复merge逻辑）
         */
        val mergedEvents = mergeEvents(rawEvents)
        Log.d(TAG, "合并后最终声响数量：${mergedEvents.size}")
        val totalNoiseDuration = mergedEvents.sumOf { it.endSecond - it.startSecond }

        val avgVolume = totalVolume / sampleCount.toDouble()

        /*
         * 找第一次有效声音
         */
        val firstSound = mergedEvents.firstOrNull()?.startSecond ?: 0.0

        val analysis = SleepAnalysis(
            averageVolume = avgVolume,
            maxVolume = maxVolume,
            noiseCount = mergedEvents.size,
            totalNoiseDuration = totalNoiseDuration,
            score = 100,
            events = mergedEvents
        )

        return Pair(analysis, firstSound)
    }

    /**
     * 动态计算阈值
     */
    private fun calculateDynamicThreshold(
        volumes: List<Pair<Double, Double>>
    ): Double {
        /*
         * 取前10秒作为环境噪声
         */
        val background = volumes
            .filter { it.first <= 10 }
            .map { it.second }

        if (background.isEmpty()) {
            Log.d(TAG, "前10秒无音频数据，直接使用最低阈值$MIN_THRESHOLD")
            return MIN_THRESHOLD
        }

        val avg = background.average()
        val threshold = avg * 3
        val finalTh = threshold.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)
        Log.d(TAG, "环境噪音均值:$avg，计算阈值:$threshold，限制后最终阈值:$finalTh")
        return finalTh
    }

    /**
     * 初步检测事件，采集事件内全部音量并压缩生成waveform
     */
    private fun detectEvents(
        volumes: List<Pair<Double, Double>>,
        threshold: Double
    ): MutableList<AudioEvent> {
        val events = mutableListOf<AudioEvent>()

        var inEvent = false
        var start = 0.0
        var maxVolume = 0.0
        // 新增：缓存当前事件所有音量采样
        val eventVolumes = mutableListOf<Double>()

        for ((time, volume) in volumes) {
            if (volume > threshold) {
                // 记录当前帧音量用于波形
                eventVolumes.add(volume)

                if (!inEvent) {
                    inEvent = true
                    start = time
                    maxVolume = volume
                } else {
                    if (volume > maxVolume)
                        maxVolume = volume
                }
            } else {
                if (inEvent) {
                    val duration = time - start
                    if (duration >= MIN_EVENT_DURATION) {
                        // 压缩音量采样生成波形
                        val waveData = compressWaveform(eventVolumes)
                        events.add(
                            AudioEvent(
                                startSecond = start,
                                endSecond = time,
                                maxVolume = maxVolume,
                                peakTime = (start + time) / 2,
                                waveform = waveData,
                                type = AudioType.UNKNOWN
                            )
                        )
                        Log.d(TAG, "成功识别声响：开始=$start 结束=$time 持续=$duration 秒")
                    }
                    // 清空缓存，准备下一个事件
                    eventVolumes.clear()
                    inEvent = false
                }
            }
        }
        return events
    }

    /**
     * 合并连续事件（当前版本暂不处理波形拼接，下一阶段优化）
     */
    private fun mergeEvents(
        events: List<AudioEvent>
    ): List<AudioEvent> {
        if (events.isEmpty())
            return emptyList()

        val result = mutableListOf<AudioEvent>()
        var current = events[0]

        for (i in 1 until events.size) {
            val next = events[i]
            if (next.startSecond - current.endSecond <= MERGE_GAP) {
                current = current.copy(
                    endSecond = next.endSecond,
                    maxVolume = maxOf(current.maxVolume, next.maxVolume),
                    peakTime = if (current.maxVolume > next.maxVolume) current.peakTime else next.peakTime
                    // 此处暂不合并waveform，下一阶段补充波形拼接压缩逻辑
                )
            } else {
                result.add(current)
                current = next
            }
        }
        result.add(current)
        return result
    }

    /**
     * 将原始大量音量采样压缩至固定WAVEFORM_POINTS个归一化浮点(0f~1f)
     */
    private fun compressWaveform(source: List<Double>): List<Float> {
        if (source.isEmpty()) return emptyList()
        val output = mutableListOf<Float>()
        val blockStep = source.size.toDouble() / WAVEFORM_POINTS

        for (i in 0 until WAVEFORM_POINTS) {
            val blockStart = (i * blockStep).toInt()
            val blockEnd = ((i + 1) * blockStep).toInt().coerceAtMost(source.size)
            if (blockStart >= source.size) break

            // 取分段内最大音量作为波形高度
            val blockMax = source.subList(blockStart, blockEnd).maxOrNull() ?: 0.0
            // 归一化到0~1，上限5000作为基准音量
            val normalized = (blockMax / 5000.0).coerceIn(0.0, 1.0).toFloat()
            output.add(normalized)
        }
        return output
    }

    /**
     * PCM16音量计算
     */
    private fun calculateVolume(
        buffer: ByteArray,
        size: Int
    ): Double {
        var sum = 0.0
        var count = 0
        for (i in 0 until size step 2) {
            if (i + 1 >= size) break
            // 小端序：i=低字节，i+1=高字节
            val byteLow = buffer[i].toUByte().toInt()
            val byteHigh = buffer[i + 1].toUByte().toInt()
            // 拼接16bit无符号
            val raw = (byteHigh shl 8) or byteLow
            // 转为有符号short -32768 ~ 32767
            val sample = raw.toShort().toInt()
            sum += (sample * sample).toDouble()
            count++
        }
        if (count == 0) return 0.0
        return sqrt(sum / count.toDouble())
    }
}