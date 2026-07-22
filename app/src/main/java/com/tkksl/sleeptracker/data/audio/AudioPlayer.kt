package com.tkksl.sleeptracker.data.audio

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    // 独立协程域，解决viewModelScope不存在报错
    private val playerScope = CoroutineScope(Dispatchers.IO)

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.seekTo(0)
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    fun play(filePath: String, onComplete: () -> Unit) {
        stop()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
            setOnCompletionListener {
                onComplete()
            }
        }
    }

    /** 定点播放片段：从seekMs开始播放durationMs时长 */
    fun playSegment(filePath: String, seekMs: Long, durationMs: Long, onComplete: () -> Unit) {
        stop()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            // seekTo接收Int，Long强转
            seekTo(seekMs.toInt())
            start()
            // 到时长自动停止
            playerScope.launch {
                delay(durationMs)
                stop()
                onComplete()
            }
            setOnCompletionListener {
                onComplete()
            }
        }
    }

    /** 释放播放器资源 */
    fun release() {
        stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}