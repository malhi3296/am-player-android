package com.example.player

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RepeatState {
    OFF, ALL, ONE
}

data class MusicPlayerState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatState: RepeatState = RepeatState.ALL,
    val volumeBoost: Float = 1.0f // 1.0f to 2.0f
)

class MusicPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var positionJob: Job? = null

    private val playlist = mutableListOf<AudioTrack>()
    private var currentIndex: Int = -1

    private val _state = MutableStateFlow(MusicPlayerState())
    val state: StateFlow<MusicPlayerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val dur = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
            _state.update { it.copy(durationMs = dur) }
            if (playbackState == Player.STATE_ENDED) {
                handleTrackEnded()
            }
        }
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        exoPlayer?.let { return it }

        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(listener)
        exoPlayer = player
        setupEnhancer(player.audioSessionId)
        startPositionTracker()
        return player
    }

    private fun setupEnhancer(sessionId: Int) {
        if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
            try {
                loudnessEnhancer?.release()
                loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = true }
            } catch (_: Exception) {}
        }
    }

    fun playTrack(track: AudioTrack, trackList: List<AudioTrack>) {
        val player = getOrCreatePlayer()
        playlist.clear()
        playlist.addAll(trackList)
        currentIndex = playlist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        val mediaItem = MediaItem.fromUri(Uri.parse(track.uri))
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        _state.update {
            it.copy(
                currentTrack = track,
                currentPositionMs = 0L,
                durationMs = track.durationMs
            )
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    fun next() {
        if (playlist.isEmpty()) return
        val nextIdx = if (_state.value.isShuffle) {
            playlist.indices.random()
        } else {
            (currentIndex + 1) % playlist.size
        }
        playTrack(playlist[nextIdx], playlist)
    }

    fun previous() {
        if (playlist.isEmpty()) return
        val prevIdx = if (currentIndex > 0) currentIndex - 1 else playlist.size - 1
        playTrack(playlist[prevIdx], playlist)
    }

    fun toggleShuffle() {
        _state.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        val nextMode = when (_state.value.repeatState) {
            RepeatState.OFF -> RepeatState.ALL
            RepeatState.ALL -> RepeatState.ONE
            RepeatState.ONE -> RepeatState.OFF
        }
        _state.update { it.copy(repeatState = nextMode) }
    }

    fun setVolumeBoost(multiplier: Float) {
        val clamped = multiplier.coerceIn(1.0f, 2.0f)
        _state.update { it.copy(volumeBoost = clamped) }
        val player = exoPlayer ?: return

        if (clamped <= 1.0f) {
            player.volume = 1.0f
            try { loudnessEnhancer?.setTargetGain(0) } catch (_: Exception) {}
        } else {
            player.volume = 1.0f
            val gain = ((clamped - 1.0f) * 1000).toInt()
            try { loudnessEnhancer?.setTargetGain(gain) } catch (_: Exception) {}
        }
    }

    private fun handleTrackEnded() {
        when (_state.value.repeatState) {
            RepeatState.ONE -> {
                exoPlayer?.seekTo(0L)
                exoPlayer?.play()
            }
            RepeatState.ALL -> next()
            RepeatState.OFF -> {
                if (currentIndex < playlist.size - 1) next() else exoPlayer?.pause()
            }
        }
    }

    private fun startPositionTracker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _state.update {
                            it.copy(
                                currentPositionMs = player.currentPosition,
                                durationMs = player.duration.coerceAtLeast(0L)
                            )
                        }
                    }
                }
                delay(400L)
            }
        }
    }

    fun release() {
        positionJob?.cancel()
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        loudnessEnhancer = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
