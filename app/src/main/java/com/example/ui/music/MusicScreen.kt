package com.example.ui.music

import androidx.compose.material.icons.filled.Radio

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
import androidx.compose.material.icons.filled.Folder
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
    settingsRepository: com.example.data.repository.SettingsRepository? = null,
    radioStations: List<com.example.data.model.IptvChannel> = emptyList(),
    onRescan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerState by playerManager.state.collectAsState()
    val eqState by playerManager.eqState.collectAsState()
    val settingsState by (settingsRepository?.settings?.collectAsState() ?: remember { mutableStateOf(null) })
    val isOnlineMusicEnabled = settingsState?.onlineMusicEnabled ?: false

    var searchQuery by remember { mutableStateOf("") }
    var activeMusicTab by remember { mutableStateOf("Songs") }
    // radioStations passed as parameter
    var isFullScreenPlayer by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showAddStreamDialog by remember { mutableStateOf(false) }

    // View modes: "Songs", "Albums", "Artists", "Folders", "Radio"
    var viewMode by remember { mutableStateOf("Songs") }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var selectedRadioFolder by remember { mutableStateOf<String?>(null) }

    // Sorting: "A-Z", "Z-A", "Duration", "Artist"
    var sortOrder by remember { mutableStateOf("A-Z") }
    var showSortMenu by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Handle back button when full screen player or sub-view is open
    BackHandler(enabled = isFullScreenPlayer || selectedAlbum != null || selectedArtist != null || selectedFolder != null || selectedRadioFolder != null) {
        when {
            isFullScreenPlayer -> isFullScreenPlayer = false
            selectedAlbum != null -> selectedAlbum = null
            selectedArtist != null -> selectedArtist = null
            selectedFolder != null -> selectedFolder = null
            selectedRadioFolder != null -> selectedRadioFolder = null
        }
    }

    val visibleTracks = remember(tracks, selectedAlbum, selectedArtist, selectedFolder, viewMode) {
        when {
            selectedAlbum != null -> tracks.filter { it.album.equals(selectedAlbum, ignoreCase = true) }
            selectedArtist != null -> tracks.filter { it.artist.equals(selectedArtist, ignoreCase = true) }
            selectedFolder != null -> tracks.filter { it.folderName.equals(selectedFolder, ignoreCase = true) }
            else -> tracks
        }
    }

    val filteredTracks = remember(visibleTracks, searchQuery, sortOrder) {
        val searched = visibleTracks.filter { track ->
            searchQuery.isBlank() ||
                    track.title.contains(searchQuery, ignoreCase = true) ||
                    track.artist.contains(searchQuery, ignoreCase = true) ||
                    track.album.contains(searchQuery, ignoreCase = true)
        }
        when (sortOrder) {
            "A-Z" -> searched.sortedBy { it.title.lowercase() }
            "Z-A" -> searched.sortedByDescending { it.title.lowercase() }
            "Duration" -> searched.sortedByDescending { it.durationMs }
            "Artist" -> searched.sortedBy { it.artist.lowercase() }
            else -> searched
        }
    }

    val albums = remember(tracks) {
        tracks.groupBy { it.album }.map { (album, songList) -> album to songList }
    }
    val artists = remember(tracks) {
        tracks.groupBy { it.artist }.map { (artist, songList) -> artist to songList }
    }
    val audioFolders = remember(tracks) {
        tracks.groupBy { it.folderName }.map { (folder, songList) -> folder to songList }
    }

    if (isFullScreenPlayer && playerState.currentTrack != null) {
        AdvancedFullScreenPlayer(
            track = playerState.currentTrack!!,
            playerState = playerState,
            eqState = eqState,
            playerManager = playerManager,
            onCloseFullScreen = { isFullScreenPlayer = false },
            onOpenEqualizer = { showEqualizerDialog = true }
        )
    } else {
        Box(modifier = modifier.fillMaxSize().testTag("music_screen")) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Music Section Header - Clean heading without subheadings (Item 13)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedAlbum != null || selectedArtist != null || selectedFolder != null || selectedRadioFolder != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                selectedAlbum = null
                                selectedArtist = null
                                selectedFolder = null
                                selectedRadioFolder = null
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedAlbum ?: selectedArtist ?: selectedFolder ?: selectedRadioFolder ?: "Music",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = "Music",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOnlineMusicEnabled) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clickable { showAddStreamDialog = true }
                                    .padding(end = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Stream", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (eqState.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
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
                                    tint = if (eqState.isEnabled) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "EQ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (eqState.isEnabled) Color.White else MaterialTheme.colorScheme.onSurface
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

                // View Modes: Songs, Albums, Artists, Folders, Radio
                if (selectedAlbum == null && selectedArtist == null && selectedFolder == null && selectedRadioFolder == null) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Songs", "Albums", "Artists", "Folders", "Radio").forEach { mode ->
                            val isSelected = viewMode == mode
                            item {
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { 
                                        viewMode = mode 
                                        selectedRadioFolder = null
                                    },
                                    label = { Text(mode, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Search Bar + Sort Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search songs, artists, albums...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
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
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("music_search_input")
                    )

                    if (viewMode == "Songs" || selectedAlbum != null || selectedArtist != null || selectedFolder != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable {
                                    sortOrder = when (sortOrder) {
                                        "A-Z" -> "Z-A"
                                        "Z-A" -> "Duration"
                                        "Duration" -> "Artist"
                                        else -> "A-Z"
                                    }
                                }
                        ) {
                            Text(
                                text = sortOrder,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content View according to active mode
                when {
                    selectedAlbum != null || selectedArtist != null || selectedFolder != null || viewMode == "Songs" -> {
                        // Songs List
                        if (filteredTracks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No audio tracks found on device", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
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
                        }
                    }

                    viewMode == "Albums" -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            items(albums, key = { it.first }) { (albumName, albumTracks) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedAlbum = albumName }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Album, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(albumName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text("${albumTracks.size} songs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    viewMode == "Artists" -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            items(artists, key = { it.first }) { (artistName, artistTracks) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedArtist = artistName }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(artistName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text("${artistTracks.size} songs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    viewMode == "Folders" -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            items(audioFolders, key = { it.first }) { (folderName, folderTracks) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedFolder = folderName }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(folderName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text("${folderTracks.size} songs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    viewMode == "Radio" -> {
                        val indianStations = remember(radioStations) {
                            radioStations.filter { it.category.contains("Indian", ignoreCase = true) || it.country.equals("IN", ignoreCase = true) }
                        }
                        val intlStations = remember(radioStations) {
                            radioStations.filter { it.category.contains("International", ignoreCase = true) || !it.country.equals("IN", ignoreCase = true) }
                        }

                        if (selectedRadioFolder == null && searchQuery.isBlank()) {
                            // Two distinct folder cards: "Indian Radio" and "International Radio"
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedRadioFolder = "Indian Radio" }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(Color(0xFFFF9933), Color(0xFF138808))
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Radio,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Indian Radio",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${indianStations.size} Channels • AIR Vividh Bharati, Mirchi, City & more",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                item {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedRadioFolder = "International Radio" }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(Color(0xFF2196F3), Color(0xFF9C27B0))
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Public,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "International Radio",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${intlStations.size} Channels • BBC World, NPR, Capital FM & more",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            val activeStations = remember(radioStations, selectedRadioFolder, searchQuery) {
                                val pool = when (selectedRadioFolder) {
                                    "Indian Radio" -> indianStations
                                    "International Radio" -> intlStations
                                    else -> radioStations
                                }
                                pool.filter { radio ->
                                    searchQuery.isBlank() ||
                                        radio.name.contains(searchQuery, ignoreCase = true) ||
                                        radio.category.contains(searchQuery, ignoreCase = true) ||
                                        radio.country.contains(searchQuery, ignoreCase = true)
                                }
                            }

                            if (activeStations.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Radio, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No Radio Stations Found", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                ) {
                                    items(activeStations, key = { it.id }) { station ->
                                        val isCurrent = playerState.currentTrack?.id == station.id
                                        val isPlaying = isCurrent && playerState.isPlaying

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                            border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val radioTrack = AudioTrack(
                                                        id = station.id,
                                                        uri = station.streamUrl,
                                                        title = station.name,
                                                        artist = station.category.ifBlank { "Live Radio" },
                                                        album = "Live Radio (${station.country})",
                                                        folderName = selectedRadioFolder ?: "Radio",
                                                        durationMs = 0L,
                                                        sizeBytes = 0L
                                                    )
                                                    val allRadioTracks = activeStations.map {
                                                        AudioTrack(
                                                            id = it.id,
                                                            uri = it.streamUrl,
                                                            title = it.name,
                                                            artist = it.category.ifBlank { "Live Radio" },
                                                            album = "Live Radio (${it.country})",
                                                            folderName = selectedRadioFolder ?: "Radio",
                                                            durationMs = 0L,
                                                            sizeBytes = 0L
                                                        )
                                                    }
                                                    playerManager.playTrack(radioTrack, allRadioTracks)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.Radio,
                                                        contentDescription = null,
                                                        tint = if (isPlaying) Color.White else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = station.name,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = station.category.ifBlank { "Radio" },
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        if (station.country.isNotBlank()) {
                                                            Text(
                                                                text = " • ${station.country}",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val radioTrack = AudioTrack(
                                                            id = station.id,
                                                            uri = station.streamUrl,
                                                            title = station.name,
                                                            artist = station.category.ifBlank { "Live Radio" },
                                                            album = "Live Radio (${station.country})",
                                                            folderName = selectedRadioFolder ?: "Radio",
                                                            durationMs = 0L,
                                                            sizeBytes = 0L
                                                        )
                                                        if (isCurrent) {
                                                            playerManager.togglePlayPause()
                                                        } else {
                                                            playerManager.playTrack(radioTrack, listOf(radioTrack))
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                        contentDescription = "Play/Pause",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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

    // ADD ONLINE STREAMING TRACK / PLAYLIST DIALOG
    if (showAddStreamDialog && musicRepository != null) {
        var streamTitle by remember { mutableStateOf("") }
        var streamUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddStreamDialog = false },
            containerColor = SaCardBgElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Add Online Streaming Track", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter a title and audio stream URL (.mp3, .aac, .m3u, .ogg):",
                        fontSize = 12.sp,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = streamTitle,
                        onValueChange = { streamTitle = it },
                        placeholder = { Text("Stream Title", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF3F3F46)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        placeholder = { Text("https://example.com/stream.mp3", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF3F3F46)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (streamUrl.isNotBlank()) {
                            musicRepository.addCustomStream(
                                title = streamTitle.ifBlank { "Online Stream" },
                                url = streamUrl,
                                artist = "Streaming Audio"
                            )
                        }
                        showAddStreamDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Stream", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStreamDialog = false }) {
                    Text("Cancel", color = Color(0xFFA1A1AA))
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
                        color = MaterialTheme.colorScheme.primary,
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
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                Color(0xFF1E1B4B),
                                Color(0xFF0F0F1A)
                            )
                        )
                    )
                    .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(90.dp)
                    )
                }

                // Center spindle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
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
                                if (idx % 2 == 0) MaterialTheme.colorScheme.primary else CyanNeon
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
                        color = MaterialTheme.colorScheme.primary,
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
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
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
                    color = MaterialTheme.colorScheme.primary,
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
                        color = MaterialTheme.colorScheme.primary,
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
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Audio Equalizer", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Switch(
                    checked = eqState.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
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
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF27272A),
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
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${eqState.bassBoostStrength / 10}%",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
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
                Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
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
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF23232B)),
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
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
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
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
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
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
    }
}