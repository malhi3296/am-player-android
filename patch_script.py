import os

def patch_all():
    # 1. Patch ExoPlayerManager.kt
    p1 = 'app/src/main/java/com/example/player/ExoPlayerManager.kt'
    with open(p1, 'r', encoding='utf-8') as f:
        c1 = f.read()

    if 'var onTracksChangedListener' not in c1:
        c1 = c1.replace(
            'class ExoPlayerManager(private val context: Context) {',
            'class ExoPlayerManager(private val context: Context) {\n    var onTracksChangedListener: (() -> Unit)? = null'
        )

    if 'onTracksChangedListener?.invoke()' not in c1:
        c1 = c1.replace(
            '.setSeekForwardIncrementMs(10000L)\n            .build()',
            '''.setSeekForwardIncrementMs(10000L)
            .build()
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                onTracksChangedListener?.invoke()
            }
        })'''
        )

    old_tracks = """    fun selectAudioTrack(index: Int) {
        val player = exoPlayer ?: return
        trackSelector?.let { selector ->
            val builder = selector.parameters.buildUpon()
            // Track selection
            builder.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            selector.parameters = builder.build()
        }
    }"""

    new_tracks = """    fun selectAudioTrack(index: Int) {
        val player = exoPlayer ?: return
        val currentTracks = player.currentTracks
        var counter = 0
        for (g in 0 until currentTracks.groups.size) {
            val group = currentTracks.groups[g]
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (t in 0 until group.length) {
                    if (counter == index) {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, t))
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                            .build()
                        return
                    }
                    counter++
                }
            }
        }
    }

    fun getSubtitleTracks(): List<Pair<Int, String>> {
        val player = exoPlayer ?: return emptyList()
        val list = mutableListOf<Pair<Int, String>>()
        val currentTracks = player.currentTracks
        for (g in 0 until currentTracks.groups.size) {
            val group = currentTracks.groups[g]
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (t in 0 until group.length) {
                    val format = group.getTrackFormat(t)
                    val label = format.label ?: format.language ?: ("Subtitle Track " + (list.size + 1))
                    list.add(Pair(list.size, label))
                }
            }
        }
        return list
    }

    fun selectSubtitleTrack(index: Int) {
        val player = exoPlayer ?: return
        if (index < 0) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            return
        }
        val currentTracks = player.currentTracks
        var counter = 0
        for (g in 0 until currentTracks.groups.size) {
            val group = currentTracks.groups[g]
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (t in 0 until group.length) {
                    if (counter == index) {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, t))
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .build()
                        return
                    }
                    counter++
                }
            }
        }
    }"""

    if old_tracks in c1:
        c1 = c1.replace(old_tracks, new_tracks)
        with open(p1, 'w', encoding='utf-8') as f:
            f.write(c1)
        print("P1: ExoPlayerManager updated")

    # 2. Patch PlayerViewModel.kt
    p2 = 'app/src/main/java/com/example/ui/player/PlayerViewModel.kt'
    with open(p2, 'r', encoding='utf-8') as f:
        c2 = f.read()

    old_state = "val seekDurationSeconds: Int = 10\n)"
    new_state = """val seekDurationSeconds: Int = 10,
    val audioTracks: List<Pair<Int, String>> = emptyList(),
    val selectedAudioTrackIndex: Int = 0,
    val subtitleTracks: List<Pair<Int, String>> = emptyList(),
    val selectedSubtitleTrackIndex: Int = -1,
    val noSubtitlesFound: Boolean = false,
    val subtitleColor: String = "#FFFFFF",
    val subtitleSizeSp: Int = 18,
    val subtitlePlacement: String = "Bottom",
    val isIptvStream: Boolean = false,
    val iptvPlaylistChannels: List<com.example.data.model.IptvChannel> = emptyList()
)"""

    if old_state in c2:
        c2 = c2.replace(old_state, new_state)

    old_init = "    init {\n        // Observe settings changes"
    new_init = """    init {
        playerManager.onTracksChangedListener = {
            viewModelScope.launch(Dispatchers.Main) {
                refreshTracks()
            }
        }
        // Observe settings changes"""

    if old_init in c2:
        c2 = c2.replace(old_init, new_init)

    if 'fun refreshTracks()' not in c2:
        last_brace = c2.rfind('}')
        helpers = """
    fun refreshTracks() {
        val aTracks = playerManager.getAudioTracks()
        val sTracks = playerManager.getSubtitleTracks()
        _uiState.update { it.copy(audioTracks = aTracks, subtitleTracks = sTracks) }
    }

    fun selectAudioTrack(index: Int) {
        playerManager.selectAudioTrack(index)
        _uiState.update { it.copy(selectedAudioTrackIndex = index, toastMessage = "Audio Track " + (index + 1) + " Selected") }
    }

    fun selectSubtitleTrack(index: Int) {
        playerManager.selectSubtitleTrack(index)
        _uiState.update { it.copy(selectedSubtitleTrackIndex = index, toastMessage = if (index < 0) "Subtitles Off" else "Subtitle " + (index + 1) + " Selected") }
    }

    fun setSubtitleColor(colorHex: String) {
        _uiState.update { it.copy(subtitleColor = colorHex) }
    }

    fun setSubtitleSizeSp(size: Int) {
        _uiState.update { it.copy(subtitleSizeSp = size) }
    }

    fun setSubtitlePlacement(placement: String) {
        _uiState.update { it.copy(subtitlePlacement = placement) }
    }

    fun playNextChannel() {
        val channels = _uiState.value.iptvPlaylistChannels
        if (channels.isNotEmpty()) {
            val currentUri = _uiState.value.mediaItem?.uri ?: ""
            val curIdx = channels.indexOfFirst { it.streamUrl.equals(currentUri, ignoreCase = true) }
            val nextIdx = if (curIdx != -1) (curIdx + 1) % channels.size else 0
            val nextChan = channels[nextIdx]
            loadMedia(nextChan.streamUrl)
        }
    }

    fun playPreviousChannel() {
        val channels = _uiState.value.iptvPlaylistChannels
        if (channels.isNotEmpty()) {
            val currentUri = _uiState.value.mediaItem?.uri ?: ""
            val curIdx = channels.indexOfFirst { it.streamUrl.equals(currentUri, ignoreCase = true) }
            val prevIdx = if (curIdx > 0) curIdx - 1 else channels.size - 1
            val prevChan = channels[prevIdx]
            loadMedia(prevChan.streamUrl)
        }
    }
"""
        c2 = c2[:last_brace] + helpers + "\n}\n"

    old_live = """            } else {
                // Live IPTV Stream: Start directly without seeking or subtitle hashing
                playerManager.prepareMedia(item.uri, 0L, null)
            }"""

    new_live = """            } else {
                // Live IPTV Stream: Start directly without seeking or subtitle hashing
                val allChannels = container.iptvRepository.channels.value
                _uiState.update { it.copy(isIptvStream = true, iptvPlaylistChannels = allChannels) }
                playerManager.prepareMedia(item.uri, 0L, null)
            }"""

    if old_live in c2:
        c2 = c2.replace(old_live, new_live)

    old_search = """    fun searchSubtitlesManual(query: String, language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSearchingSubtitles = true) }
            try {
                val results = subtitleRepo.searchSubtitles(videoHash, query, language)
                _uiState.update { it.copy(isSearchingSubtitles = false, subtitleCandidates = results) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchingSubtitles = false) }
            }
        }
    }"""

    new_search = """    fun searchSubtitlesManual(query: String, language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSearchingSubtitles = true, noSubtitlesFound = false) }
            try {
                val results = subtitleRepo.searchSubtitles(videoHash, query, language)
                val noneFound = results.isEmpty()
                _uiState.update { 
                    it.copy(
                        isSearchingSubtitles = false, 
                        subtitleCandidates = results,
                        noSubtitlesFound = noneFound,
                        toastMessage = if (noneFound) "No subtitles found for this video" else null
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchingSubtitles = false, noSubtitlesFound = true, toastMessage = "No subtitles found for this video") }
            }
        }
    }"""

    if old_search in c2:
        c2 = c2.replace(old_search, new_search)

    with open(p2, 'w', encoding='utf-8') as f:
        f.write(c2)
    print("P2: PlayerViewModel updated")

patch_all()
EOF
