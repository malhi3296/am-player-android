package com.example.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
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

data class EqualizerPreset(
    val name: String,
    val bandLevels: List<Int>, // 5 bands in mB (-1500 to +1500)
    val bassBoost: Int // 0 to 1000
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val currentPreset: String = "Balanced",
    val bandLevels: List<Int> = listOf(0, 0, 0, 0, 0), // 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
    val bassBoostStrength: Int = 300 // 0 to 1000
)

data class MusicPlayerState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatState: RepeatState = RepeatState.ALL,
    val volumeBoost: Float = 1.0f, // 1.0f to 2.0f
    val playbackSpeed: Float = 1.0f,
    val playlist: List<AudioTrack> = emptyList()
)

class MusicPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val scope = CoroutineScope(Dispatchers.Main)
    private var positionJob: Job? = null

    private val playlist = mutableListOf<AudioTrack>()
    private var currentIndex: Int = -1

    private val _state = MutableStateFlow(MusicPlayerState())
    val state: StateFlow<MusicPlayerState> = _state.asStateFlow()

    private val _eqState = MutableStateFlow(EqualizerState())
    val eqState: StateFlow<EqualizerState> = _eqState.asStateFlow()

    val presets = listOf(
        EqualizerPreset("Balanced", listOf(0, 0, 0, 0, 0), 200),
        EqualizerPreset("Bass Boost", listOf(900, 600, 100, -100, -200), 800),
        EqualizerPreset("Rock", listOf(500, 300, -100, 400, 600), 400),
        EqualizerPreset("Pop", listOf(-200, 200, 500, 300, -100), 300),
        EqualizerPreset("Jazz", listOf(300, 200, -200, 200, 400), 250),
        EqualizerPreset("Electronic", listOf(600, 400, 0, 300, 700), 600),
        EqualizerPreset("Vocal Clarity", listOf(-300, -100, 400, 700, 500), 100),
        EqualizerPreset("Classical", listOf(400, 300, -200, 300, 400), 200)
    )

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
            .setWakeMode(C.WAKE_MODE_LOCAL) // Keeps CPU awake for lock screen background playback!
            .build()

        player.addListener(listener)
        exoPlayer = player

        try {
            mediaSession?.release()
            mediaSession = MediaSession.Builder(context, player).build()
        } catch (_: Exception) {}

        setupAudioEffects(player.audioSessionId)
        startPositionTracker()
        return player
    }

    private fun setupAudioEffects(sessionId: Int) {
        if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
            try {
                loudnessEnhancer?.release()
                loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = true }
            } catch (_: Exception) {}

            try {
                equalizer?.release()
                equalizer = Equalizer(0, sessionId).apply {
                    enabled = _eqState.value.isEnabled
                    applyBandsToHardware(this, _eqState.value.bandLevels)
                }
            } catch (_: Exception) {}

            try {
                bassBoost?.release()
                bassBoost = BassBoost(0, sessionId).apply {
                    enabled = _eqState.value.isEnabled
                    setStrength(_eqState.value.bassBoostStrength.toShort())
                }
            } catch (_: Exception) {}
        }
    }

    private fun applyBandsToHardware(eq: Equalizer, levels: List<Int>) {
        try {
            val numBands = eq.numberOfBands.toInt().coerceAtMost(levels.size)
            for (i in 0 until numBands) {
                val clamped = levels[i].toShort().coerceIn(eq.bandLevelRange[0], eq.bandLevelRange[1])
                eq.setBandLevel(i.toShort(), clamped)
            }
        } catch (_: Exception) {}
    }

    fun playTrack(track: AudioTrack, trackList: List<AudioTrack>) {
        val player = getOrCreatePlayer()
        if (playlist != trackList) {
            playlist.clear()
            playlist.addAll(trackList)
        }
        currentIndex = playlist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        // Instant optimistic state update for 0ms UI latency
        _state.update {
            it.copy(
                currentTrack = track,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = track.durationMs,
                playlist = playlist
            )
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(track.uri))
            .setMediaMetadata(metadata)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        val player = exoPlayer
        if (player == null) {
            val track = _state.value.currentTrack ?: playlist.firstOrNull() ?: return
            playTrack(track, playlist)
            return
        }
        val willPlay = !player.isPlaying
        _state.update { it.copy(isPlaying = willPlay) }
        if (willPlay) {
            player.play()
        } else {
            player.pause()
        }
    }

    fun seekTo(positionMs: Long) {
        _state.update { it.copy(currentPositionMs = positionMs) }
        exoPlayer?.seekTo(positionMs)
    }

    fun seekBy(offsetMs: Long) {
        val current = _state.value.currentPositionMs
        val dur = _state.value.durationMs
        val target = (current + offsetMs).coerceIn(0L, dur)
        seekTo(target)
    }

    fun next() {
        if (playlist.isEmpty()) return
        val nextIdx = if (_state.value.isShuffle) {
            playlist.indices.random()
        } else {
            (currentIndex + 1) % playlist.size
        }
        val nextTrack = playlist[nextIdx]
        currentIndex = nextIdx
        _state.update {
            it.copy(
                currentTrack = nextTrack,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = nextTrack.durationMs
            )
        }
        playTrack(nextTrack, playlist)
    }

    fun previous() {
        if (playlist.isEmpty()) return
        val prevIdx = if (currentIndex > 0) currentIndex - 1 else playlist.size - 1
        val prevTrack = playlist[prevIdx]
        currentIndex = prevIdx
        _state.update {
            it.copy(
                currentTrack = prevTrack,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = prevTrack.durationMs
            )
        }
        playTrack(prevTrack, playlist)
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

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        exoPlayer?.playbackParameters = PlaybackParameters(clamped)
        _state.update { it.copy(playbackSpeed = clamped) }
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

    // EQUALIZER CONTROLS
    fun setEqualizerEnabled(enabled: Boolean) {
        _eqState.update { it.copy(isEnabled = enabled) }
        try { equalizer?.enabled = enabled } catch (_: Exception) {}
        try { bassBoost?.enabled = enabled } catch (_: Exception) {}
    }

    fun setEqualizerPreset(presetName: String) {
        val preset = presets.firstOrNull { it.name.equals(presetName, ignoreCase = true) } ?: return
        _eqState.update {
            it.copy(
                currentPreset = preset.name,
                bandLevels = preset.bandLevels,
                bassBoostStrength = preset.bassBoost
            )
        }
        equalizer?.let { applyBandsToHardware(it, preset.bandLevels) }
        try { bassBoost?.setStrength(preset.bassBoost.toShort()) } catch (_: Exception) {}
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val levels = _eqState.value.bandLevels.toMutableList()
        if (bandIndex in levels.indices) {
            levels[bandIndex] = levelMb
            _eqState.update { it.copy(bandLevels = levels, currentPreset = "Custom") }
            equalizer?.let { applyBandsToHardware(it, levels) }
        }
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _eqState.update { it.copy(bassBoostStrength = clamped) }
        try { bassBoost?.setStrength(clamped.toShort()) } catch (_: Exception) {}
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
                delay(300L)
            }
        }
    }

    fun release() {
        positionJob?.cancel()
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { mediaSession?.release() } catch (_: Exception) {}
        mediaSession = null
        loudnessEnhancer = null
        equalizer = null
        bassBoost = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
