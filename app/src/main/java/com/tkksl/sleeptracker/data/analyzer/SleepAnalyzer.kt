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
        if (!pcmFile.exists() || pcmFile.length() <= 0) {
            Log.e(TAG, "PCM文件不存在或者为空")
            return emptyAnalysisResult()
        }

        // ========== 第一遍流式读取：只采集前10秒数据，计算动态阈值 ==========
        val backgroundVolumeList = mutableListOf<Double>()
        var totalVolumeAll = 0.0
        var sampleCountAll = 0L
        var maxVolumeAll = 0.0

        FileInputStream(pcmFile).use { input ->
            val buffer = ByteArray(2048)
            var frameIndex = 0L
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                val volume = calculateVolume(buffer, read)
                val currentTime = frameIndex * (read / 2.0) / sampleRate

                totalVolumeAll += volume
                sampleCountAll++
                if (volume > maxVolumeAll) maxVolumeAll = volume

                // 仅收集前10秒用于环境噪声阈值计算
                if (currentTime <= 10.0) {
                    backgroundVolumeList.add(volume)
                }
                frameIndex++
            }
        }

        if (sampleCountAll == 0L) {
            Log.e(TAG, "PCM无有效音频采样")
            return emptyAnalysisResult()
        }

        val threshold = calculateDynamicThreshold(backgroundVolumeList)
        Log.d(TAG, "动态检测阈值：$threshold")

        // ========== 第二遍流式读取：实时识别声响事件，不缓存全部音量 ==========
        val rawEvents = detectEventsStream(pcmFile, sampleRate, threshold)
        Log.d(TAG, "原始检测声响数量：${rawEvents.size}")

        // 合并相近事件
        val mergedEvents = mergeEvents(rawEvents)
        Log.d(TAG, "合并后最终声响数量：${mergedEvents.size}")
        val totalNoiseDuration = mergedEvents.sumOf { it.endSecond - it.startSecond }

        val avgVolume = totalVolumeAll / sampleCountAll.toDouble()
        val firstSound = mergedEvents.firstOrNull()?.startSecond ?: 0.0

        val analysis = SleepAnalysis(
            averageVolume = avgVolume,
            maxVolume = maxVolumeAll,
            noiseCount = mergedEvents.size,
            totalNoiseDuration = totalNoiseDuration,
            score = 100,
            events = mergedEvents
        )
        return Pair(analysis, firstSound)
    }

    private fun emptyAnalysisResult(): Pair<SleepAnalysis, Double> {
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

    /**
     * 动态计算阈值
     */
    private fun calculateDynamicThreshold(
        background: List<Double>
    ): Double {
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
     * ✅【新增流式检测】逐段读取PCM，实时识别事件，不存储全部音量数据
     */
    private fun detectEventsStream(
        pcmFile: File,
        sampleRate: Int,
        threshold: Double
    ): MutableList<AudioEvent> {
        val events = mutableListOf<AudioEvent>()
        FileInputStream(pcmFile).use { input ->
            val buffer = ByteArray(2048)
            var frameIndex = 0L

            var inEvent = false
            var start = 0.0
            var maxVolume = 0.0
            val eventVolumes = mutableListOf<Double>()

            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                val volume = calculateVolume(buffer, read)
                val currentTime = frameIndex * (read / 2.0) / sampleRate

                if (volume > threshold) {
                    eventVolumes.add(volume)
                    if (!inEvent) {
                        inEvent = true
                        start = currentTime
                        maxVolume = volume
                    } else {
                        if (volume > maxVolume) maxVolume = volume
                    }
                } else {
                    if (inEvent) {
                        val duration = currentTime - start
                        if (duration >= MIN_EVENT_DURATION) {
                            val waveData = compressWaveform(eventVolumes)
                            events.add(
                                AudioEvent(
                                    startSecond = start,
                                    endSecond = currentTime,
                                    maxVolume = maxVolume,
                                    peakTime = (start + currentTime) / 2,
                                    waveform = waveData,
                                    type = AudioType.UNKNOWN
                                )
                            )
                            Log.d(TAG, "成功识别声响：开始=$start 结束=$currentTime 持续=$duration 秒")
                        }
                        eventVolumes.clear()
                        inEvent = false
                    }
                }
                frameIndex++
            }

            // 处理文件末尾还处于事件中的情况
            if (inEvent) {
                val duration = pcmFile.length() / (2.0 * sampleRate) - start
                if (duration >= MIN_EVENT_DURATION) {
                    val waveData = compressWaveform(eventVolumes)
                    events.add(
                        AudioEvent(
                            startSecond = start,
                            endSecond = pcmFile.length() / (2.0 * sampleRate),
                            maxVolume = maxVolume,
                            peakTime = (start + pcmFile.length() / (2.0 * sampleRate)) / 2,
                            waveform = waveData,
                            type = AudioType.UNKNOWN
                        )
                    )
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
