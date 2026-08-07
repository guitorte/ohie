package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AudioPlayerHelper(private val context: Context) {

    companion object {
        private const val TAG = "AudioPlayerHelper"
    }

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    _playbackState.value = _playbackState.value.copy(
                        currentPositionMs = player.currentPosition.toLong(),
                        durationMs = player.duration.coerceAtLeast(0).toLong()
                    )
                    handler.postDelayed(this, 100)
                }
            }
        }
    }

    fun playAudio(filePath: String, clipId: Long? = null, title: String? = null) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: $filePath")
            return
        }

        // If clicking same active audio, toggle pause/play
        val currentState = _playbackState.value
        if (currentState.filePath == filePath && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                pause()
            } else {
                resume()
            }
            return
        }

        stop()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(filePath)
                prepare()
                
                // Apply playback speed
                playbackParams = playbackParams.setSpeed(currentState.playbackSpeed)
                start()
            }

            mediaPlayer = player

            _playbackState.value = PlaybackState(
                isPlaying = true,
                filePath = filePath,
                clipId = clipId,
                title = title ?: file.name,
                currentPositionMs = 0L,
                durationMs = player.duration.toLong(),
                playbackSpeed = currentState.playbackSpeed
            )

            player.setOnCompletionListener {
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    currentPositionMs = player.duration.toLong()
                )
                handler.removeCallbacks(updateProgressRunnable)
            }

            handler.post(updateProgressRunnable)

        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file: $filePath", e)
            _playbackState.value = PlaybackState()
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    currentPositionMs = player.currentPosition.toLong()
                )
                handler.removeCallbacks(updateProgressRunnable)
            }
        }
    }

    fun resume() {
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                player.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                handler.post(updateProgressRunnable)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            player.seekTo(positionMs.toInt())
            _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
        }
    }

    fun setSpeed(speed: Float) {
        val newSpeed = speed.coerceIn(0.5f, 2.0f)
        mediaPlayer?.let { player ->
            try {
                player.playbackParams = player.playbackParams.setSpeed(newSpeed)
            } catch (e: Exception) {
                Log.e(TAG, "Failed setting speed $newSpeed", e)
            }
        }
        _playbackState.value = _playbackState.value.copy(playbackSpeed = newSpeed)
    }

    fun stop() {
        handler.removeCallbacks(updateProgressRunnable)
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            } catch (ignored: Exception) {}
        }
        mediaPlayer = null
        _playbackState.value = PlaybackState()
    }

    data class PlaybackState(
        val isPlaying: Boolean = false,
        val filePath: String? = null,
        val clipId: Long? = null,
        val title: String? = null,
        val currentPositionMs: Long = 0L,
        val durationMs: Long = 0L,
        val playbackSpeed: Float = 1.0f
    )
}
