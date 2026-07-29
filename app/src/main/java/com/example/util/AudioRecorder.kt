package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    fun start(fileNamePrefix: String): File? {
        val outputDir = File(context.filesDir, "audio_captures").apply { if (!exists()) mkdirs() }
        val outputFile = File(outputDir, "${fileNamePrefix}_${System.currentTimeMillis()}.m4a")
        currentOutputFile = outputFile

        try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            return outputFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error starting media recorder", e)
            stop()
            return null
        }
    }

    fun stop(): File? {
        val file = currentOutputFile
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping recorder", e)
        } finally {
            recorder = null
        }
        return file
    }
}
