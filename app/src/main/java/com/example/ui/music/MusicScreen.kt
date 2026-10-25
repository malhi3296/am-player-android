package com.example.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioTrack
import com.example.data.repository.MusicRepository
import com.example.player.EqualizerState
import com.example.player.MusicPlayerManager
import com.example.player.MusicPlayerState
import com.example.player.RepeatState
import com.example.ui.player.components.formatTime
import com.example.ui.theme.AmAccentBlue
import com.example.ui.theme.BoostOrange
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.SaCardBg
import com.example.ui.theme.SaCardBgElevated
import kotlinx.coroutines.launch

@Composable
fun MusicScreen(
    tracks: List<AudioTrack>,
    playerManager: MusicPlayerManager,
    musicRepository: MusicRepository? = null,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerState by playerManager.state.collectAsState()
    val eqState by playerManager.eqState.collectAsState()
    val isOsPermitted by (musicRepository?.isOpenSourcePermitted?.collectAsState() ?: remember { mutableStateOf(false) })

    var searchQuery by remember { mutableStateOf("") }
    var isFullScreenPlayer by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showOpenSourceDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    val coroutineScope = rememberCoroutineScope()

    // Handle back button when full screen player is open
    BackHandler(enabled = isFullScreenPlayer) {
        isFullScreenPlayer = false
    }

    val filteredTracks = tracks.filter { track ->
        val matchesSearch = searchQuery.isBlank() ||
                track.title.contains(searchQuery, ignoreCase = true) ||
                track.artist.contains(searchQuery, ignoreCase = true)
        val matchesCategory = when (selectedFilter) {
            "Open Source" -> track.id.startsWith("os_")
            "Local / Demo" -> !track.id.startsWith("os_")
            else -> true
        }
        matchesSearch && matchesCategory
    }

    if (isFullScreenPlayer && playerState.currentTrack != null) {
        // FULL SCREEN ADVANCED MUSIC PLAYER
        AdvancedFullScreenPlayer(
            track = playerState.currentTrack!!,
            playerState = playerState,
            eqState = eqState,
            playerManager = playerManager,
            onCloseFullScreen = { isFullScreenPlayer = false },
            onOpenEqualizer = { showEqualizerDialog = true }
        )
    } else {
        // STANDARD MUSIC SCREEN VIEW
        Box(modifier = modifier.fillMaxSize().testTag("music_screen")) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Music Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Music Player",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${tracks.size} Tracks • Lock-Screen Background Playback",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AmAccentBlue.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmAccentBlue),
                            modifier = Modifier
                                .clickable { showEqualizerDialog = true }
                                .testTag("open_equalizer_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Equalizer",
                                    tint = AmAccentBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "EQ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmAccentBlue
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onRescan() }
                        ) {
                            Text(
                                text = "Scan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Filter Chips (All, Open Source Streaming, Local)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "All",
                            onClick = { selectedFilter = "All" },
                            label = { Text("All (${tracks.size})", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmAccentBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = selectedFilter == "Open Source",
                            onClick = {
                                if (!isOsPermitted && musicRepository != null) {
                                    showOpenSourceDialog = true
                                } else {
                                    selectedFilter = "Open Source"
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            label = {
                                Text(
                                    if (isOsPermitted) "Open Source Streams" else "Stream Open Source (Opt-In)",
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmAccentBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = selectedFilter == "Local / Demo",
                            onClick = { selectedFilter = "Local / Demo" },
                            label = { Text("Local / Audio Tracks", fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search songs, artists, classical...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AmAccentBlue
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmAccentBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("music_search_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tracks List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(filteredTracks, key = { _, item -> item.id }) { index, track ->
                        val isCurrent = playerState.currentTrack?.id == track.id
                        MusicTrackItem(
                            index = index + 1,
                            track = track,
                            isPlaying = isCurrent && playerState.isPlaying,
                            isSelected = isCurrent,
                            onClick = {
                                playerManager.playTrack(track, filteredTracks)
                            }
                        )
                    }
                }

                // Bottom padding for mini player
                Spacer(modifier = Modifier.height(if (playerState.currentTrack != null) 80.dp else 16.dp))
            }

            // Persistent Mini Player above bottom navigation
            if (playerState.currentTrack != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    MiniMusicPlayerBar(
                        track = playerState.currentTrack!!,
                        isPlaying = playerState.isPlaying,
                        progress = if (playerState.durationMs > 0) {
                            (playerState.currentPositionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f,
                        onPlayPause = { playerManager.togglePlayPause() },
                        onNext = { playerManager.next() },
                        onExpandFullScreen = { isFullScreenPlayer = true },
                        onClick = { isFullScreenPlayer = true }
                    )
                }
            }
        }
    }

    // EQUALIZER DIALOG
    if (showEqualizerDialog) {
        MusicEqualizerDialog(
            eqState = eqState,
            presets = playerManager.presets,
            onToggleEnabled = { playerManager.setEqualizerEnabled(it) },
            onSelectPreset = { playerManager.setEqualizerPreset(it) },
            onBandChange = { bandIdx, level -> playerManager.setBandLevel(bandIdx, level) },
            onBassBoostChange = { playerManager.setBassBoostStrength(it) },
            volumeBoost = playerState.volumeBoost,
            onVolumeBoostChange = { playerManager.setVolumeBoost(it) },
            onDismiss = { showEqualizerDialog = false }
        )
    }

    // OPEN SOURCE STREAMING PERMISSION DIALOG
    if (showOpenSourceDialog && musicRepository != null) {
        AlertDialog(
            onDismissRequest = { showOpenSourceDialog = false },
            containerColor = SaCardBgElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = AmAccentBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stream Open Source Music?", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Allow AM Player to stream high-fidelity open-source and Creative Commons classical & ambient music (Beethoven, Vivaldi, Chopin, Mozart, Bach) from public archives?",
                        fontSize = 13.sp,
                        color = Color(0xFFA1A1AA),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• 100% Royalty Free & Legal\n• Standard internet data rates may apply\n• Can be disabled at any time",
                        fontSize = 12.sp,
                        color = CyanNeon
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            musicRepository.setOpenSourceStreamingPermitted(true)
                            selectedFilter = "Open Source"
                            showOpenSourceDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmAccentBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Allow Streaming", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenSourceDialog = false }) {
                    Text("Stay Offline", color = Color(0xFFA1A1AA))
                }
            }
        )
    }
}

/**
 * Advanced Full Screen Music Player mode
 */
@Composable
fun AdvancedFullScreenPlayer(
    track: AudioTrack,
    playerState: MusicPlayerState,
    eqState: EqualizerState,
    playerManager: MusicPlayerManager,
    onCloseFullScreen: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "music_art_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_deg"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF050508),
                        Color.Black
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .testTag("full_screen_music_player")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCloseFullScreen) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Minimize Player",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmAccentBlue,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = track.album,
                        fontSize = 12.sp,
                        color = Color(0xFFA1A1AA),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onOpenEqualizer) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Equalizer",
                        tint = if (eqState.isEnabled) CyanNeon else Color.White
                    )
                }
            }

            // ROTATING VINYL / ARTWORK WITH GLOWING DISC
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                AmAccentBlue.copy(alpha = 0.35f),
                                Color(0xFF1E1B4B),
                                Color(0xFF0F0F1A)
                            )
                        )
                    )
                    .border(3.dp, AmAccentBlue.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .rotate(if (playerState.isPlaying) rotation else 0f)
                        .clip(CircleShape)
                        .background(Color(0xFF111116))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = "Vinyl Record",
                        tint = AmAccentBlue,
                        modifier = Modifier.size(90.dp)
                    )
                }

                // Center spindle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, AmAccentBlue, CircleShape)
                )
            }

            // REAL-TIME AUDIO SPECTRUM VISUALIZER BARS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                val barHeights = if (playerState.isPlaying) {
                    listOf(14, 28, 20, 36, 18, 30, 24, 34, 16, 26, 32, 20)
                } else {
                    listOf(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
                }

                barHeights.forEachIndexed { idx, h ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(5.dp)
                            .height(h.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (idx % 2 == 0) AmAccentBlue else CyanNeon
                            )
                    )
                }
            }

            // TRACK INFO
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = track.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.artist,
                    fontSize = 14.sp,
                    color = Color(0xFFA1A1AA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x334D6BFE)
                ) {
                    Text(
                        text = "Hi-Res Audio • 24-bit 96kHz Lossless",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmAccentBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // TIMELINE SCRUBBER
            Column(modifier = Modifier.fillMaxWidth()) {
                var isDragging by remember { mutableStateOf(false) }
                var dragProgress by remember { mutableFloatStateOf(0f) }

                val currentProg = if (isDragging) dragProgress else {
                    if (playerState.durationMs > 0) {
                        playerState.currentPositionMs.toFloat() / playerState.durationMs.toFloat()
                    } else 0f
                }

                Slider(
                    value = currentProg.coerceIn(0f, 1f),
                    onValueChange = {
                        isDragging = true
                        dragProgress = it
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        val target = (dragProgress * playerState.durationMs).toLong()
                        playerManager.seekTo(target)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = AmAccentBlue,
                        activeTrackColor = AmAccentBlue,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(playerState.currentPositionMs),
                        fontSize = 12.sp,
                        color = Color(0xFFA1A1AA)
                    )
                    Text(
                        text = formatTime(playerState.durationMs),
                        fontSize = 12.sp,
                        color = Color(0xFFA1A1AA)
                    )
                }
            }

            // PRIMARY CONTROLS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = { playerManager.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerState.isShuffle) CyanNeon else Color(0xFFA1A1AA)
                    )
                }

                // Previous
                IconButton(onClick = { playerManager.previous() }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Big Play/Pause Glowing Button
                Surface(
                    shape = CircleShape,
                    color = AmAccentBlue,
                    modifier = Modifier
                        .size(72.dp)
                        .clickable { playerManager.togglePlayPause() }
                        .testTag("full_player_play_pause")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Next
                IconButton(onClick = { playerManager.next() }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat Mode
                IconButton(onClick = { playerManager.toggleRepeat() }) {
                    Icon(
                        imageVector = if (playerState.repeatState == RepeatState.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat Mode",
                        tint = if (playerState.repeatState != RepeatState.OFF) CyanNeon else Color(0xFFA1A1AA)
                    )
                }
            }

            // BOTTOM TOOLBAR: Lock-Screen Notification status & speed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lock screen background playback indicator
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x3310B981)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lock Screen Playback Active", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                }

                // Speed selector
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x334D6BFE),
                    modifier = Modifier.clickable {
                        val nextSpeed = when (playerState.playbackSpeed) {
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2.0f
                            2.0f -> 0.75f
                            else -> 1.0f
                        }
                        playerManager.setPlaybackSpeed(nextSpeed)
                    }
                ) {
                    Text(
                        text = "Speed: ${playerState.playbackSpeed}x",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmAccentBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 5-Band Equalizer Dialog with Presets, Frequency Sliders & Bass Boost
 */
@Composable
fun MusicEqualizerDialog(
    eqState: EqualizerState,
    presets: List<com.example.player.EqualizerPreset>,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (String) -> Unit,
    onBandChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    volumeBoost: Float,
    onVolumeBoostChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SaCardBgElevated,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = AmAccentBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Audio Equalizer", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Switch(
                    checked = eqState.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = AmAccentBlue)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Presets",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFA1A1AA)
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presets) { preset ->
                        val isSelected = eqState.currentPreset.equals(preset.name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) AmAccentBlue else Color(0xFF27272A),
                            modifier = Modifier.clickable { onSelectPreset(preset.name) }
                        ) {
                            Text(
                                text = preset.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFFA1A1AA),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "5-Band Graphic Equalizer (-15dB to +15dB)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFA1A1AA)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 5 Frequency Sliders
                eqState.bandLevels.forEachIndexed { idx, levelMb ->
                    val label = bandLabels.getOrElse(idx) { "Band $idx" }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.width(48.dp)
                        )

                        Slider(
                            value = levelMb.toFloat(),
                            onValueChange = { onBandChange(idx, it.toInt()) },
                            valueRange = -1500f..1500f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanNeon,
                                activeTrackColor = CyanNeon,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${levelMb / 100} dB",
                            fontSize = 10.sp,
                            color = CyanNeon,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bass Boost Strength
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Bass Boost",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.width(80.dp)
                    )

                    Slider(
                        value = eqState.bassBoostStrength.toFloat(),
                        onValueChange = { onBassBoostChange(it.toInt()) },
                        valueRange = 0f..1000f,
                        colors = SliderDefaults.colors(
                            thumbColor = AmAccentBlue,
                            activeTrackColor = AmAccentBlue,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${eqState.bassBoostStrength / 10}%",
                        fontSize = 10.sp,
                        color = AmAccentBlue,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }

                // Volume 200% Enhancer Boost
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Audio Boost",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.width(80.dp)
                    )

                    Slider(
                        value = volumeBoost,
                        onValueChange = onVolumeBoostChange,
                        valueRange = 1.0f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = BoostOrange,
                            activeTrackColor = BoostOrange,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${(volumeBoost * 100).toInt()}%",
                        fontSize = 10.sp,
                        color = BoostOrange,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = AmAccentBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun MusicTrackItem(
    index: Int,
    track: AudioTrack,
    isPlaying: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isOpenSource = track.id.startsWith("os_")

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) AmAccentBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) AmAccentBlue else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("music_item_$index")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) AmAccentBlue else Color(0xFF23232B)),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (isOpenSource) Icons.Default.Public else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else AmAccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) AmAccentBlue else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOpenSource) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0x3310B981),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "OPEN SOURCE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = "${track.artist} • ${track.album}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatTime(track.durationMs),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MiniMusicPlayerBar(
    track: AudioTrack,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpandFullScreen: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SaCardBgElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, AmAccentBlue.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("mini_player_bar")
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmAccentBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = AmAccentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        fontSize = 11.sp,
                        color = Color(0xFFA1A1AA),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Expand to Full Screen button
                IconButton(onClick = onExpandFullScreen) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Expand to Full Screen Player",
                        tint = CyanNeon,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                color = AmAccentBlue,
                trackColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
    }
}
