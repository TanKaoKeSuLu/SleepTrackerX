package com.tkksl.sleeptracker.data.audio

import com.tkksl.sleeptracker.data.analyzer.AudioEvent
import com.tkksl.sleeptracker.data.analyzer.AudioType
import kotlin.math.sqrt

class StreamingAudioAnalyzer(
    private val sampleRate: Int,
    private val clipWriter: EventClipWriter
) {
    // 环形缓冲区：保存最近3秒采样
    private val ringBuffer = RingBuffer(sampleRate, keepSeconds = 3.0)

    // 环境学习窗口：前30s 只统计噪音基线，不产生事件
    private val learnWindowMs = 30_000L
    // 声音消失后，再多等待5s才闭合事件
    private val eventHoldMs = 5000L
    // 新增：单个事件最大时长保护，防止持续噪音导致内存暴涨OOM
    private val MAX_EVENT_DURATION_MS = 120_000L

    // 状态
    private var recordStartMs: Long = 0L
    private var isLearnPhase = true

    // 环境噪音基线
    private var noiseSumRms = 0.0
    private var noiseSampleCount = 0
    private var dynamicThreshold = 0.0

    // 事件状态机
    private var inEvent = false
    private var eventStartMs: Long = 0L
    private var eventMaxVolume = 0.0
    private var lastSoundMs: Long = 0L

    // 缓存当前事件所有音频块
    private val currentEventChunks = mutableListOf<ShortArray>()
    private var clipSeq = 0

    // 已识别全部事件（内存）
    val detectedEvents = mutableListOf<AudioEvent>()

    // ============ 新增：全局统计指标，供Service广播输出 ============
    var totalNoiseDuration: Double = 0.0
        private set
    var averageVolume: Double = 0.0
        private set
    var maxWholeVolume: Double = 0.0
        private set

    private var volumeAccSum = 0.0
    private var volumeAccCount = 0

    fun start(recordStartTimeMs: Long) {
        recordStartMs = recordStartTimeMs
        ringBuffer.clear()
        isLearnPhase = true
        noiseSumRms = 0.0
        noiseSampleCount = 0
        dynamicThreshold = 0.0
        inEvent = false
        currentEventChunks.clear()
        detectedEvents.clear()
        clipSeq = 0

        // 重置全局统计
        totalNoiseDuration = 0.0
        averageVolume = 0.0
        maxWholeVolume = 0.0
        volumeAccSum = 0.0
        volumeAccCount = 0
    }

    /**
     * 每一块音频输入，由AudioStreamCallback驱动
     * @param samples short pcm
     * @param chunkRelativeMs 当前块相对录音开始毫秒
     */
    fun processChunk(samples: ShortArray, chunkRelativeMs: Long) {
        ringBuffer.write(samples)

        // 计算本块RMS音量 0‑1
        val rms = calculateRms(samples)

        // 非学习期，累积全局音量统计
        if (!isLearnPhase) {
            volumeAccSum += rms
            volumeAccCount++
            if(rms > maxWholeVolume){
                maxWholeVolume = rms
            }
        }

        // 阶段1：环境噪音学习期
        if (isLearnPhase) {
            noiseSumRms += rms
            noiseSampleCount++
            // 满30s，退出学习，计算阈值
            if (chunkRelativeMs >= learnWindowMs) {
                val avgNoise = noiseSumRms / noiseSampleCount
                // 阈值：环境噪音 × 2.8 放大系数，和旧算法行为对齐
                dynamicThreshold = avgNoise * 2.8
                isLearnPhase = false
                android.util.Log.d("StreamAnalyzerDebug",
                    "环境学习完成 avgNoise=$avgNoise, dynamicThreshold=$dynamicThreshold")
            }
            return
        }

        // ========== 超长事件保护：如果当前事件已经超过最大时长，强制切分 ==========
        if(inEvent){
            val eventElapseMs = chunkRelativeMs - eventStartMs
            if(eventElapseMs >= MAX_EVENT_DURATION_MS){
                android.util.Log.d("StreamAnalyzerDebug","事件超时保护触发，强制切分事件")
                closeEvent(chunkRelativeMs)
            }
            currentEventChunks.add(samples.copyOf())
        }

        // 正式事件检测阶段
        if (rms > dynamicThreshold) {
            lastSoundMs = chunkRelativeMs
            if (!inEvent) {
                // 新事件开始
                inEvent = true
                eventStartMs = chunkRelativeMs
                eventMaxVolume = rms
                currentEventChunks.clear()
                android.util.Log.d("StreamAnalyzerDebug",
                    "检测事件开始 startMs=$eventStartMs")
            } else {
                eventMaxVolume = maxOf(eventMaxVolume, rms)
            }
        } else {
            // 当前块安静，判断是否可以关闭事件
            if (inEvent) {
                val quietDuration = chunkRelativeMs - lastSoundMs
                if (quietDuration >= eventHoldMs) {
                    // 闭合事件
                    closeEvent(chunkRelativeMs)
                }
            }
        }
    }

    /**
     * 录音结束，强制收尾未关闭事件
     */
    fun finishAll() {
        if (inEvent) {
            val nowRelativeMs = System.currentTimeMillis() - recordStartMs
            closeAllEvent(nowRelativeMs)
        }
        // 录音结束计算全局平均音量
        if(volumeAccCount > 0){
            averageVolume = volumeAccSum / volumeAccCount
        }
        android.util.Log.d("StreamAnalyzerDebug",
            "录音结束，总事件数量=${detectedEvents.size}, totalNoiseSec=$totalNoiseDuration, maxVol=$maxWholeVolume, avgVol=$averageVolume")
    }


    private fun closeAllEvent(endRelativeMs: Long) {
        val fullPre = ringBuffer.readAll()
        val eventBody = concatShorts(currentEventChunks)
        val fullClipSamples = concatShorts(listOf(fullPre, eventBody))

        clipSeq++
        val fileName = "clip_${clipSeq}.wav"
        val file = clipWriter.writeWavClip(fullClipSamples, fileName)
        val clipPath = file?.absolutePath ?: ""

        val waveform = buildWaveform(fullClipSamples)

        val startSec = eventStartMs / 1000.0
        val endSec = endRelativeMs / 1000.0
        val peakTime = startSec
        val evt = AudioEvent(
            startSecond = startSec,
            endSecond = endSec,
            peakTime = peakTime,
            maxVolume = eventMaxVolume,
            waveform = waveform,
            type = AudioType.UNKNOWN,
            clipPath = clipPath
        )
        detectedEvents.add(evt)
        // 累加噪音总时长
        totalNoiseDuration += (endSec - startSec)

        android.util.Log.d("StreamAnalyzerDebug",
            "强制结束事件 start=$startSec end=$endSec clipPath=$clipPath")

        inEvent = false
        eventMaxVolume = 0.0
        currentEventChunks.clear()
    }

    private fun closeEvent(endRelativeMs: Long) {
        // 取出环形缓冲区3秒前置历史 + 当前事件全部采样
        val preSamples = ringBuffer.readAll()
        val bodySamples = concatShorts(currentEventChunks)
        val fullClipSamples = concatShorts(listOf(preSamples, bodySamples))

        clipSeq++
        val fileName = "clip_${clipSeq}.wav"
        val outFile = clipWriter.writeWavClip(fullClipSamples, fileName)
        val clipAbsolutePath = outFile?.absolutePath ?: ""

        val waveform = buildWaveform(fullClipSamples)

        val startSec = eventStartMs / 1000.0
        val endSec = endRelativeMs / 1000.0
        val peakTime = startSec
        val evt = AudioEvent(
            startSecond = startSec,
            endSecond = endSec,
            peakTime = peakTime,
            maxVolume = eventMaxVolume,
            waveform = waveform,
            type = AudioType.UNKNOWN,
            clipPath = clipAbsolutePath
        )
        detectedEvents.add(evt)
        // 累加噪音事件时长
        totalNoiseDuration += (endSec - startSec)

        android.util.Log.d("StreamAnalyzerDebug",
            "事件闭合 start=$startSec end=$endSec maxVol=$eventMaxVolume clip=$clipAbsolutePath")

        inEvent = false
        eventMaxVolume = 0.0
        currentEventChunks.clear()
    }

    /**
     * 拼接多个ShortArray
     */
    private fun concatShorts(arrList: List<ShortArray>): ShortArray {
        val totalLen = arrList.sumOf { it.size }
        val dst = ShortArray(totalLen)
        var offset = 0
        for(arr in arrList){
            arr.copyInto(dst, offset)
            offset += arr.size
        }
        return dst
    }

    /**
     * 根据采样构建归一化waveform(0‑1)，降采样简化，最多120个点
     */
    private fun buildWaveform(samples: ShortArray): List<Float> {
        if(samples.isEmpty()) return emptyList()
        val targetPointCount = 120
        val step = (samples.size / targetPointCount).coerceAtLeast(1)
        val res = mutableListOf<Float>()
        var i = 0
        while(i < samples.size){
            val s = samples[i]
            val norm = Math.abs(s.toInt()).toFloat() / Short.MAX_VALUE
            res.add(norm)
            i += step
        }
        return res
    }

    /**
     * 计算RMS，归一化输出0‑1
     */
    private fun calculateRms(samples: ShortArray): Double {
        if (samples.isEmpty()) return 0.0
        var sum = 0.0
        for (s in samples) {
            val norm = s.toDouble() / Short.MAX_VALUE
            sum += norm * norm
        }
        val rms = sqrt(sum / samples.size)
        return rms
    }
}
