package com.tkksl.sleeptracker.data.audio

import android.media.MediaPlayer
import java.io.File

class AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun play(
        path: String,
        onCompletion: () -> Unit
    ) {
        stop()
        val file = File(path)
        if (!file.exists()) return

        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            setOnCompletionListener {
                stop()
                onCompletion()
            }
            start()
        }
    }

    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }
}