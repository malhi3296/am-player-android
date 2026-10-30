package com.example.ui.player

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.AMPlayerApplication
import com.example.data.model.MediaItemEntity
import com.example.data.model.SubtitleCacheEntity
import com.example.data.model.SubtitleCandidate
import com.example.data.subtitles.OpenSubtitlesHasher
import com.example.player.AspectRatioMode
import com.example.player.DecoderMode
import com.example.player.ExoPlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed class PlayerHudState {
    object None : PlayerHudState()
    data class Volume(val value: Float, val isBoost: Boolean) : PlayerHudState()
    data class Brightness(val value: Float) : PlayerHudState()
    data class Seek(val targetTimeMs: Long, val totalDurationMs: Long, val diffMs: Long) : PlayerHudState()
}

data class PlayerUiState(
    val mediaItem: MediaItemEntity? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val volume: Float = 1.0f, // 0.0 to 2.0 (200%)
    val brightness: Float = 0.5f, // 0.0 to 1.0
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val decoderMode: DecoderMode = DecoderMode.HW_PLUS,
    val playbackSpeed: Float = 1.0f,
    val isLocked: Boolean = false,
    val isBackgroundPlayEnabled: Boolean = false,
    val showControls: Boolean = true,
    val hudState: PlayerHudState = PlayerHudState.None,
    val zoomScale: Float = 1.0f,
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f,
    // Subtitle states
    val isSearchingSubtitles: Boolean = false,
    val subtitleCandidates: List<SubtitleCandidate> = emptyList(),
    val currentSubtitleTitle: String? = null,
    val subtitleDelayMs: Long = 0L,
    val cachedSubtitles: List<SubtitleCacheEntity> = emptyList(),
    val showRemainingTime: Boolean = false,
    // Features & Skins
    val playerSkin: com.example.player.PlayerSkin = com.example.player.PlayerSkin.AM_ROYAL_BLUE,
    val orientationLock: com.example.player.OrientationLock = com.example.player.OrientationLock.AUTO,
    val audioPreset: com.example.player.AudioPreset = com.example.player.AudioPreset.NORMAL,
    val sleepTimerMinutes: Int? = null,
    val abRepeatPointA: Long? = null,
    val abRepeatPointB: Long? = null,
    val isMuted: Boolean = false,
    val toastMessage: String? = null,
    val triggerSubtitleSearch: Boolean = false,
    val videoColorMode: com.example.player.VideoColorMode = com.example.player.VideoColorMode.NORMAL,
    val gestureControlsEnabled: Boolean = true,
    val seekDurationSeconds: Int = 10,
    val audioTracks: List<Pair<Int, String>> = emptyList(),
    val selectedAudioTrackIndex: Int = 0,
    val subtitleTracks: List<Pair<Int, String>> = emptyList(),
    val selectedSubtitleTrackIndex: Int = -1,
    val subtitleSearchPerformed: Boolean = false,
    val subtitleSettingsOpen: Boolean = false,
    val subtitleFontSizeSp: Int = 18,
    val subtitleColorHex: Long = 0xFFFFFFFF,
    val subtitlePositionBottom: Boolean = true,
    val isIptvStream: Boolean = false,
    val currentIptvChannels: List<com.example.data.model.IptvChannel> = emptyList(),
    val currentIptvChannelIndex: Int = -1,
    val showIptvQuickDrawer: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AMPlayerApplication).container
    private val mediaRepo = container.mediaRepository
    private val subtitleRepo = container.subtitleRepository

    val playerManager = ExoPlayerManager(application)
    val exoPlayer: ExoPlayer get() = playerManager.getOrCreatePlayer()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var positionTrackingJob: Job? = null
    private var controlsTimeoutJob: Job? = null
    private var hudTimeoutJob: Job? = null
    private var activeSubtitleFile: File? = null
    private var videoHash: String = ""

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                scheduleControlsHide()
            }
            val title = _uiState.value.mediaItem?.title ?: "Video"
            com.example.player.PlaybackNotificationHelper.showPlaybackNotification(
                getApplication(),
                title,
                "AM Video Player",
                isPlaying
            )
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val duration = exoPlayer.duration.coerceAtLeast(0L)
            _uiState.update {
                it.copy(
                    isBuffering = isBuffering,
                    durationMs = duration
                )
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val cause = error.cause
            val msg = when {
                cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException -> {
                    "Stream error: HTTP ${cause.responseCode} (Channel offline or unavailable)"
                }
                cause is androidx.media3.datasource.FileDataSource.FileDataSourceException -> {
                    "Unable to open video: File not found or invalid URL"
                }
                else -> error.message ?: "Live stream buffering or unavailable"
            }
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    toastMessage = msg
                )
            }
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        startPositionTracker()
        val settings = container.settingsRepository.settings.value
        _uiState.update {
            it.copy(
                gestureControlsEnabled = settings.gestureControlsEnabled,
                seekDurationSeconds = settings.seekDurationSeconds
            )
        }
    }

    
    fun refreshTracks() {
        val audio = playerManager.getAudioTracks()
        val subs = playerManager.getSubtitleTracks()
        _uiState.update {
            it.copy(
                audioTracks = audio,
                subtitleTracks = subs
            )
        }
    }

    fun selectAudioTrack(index: Int) {
        playerManager.selectAudioTrack(index)
        _uiState.update { it.copy(selectedAudioTrackIndex = index) }
        refreshTracks()
    }

    fun selectSubtitleTrack(index: Int) {
        playerManager.selectSubtitleTrack(index)
        _uiState.update { it.copy(selectedSubtitleTrackIndex = index) }
        refreshTracks()
    }

    fun setSubtitleSettings(sizeSp: Int, colorHex: Long, positionBottom: Boolean) {
        _uiState.update {
            it.copy(
                subtitleFontSizeSp = sizeSp,
                subtitleColorHex = colorHex,
                subtitlePositionBottom = positionBottom
            )
        }
    }

    fun playNextChannel() {
        val list = _uiState.value.currentIptvChannels
        if (list.isEmpty()) return
        val curr = _uiState.value.currentIptvChannelIndex
        val nextIdx = (curr + 1) % list.size
        val nextChan = list[nextIdx]
        _uiState.update { it.copy(currentIptvChannelIndex = nextIdx) }
        loadMedia(nextChan.streamUrl)
    }

    fun playPreviousChannel() {
        val list = _uiState.value.currentIptvChannels
        if (list.isEmpty()) return
        val curr = _uiState.value.currentIptvChannelIndex
        val prevIdx = if (curr - 1 < 0) list.size - 1 else curr - 1
        val prevChan = list[prevIdx]
        _uiState.update { it.copy(currentIptvChannelIndex = prevIdx) }
        loadMedia(prevChan.streamUrl)
    }

    fun selectIptvChannel(channel: com.example.data.model.IptvChannel) {
        val idx = _uiState.value.currentIptvChannels.indexOf(channel)
        if (idx >= 0) {
            _uiState.update { it.copy(currentIptvChannelIndex = idx, showIptvQuickDrawer = false) }
        }
        loadMedia(channel.streamUrl)
    }

    fun toggleIptvQuickDrawer() {
        _uiState.update { it.copy(showIptvQuickDrawer = !it.showIptvQuickDrawer) }
    }

    fun setVideoColorMode(mode: com.example.player.VideoColorMode) {
        _uiState.update { it.copy(videoColorMode = mode, toastMessage = "Color Mode: ${mode.displayName}") }
    }

    fun loadMedia(mediaUriOrId: String) {
        viewModelScope.launch {
            val matchingChannel = container.iptvRepository.folders.value
                .flatMap { it.channels }
                .firstOrNull { 
                    it.streamUrl.equals(mediaUriOrId, ignoreCase = true) ||
                    it.id.equals(mediaUriOrId, ignoreCase = true) ||
                    it.name.equals(mediaUriOrId, ignoreCase = true)
                }
                ?: container.iptvRepository.channels.value
                    .firstOrNull { 
                        it.streamUrl.equals(mediaUriOrId, ignoreCase = true) ||
                        it.id.equals(mediaUriOrId, ignoreCase = true) ||
                        it.name.equals(mediaUriOrId, ignoreCase = true)
                    }

            val item = if (matchingChannel != null) {
                MediaItemEntity(
                    id = matchingChannel.id,
                    uri = matchingChannel.streamUrl,
                    title = matchingChannel.name,
                    folderName = "IPTV: ${matchingChannel.category}",
                    resolution = "Live Stream"
                )
            } else if (mediaUriOrId.startsWith("http://", ignoreCase = true) || mediaUriOrId.startsWith("https://", ignoreCase = true)) {
                MediaItemEntity(
                    id = mediaUriOrId,
                    uri = mediaUriOrId,
                    title = "IPTV Live Broadcast",
                    folderName = "IPTV Stream",
                    resolution = "Live Stream"
                )
            } else {
                mediaRepo.getMediaById(mediaUriOrId)
                    ?: mediaRepo.resolveMediaFromUri(Uri.parse(mediaUriOrId))
                    ?: MediaItemEntity(
                        id = mediaUriOrId,
                        uri = mediaUriOrId,
                        title = Uri.parse(mediaUriOrId).lastPathSegment ?: "Media Video",
                        folderName = "Local Media",
                        resolution = "Video"
                    )
            }

            _uiState.update { it.copy(mediaItem = item) }

            val isStream = item.uri.startsWith("http://", ignoreCase = true) || item.uri.startsWith("https://", ignoreCase = true)
            val isLive = isStream && (item.uri.contains(".m3u8", ignoreCase = true) || matchingChannel != null || item.resolution == "Live Stream")

            if (isLive) {
                val currentChannels = _uiState.value.currentIptvChannels
                val channelList = if (currentChannels.isNotEmpty() && currentChannels.any { 
                        it.streamUrl.equals(mediaUriOrId, ignoreCase = true) || it.id.equals(mediaUriOrId, ignoreCase = true) 
                    }) {
                    // Retain current active playlist folder so Next/Prev navigates sequentially! (Requirement 7)
                    currentChannels
                } else {
                    val allFolders = container.iptvRepository.folders.value
                    val matchingFolder = allFolders.firstOrNull { folder ->
                        folder.channels.any { it.streamUrl.equals(mediaUriOrId, ignoreCase = true) || it.id.equals(mediaUriOrId, ignoreCase = true) }
                    }
                    matchingFolder?.channels ?: container.iptvRepository.channels.value
                }
                val cIndex = channelList.indexOfFirst { 
                    it.streamUrl.equals(mediaUriOrId, ignoreCase = true) || it.id.equals(mediaUriOrId, ignoreCase = true) 
                }.coerceAtLeast(0)
                _uiState.update {
                    it.copy(
                        isIptvStream = true,
                        currentIptvChannels = channelList,
                        currentIptvChannelIndex = cIndex
                    )
                }
            } else {
                _uiState.update { it.copy(isIptvStream = false) }
            }

            if (!isLive) {
                // Compute or retrieve video hash for automatic subtitle discovery
                videoHash = try {
                    OpenSubtitlesHasher.computeHashFromUri(
                        getApplication(),
                        Uri.parse(item.uri),
                        item.title
                    )
                } catch (e: Exception) {
                    OpenSubtitlesHasher.generateFallbackHash(item.title)
                }

                // Check cached subtitles in Room
                val cached = subtitleRepo.getCachedSubtitles(videoHash)
                _uiState.update { it.copy(cachedSubtitles = cached) }

                val initialSub = cached.firstOrNull()?.let { File(it.filePath) }
                if (initialSub != null && initialSub.exists()) {
                    activeSubtitleFile = initialSub
                    _uiState.update { it.copy(currentSubtitleTitle = cached.first().language) }
                }

                // Start playback from saved resume position
                val resumePos = if (item.completed) 0L else item.playbackPositionMs
                playerManager.prepareMedia(item.uri, resumePos, activeSubtitleFile)

                // Trigger automated background subtitle search
                if (activeSubtitleFile == null) {
                    autoSearchSubtitles(videoHash, item.title)
                }
            } else {
                // Live IPTV Stream: Start directly without seeking or subtitle hashing
                playerManager.prepareMedia(item.uri, 0L, null)
            }
        }
    }

    private fun autoSearchSubtitles(hash: String, title: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingSubtitles = true) }
            try {
                val candidates = subtitleRepo.searchSubtitles(hash, title, "en")
                _uiState.update {
                    it.copy(
                        isSearchingSubtitles = false,
                        subtitleCandidates = candidates
                    )
                }

                // Auto-download and load the highest accuracy candidate (e.g. 100% or 85%)
                val bestMatch = candidates.firstOrNull()
                if (bestMatch != null && bestMatch.accuracyScore >= 80) {
                    applySubtitleCandidate(bestMatch)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchingSubtitles = false) }
            }
        }
    }

    fun searchSubtitlesManual(query: String? = null, language: String = "en") {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingSubtitles = true) }
            val titleToSearch = query ?: _uiState.value.mediaItem?.title ?: "Video"
            try {
                val results = subtitleRepo.searchSubtitles(videoHash, titleToSearch, language)
                _uiState.update {
                    it.copy(
                        isSearchingSubtitles = false,
                        subtitleCandidates = results
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchingSubtitles = false) }
            }
        }
    }

    fun applySubtitleCandidate(candidate: SubtitleCandidate) {
        viewModelScope.launch {
            val title = _uiState.value.mediaItem?.title ?: candidate.title
            val result = subtitleRepo.downloadAndCacheSubtitle(candidate, videoHash, title)
            result.onSuccess { file ->
                activeSubtitleFile = file
                playerManager.attachSubtitle(file)
                _uiState.update {
                    it.copy(
                        currentSubtitleTitle = "${candidate.language} (${candidate.provider})",
                        subtitleDelayMs = 0L
                    )
                }
            }
        }
    }

    fun setSubtitleDelay(delayMs: Long) {
        val src = activeSubtitleFile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(subtitleDelayMs = delayMs) }
            val delayed = subtitleRepo.applyDelay(src, videoHash, delayMs)
            playerManager.attachSubtitle(delayed)
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.isLocked) return
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
        showControlsTemporarily()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _uiState.value.durationMs)
        exoPlayer.seekTo(clamped)
        _uiState.update { it.copy(currentPositionMs = clamped) }
        showControlsTemporarily()
    }

    fun seekBy(offsetMs: Long) {
        val current = exoPlayer.currentPosition
        val target = (current + offsetMs).coerceIn(0L, _uiState.value.durationMs)
        exoPlayer.seekTo(target)
        _uiState.update {
            it.copy(
                currentPositionMs = target,
                hudState = PlayerHudState.Seek(target, _uiState.value.durationMs, offsetMs)
            )
        }
        scheduleHudHide()
        showControlsTemporarily()
    }

    fun onVerticalSwipe(isRightSide: Boolean, deltaFraction: Float) {
        if (_uiState.value.isLocked) return
        if (isRightSide) {
            // Volume adjustment: deltaFraction positive is up
            val boostAllowed = container.settingsRepository.settings.value.audioBoostEnabled
            val maxVol = if (boostAllowed) 2.0f else 1.0f
            val currentVol = _uiState.value.volume
            val newVol = (currentVol + deltaFraction * 1.5f).coerceIn(0.0f, maxVol)
            playerManager.setVolumeWithBoost(newVol, boostAllowed)
            _uiState.update {
                it.copy(
                    volume = newVol,
                    hudState = PlayerHudState.Volume(newVol, newVol > 1.0f)
                )
            }
        } else {
            // Brightness adjustment: 0.0 to 1.0
            val currentBri = _uiState.value.brightness
            val newBri = (currentBri + deltaFraction * 1.2f).coerceIn(0.01f, 1.0f)
            _uiState.update {
                it.copy(
                    brightness = newBri,
                    hudState = PlayerHudState.Brightness(newBri)
                )
            }
        }
        scheduleHudHide()
    }

    fun onHorizontalSeekDrag(deltaFraction: Float) {
        if (_uiState.value.isLocked) return
        val duration = _uiState.value.durationMs.coerceAtLeast(1000L)
        // Horizontal swipe scrub: full screen width = 90s scrub
        val diffMs = (deltaFraction * 90000L).toLong()
        val current = exoPlayer.currentPosition
        val target = (current + diffMs).coerceIn(0L, duration)
        _uiState.update {
            it.copy(
                hudState = PlayerHudState.Seek(target, duration, diffMs)
            )
        }
    }

    fun onHorizontalSeekEnd() {
        val hud = _uiState.value.hudState
        if (hud is PlayerHudState.Seek) {
            exoPlayer.seekTo(hud.targetTimeMs)
            _uiState.update { it.copy(currentPositionMs = hud.targetTimeMs) }
        }
        scheduleHudHide()
    }

    fun onPinchZoom(scaleMultiplier: Float, panX: Float, panY: Float) {
        if (_uiState.value.isLocked) return
        val newScale = (_uiState.value.zoomScale * scaleMultiplier).coerceIn(1.0f, 5.0f)
        _uiState.update {
            it.copy(
                zoomScale = newScale,
                panOffsetX = (it.panOffsetX + panX).coerceIn(-600f, 600f),
                panOffsetY = (it.panOffsetY + panY).coerceIn(-400f, 400f)
            )
        }
    }

    fun resetPinchZoom() {
        _uiState.update { it.copy(zoomScale = 1.0f, panOffsetX = 0f, panOffsetY = 0f) }
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _uiState.update { it.copy(aspectRatioMode = mode) }
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun toggleLock() {
        val nextLock = !_uiState.value.isLocked
        _uiState.update { it.copy(isLocked = nextLock) }
        if (nextLock) {
            _uiState.update { it.copy(showControls = false) }
        } else {
            showControlsTemporarily()
        }
    }

    fun toggleControls() {
        if (_uiState.value.isLocked) {
            // In locked mode, briefly toggle lock button visibility
            _uiState.update { it.copy(showControls = !it.showControls) }
            return
        }
        val nextState = !_uiState.value.showControls
        _uiState.update { it.copy(showControls = nextState) }
        if (nextState && exoPlayer.isPlaying) {
            scheduleControlsHide()
        }
    }

    fun toggleRemainingTime() {
        _uiState.update { it.copy(showRemainingTime = !it.showRemainingTime) }
    }

    private fun showControlsTemporarily() {
        _uiState.update { it.copy(showControls = true) }
        if (exoPlayer.isPlaying) {
            scheduleControlsHide()
        }
    }

    private fun scheduleControlsHide() {
        controlsTimeoutJob?.cancel()
        controlsTimeoutJob = viewModelScope.launch {
            delay(4000L)
            _uiState.update { it.copy(showControls = false) }
        }
    }

    private fun scheduleHudHide() {
        hudTimeoutJob?.cancel()
        hudTimeoutJob = viewModelScope.launch {
            delay(1200L)
            _uiState.update { it.copy(hudState = PlayerHudState.None) }
        }
    }

    private fun startPositionTracker() {
        positionTrackingJob?.cancel()
        positionTrackingJob = viewModelScope.launch {
            while (true) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition
                    val duration = exoPlayer.duration.coerceAtLeast(0L)
                    val buffered = exoPlayer.bufferedPosition
                    _uiState.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = duration,
                            bufferedPositionMs = buffered
                        )
                    }

                    // Periodically persist resume position every 10 seconds
                    val media = _uiState.value.mediaItem
                    if (media != null && pos > 0) {
                        mediaRepo.savePlaybackPosition(media.id, pos, duration)
                    }

                    // A-B Repeat Loop Check
                    val ptA = _uiState.value.abRepeatPointA
                    val ptB = _uiState.value.abRepeatPointB
                    if (ptA != null && ptB != null && pos >= ptB) {
                        exoPlayer.seekTo(ptA)
                    }
                }
                delay(300L)
            }
        }
    }

    fun setPlayerSkin(skin: com.example.player.PlayerSkin) {
        _uiState.update { it.copy(playerSkin = skin, toastMessage = "Skin: ${skin.displayName}") }
    }

    fun setOrientationLock(lock: com.example.player.OrientationLock) {
        _uiState.update { it.copy(orientationLock = lock, toastMessage = "Orientation: ${lock.displayName}") }
    }

    fun setAudioPreset(preset: com.example.player.AudioPreset) {
        _uiState.update { it.copy(audioPreset = preset, toastMessage = "Equalizer: ${preset.displayName}") }
    }

    fun setAbRepeatPointA() {
        val pos = exoPlayer.currentPosition
        _uiState.update { it.copy(abRepeatPointA = pos, toastMessage = "A-B Loop: Point A set at ${com.example.ui.player.components.formatTime(pos)}") }
    }

    fun setAbRepeatPointB() {
        val pos = exoPlayer.currentPosition
        _uiState.update { it.copy(abRepeatPointB = pos, toastMessage = "A-B Loop: Point B set at ${com.example.ui.player.components.formatTime(pos)}") }
    }

    fun clearAbRepeat() {
        _uiState.update { it.copy(abRepeatPointA = null, abRepeatPointB = null, toastMessage = "A-B Loop cleared") }
    }

    fun toggleMute() {
        val nextMuted = !_uiState.value.isMuted
        if (nextMuted) {
            exoPlayer.volume = 0f
        } else {
            playerManager.setVolumeWithBoost(_uiState.value.volume)
        }
        _uiState.update { it.copy(isMuted = nextMuted, toastMessage = if (nextMuted) "Muted" else "Unmuted") }
    }

    fun cycleAspectRatio() {
        val modes = com.example.player.AspectRatioMode.values()
        val nextIndex = (modes.indexOf(_uiState.value.aspectRatioMode) + 1) % modes.size
        setAspectRatio(modes[nextIndex])
        _uiState.update { it.copy(toastMessage = "Aspect: ${modes[nextIndex].displayName}") }
    }

    fun captureScreenshot() {
        _uiState.update { it.copy(toastMessage = "Frame captured at ${com.example.ui.player.components.formatTime(exoPlayer.currentPosition)}") }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun handleKeyEvent(keyCode: Int): Boolean {
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_SPACE,
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                togglePlayPause()
                true
            }
            android.view.KeyEvent.KEYCODE_DPAD_LEFT,
            android.view.KeyEvent.KEYCODE_MEDIA_REWIND,
            android.view.KeyEvent.KEYCODE_J -> {
                seekBy(-10000L)
                true
            }
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
            android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            android.view.KeyEvent.KEYCODE_L -> {
                seekBy(10000L)
                true
            }
            android.view.KeyEvent.KEYCODE_DPAD_UP,
            android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                onVerticalSwipe(isRightSide = true, deltaFraction = 0.05f)
                true
            }
            android.view.KeyEvent.KEYCODE_DPAD_DOWN,
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                onVerticalSwipe(isRightSide = true, deltaFraction = -0.05f)
                true
            }
            android.view.KeyEvent.KEYCODE_M -> {
                toggleMute()
                true
            }
            android.view.KeyEvent.KEYCODE_F -> {
                cycleAspectRatio()
                true
            }
            android.view.KeyEvent.KEYCODE_C -> {
                captureScreenshot()
                true
            }
            android.view.KeyEvent.KEYCODE_LEFT_BRACKET -> {
                val newSpeed = (_uiState.value.playbackSpeed - 0.25f).coerceIn(0.25f, 4.0f)
                setPlaybackSpeed(newSpeed)
                _uiState.update { it.copy(toastMessage = "Speed: ${newSpeed}x") }
                true
            }
            android.view.KeyEvent.KEYCODE_RIGHT_BRACKET -> {
                val newSpeed = (_uiState.value.playbackSpeed + 0.25f).coerceIn(0.25f, 4.0f)
                setPlaybackSpeed(newSpeed)
                _uiState.update { it.copy(toastMessage = "Speed: ${newSpeed}x") }
                true
            }
            else -> false
        }
    }

    fun toggleDecoderMode() {
        val newMode = when (_uiState.value.decoderMode) {
            DecoderMode.HW -> DecoderMode.HW_PLUS
            DecoderMode.HW_PLUS -> DecoderMode.SW
            DecoderMode.SW -> DecoderMode.HW
        }
        _uiState.update { it.copy(decoderMode = newMode, toastMessage = "Decoder: ${newMode.name}") }
    }

    fun pausePlayback() {
        exoPlayer.pause()
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun stopIfIptvOrExit() {
        val isIptv = _uiState.value.mediaItem?.folderName?.contains("IPTV", ignoreCase = true) == true ||
                _uiState.value.mediaItem?.uri?.contains(".m3u", ignoreCase = true) == true ||
                _uiState.value.mediaItem?.uri?.startsWith("http://", ignoreCase = true) == true ||
                _uiState.value.mediaItem?.uri?.startsWith("https://", ignoreCase = true) == true ||
                _uiState.value.mediaItem?.resolution == "Live Stream"

        exoPlayer.playWhenReady = false
        if (isIptv) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            _uiState.update { it.copy(isPlaying = false, isBuffering = false) }
        } else {
            exoPlayer.pause()
            _uiState.update { it.copy(isPlaying = false) }
        }
        com.example.player.PlaybackNotificationHelper.dismissNotification(getApplication())
    }

    fun saveResumePosition() {
        val media = _uiState.value.mediaItem ?: return
        val pos = exoPlayer.currentPosition
        val duration = exoPlayer.duration.coerceAtLeast(0L)
        viewModelScope.launch {
            mediaRepo.savePlaybackPosition(media.id, pos, duration)
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveResumePosition()
        positionTrackingJob?.cancel()
        controlsTimeoutJob?.cancel()
        hudTimeoutJob?.cancel()
        exoPlayer.removeListener(playerListener)
        playerManager.release()
    }
}
