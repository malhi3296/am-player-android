package com.example.player

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import java.io.File

@OptIn(UnstableApi::class)
class ExoPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    var currentVolumeMultiplier: Float = 1.0f // 0.0 to 2.0 (200%)
        private set

    fun getOrCreatePlayer(): ExoPlayer {
        exoPlayer?.let { return it }

        // Renderers with HW+ priority and SW fallback
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        // Custom low-latency buffer load control for instantaneous seeking without black screen
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // Min buffer
                50000, // Max buffer
                500,   // Buffer for playback start
                1000   // Buffer for playback after rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        trackSelector = DefaultTrackSelector(context)

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setUserAgent("AMPlayer/1.0 (Linux; Android)")

        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector!!)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true // handleAudioBecomingNoisy
            )
            .setSeekBackIncrementMs(10000L)
            .setSeekForwardIncrementMs(10000L)
            .build()

        exoPlayer = player
        setupAudioEnhancer(player.audioSessionId)
        return player
    }

    private fun setupAudioEnhancer(audioSessionId: Int) {
        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            try {
                loudnessEnhancer?.release()
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    enabled = true
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Volume range: 0.0f to 2.0f (up to 200% Audio Boost)
     */
    fun setVolumeWithBoost(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 2.0f)
        currentVolumeMultiplier = clamped
        val player = exoPlayer ?: return

        if (clamped <= 1.0f) {
            player.volume = clamped
            try {
                loudnessEnhancer?.setTargetGain(0)
            } catch (_: Exception) {}
        } else {
            // Maximum standard volume + Hardware LoudnessEnhancer gain boost
            player.volume = 1.0f
            val boostRatio = clamped - 1.0f // 0.0 to 1.0
            val gainMb = (boostRatio * 1000).toInt() // +0 to +1000 mB (+10 dB)
            try {
                loudnessEnhancer?.setTargetGain(gainMb)
            } catch (_: Exception) {}
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    fun prepareMedia(uriString: String, startPositionMs: Long = 0L, externalSubtitleFile: File? = null) {
        val player = getOrCreatePlayer()
        val mediaUri = Uri.parse(uriString)

        val isHlsStream = uriString.contains(".m3u8", ignoreCase = true) || uriString.contains("/hls/", ignoreCase = true)
        val mediaItemBuilder = MediaItem.Builder().setUri(mediaUri)

        if (isHlsStream) {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }

        // Attach external subtitle if present
        if (externalSubtitleFile != null && externalSubtitleFile.exists()) {
            val subUri = Uri.fromFile(externalSubtitleFile)
            val subConfig = MediaItem.SubtitleConfiguration.Builder(subUri)
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setLanguage("en")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
        }

        val item = mediaItemBuilder.build()
        // For live streams, start at default live edge position instead of seeking
        if (isHlsStream) {
            player.setMediaItem(item)
        } else {
            player.setMediaItem(item, startPositionMs)
        }
        player.prepare()
        player.playWhenReady = true
    }

    fun attachSubtitle(subtitleFile: File) {
        val player = exoPlayer ?: return
        val currentPos = player.currentPosition
        val isPlaying = player.isPlaying
        val currentMediaItem = player.currentMediaItem ?: return

        val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subtitleFile))
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setLanguage("en")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val updatedItem = currentMediaItem.buildUpon()
            .setSubtitleConfigurations(listOf(subConfig))
            .build()

        player.setMediaItem(updatedItem, currentPos)
        player.prepare()
        player.playWhenReady = isPlaying
    }

    fun getAudioTracks(): List<Pair<Int, String>> {
        val player = exoPlayer ?: return emptyList()
        val tracks = mutableListOf<Pair<Int, String>>()
        val currentTracks = player.currentTracks

        for (groupIndex in 0 until currentTracks.groups.size) {
            val group = currentTracks.groups[groupIndex]
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val label = format.label ?: format.language ?: "Audio Track ${tracks.size + 1}"
                    val codec = format.sampleMimeType?.substringAfterLast('/') ?: ""
                    val description = if (codec.isNotEmpty()) "$label ($codec)" else label
                    tracks.add(Pair(tracks.size, description))
                }
            }
        }
        return tracks
    }

    fun selectAudioTrack(index: Int) {
        val player = exoPlayer ?: return
        trackSelector?.let { selector ->
            val builder = selector.parameters.buildUpon()
            // Track selection
            builder.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            selector.parameters = builder.build()
        }
    }

    fun release() {
        try {
            loudnessEnhancer?.release()
        } catch (_: Exception) {}
        loudnessEnhancer = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
