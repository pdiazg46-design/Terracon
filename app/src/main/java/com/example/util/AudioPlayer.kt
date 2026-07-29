package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    var isPlaying = false
        private set

    fun play(audioPath: String, onComplete: () -> Unit) {
        stop()
        val file = File(audioPath)
        if (!file.exists()) {
            Log.e("AudioPlayer", "File does not exist: $audioPath")
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                prepare()
                setOnCompletionListener {
                    this@AudioPlayer.isPlaying = false
                    onComplete()
                }
                start()
            }
            isPlaying = true
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error playing audio", e)
            stop()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error stopping player", e)
        } finally {
            mediaPlayer = null
            isPlaying = false
        }
    }
}
