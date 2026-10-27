package com.example.ui.player.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IptvChannel
import com.example.player.AspectRatioMode
import com.example.player.PlayerSkin
import com.example.player.VideoColorMode
import com.example.ui.theme.*

@Composable
fun AspectRatioDialog(
    currentMode: AspectRatioMode,
    onSelectMode: (AspectRatioMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CropFree, contentDescription = null, tint = CyanNeon)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aspect Ratio", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AspectRatioMode.values().forEach { mode ->
                    val isSelected = mode == currentMode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) DarkSurfaceElevated else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectMode(mode)
                                onDismiss()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSelectMode(mode)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = mode.displayName,
                                color = if (isSelected) CyanNeon else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyanNeon)
            }
        }
    )
}

@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f, 4.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = CyanNeon)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Playback Speed", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(240.dp)
            ) {
                items(speeds) { speed ->
                    val isSelected = speed == currentSpeed
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) DarkSurfaceElevated else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectSpeed(speed)
                                onDismiss()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSelectSpeed(speed)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${speed}x",
                                color = if (isSelected) CyanNeon else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyanNeon)
            }
        }
    )
}

@Composable
fun AudioTrackDialog(
    tracks: List<Pair<Int, String>>,
    selectedIndex: Int,
    onSelectTrack: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Audiotrack, contentDescription = null, tint = CyanNeon)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Audio Stream Tracks", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (tracks.isEmpty()) {
                Text(
                    text = "Default Stereo Stream (HW Accelerated)",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(tracks) { (index, name) ->
                        val isSelected = index == selectedIndex
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) DarkSurfaceElevated else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectTrack(index)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSelectTrack(index)
                                        onDismiss()
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = name,
                                    color = if (isSelected) CyanNeon else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyanNeon)
            }
        }
    )
}

@Composable
fun PlayerSkinDialog(
    currentSkin: com.example.player.PlayerSkin,
    onSelectSkin: (com.example.player.PlayerSkin) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Player Skins", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(com.example.player.PlayerSkin.values()) { skin ->
                    val isSelected = skin == currentSkin
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) skin.cardColor else DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) skin.accentColor else Color(0x33FFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectSkin(skin)
                                onDismiss()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = skin.accentColor,
                                modifier = Modifier.size(24.dp)
                            ) {}

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = skin.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) skin.accentColor else Color.White
                                )
                                Text(
                                    text = skin.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFFA1A1AA)
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = skin.accentColor
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = currentSkin.accentColor)
            }
        }
    )
}

@Composable
fun PlayerMoreOptionsDialog(
    onOpenColorModes: () -> Unit,
    onOpenSkins: () -> Unit,
    onSetPointA: () -> Unit,
    onSetPointB: () -> Unit,
    onClearLoop: () -> Unit,
    onCaptureFrame: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Player Tools & Features", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth().clickable { onOpenColorModes(); onDismiss() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Video Color Modes (Vivid, Warm, Eye Care)", color = Color.White)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth().clickable { onOpenSkins(); onDismiss() }
                ) {
                    Text("Change Player Skin / Theme", color = Color.White, modifier = Modifier.padding(14.dp))
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth().clickable { onCaptureFrame(); onDismiss() }
                ) {
                    Text("Capture Frame Screenshot (C)", color = Color.White, modifier = Modifier.padding(14.dp))
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth().clickable { onSetPointA(); onDismiss() }
                ) {
                    Text("Set A-B Loop: Point A (Start)", color = Color.White, modifier = Modifier.padding(14.dp))
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth().clickable { onSetPointB(); onDismiss() }
                ) {
                    Text("Set A-B Loop: Point B (End)", color = Color.White, modifier = Modifier.padding(14.dp))
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth().clickable { onClearLoop(); onDismiss() }
                ) {
                    Text("Clear A-B Repeat Loop", color = Color(0xFFFF5252), modifier = Modifier.padding(14.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyanNeon)
            }
        }
    )
}

@Composable
fun VideoColorModeDialog(
    currentMode: com.example.player.VideoColorMode,
    onSelectMode: (com.example.player.VideoColorMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = CyanNeon)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Video Color Modes", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                com.example.player.VideoColorMode.values().forEach { mode ->
                    val isSelected = mode == currentMode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) DarkSurfaceElevated else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyanNeon) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectMode(mode)
                                onDismiss()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSelectMode(mode)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mode.displayName,
                                    color = if (isSelected) CyanNeon else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = mode.description,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyanNeon)
            }
        }
    )
}



@Composable
fun SubtitleTrackDialog(
    tracks: List<Pair<Int, String>>,
    selectedIndex: Int,
    onSelectTrack: (Int) -> Unit,
    onSearchOnlineClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Subtitles,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Subtitles", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Off option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectTrack(-1)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Off (Disable)",
                        color = if (selectedIndex < 0) MaterialTheme.colorScheme.primary else Color.White,
                        fontWeight = if (selectedIndex < 0) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedIndex < 0) {
                        Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                // Embedded tracks
                if (tracks.isEmpty()) {
                    Text(
                        "No embedded subtitle tracks",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
                        items(tracks) { (trackId, trackName) ->
                            val isSelected = selectedIndex == trackId || trackName.startsWith("✓")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectTrack(trackId)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = trackName,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))

                // 1. Search Online Subtitle Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onDismiss()
                            onSearchOnlineClick()
                        }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Search Online Subtitles",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                // 2. Subtitle Settings link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onDismiss()
                            onSettingsClick()
                        }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Subtitle Settings (Color, Size, Sync)",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun SubtitleSettingsDialog(
    currentSizeSp: Int,
    currentColorHex: Long,
    currentBottom: Boolean,
    onSaveSettings: (sizeSp: Int, colorHex: Long, isBottom: Boolean) -> Unit,
    onSyncAdjust: (offsetMs: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var size by remember { androidx.compose.runtime.mutableIntStateOf(currentSizeSp) }
    var color by remember { androidx.compose.runtime.mutableLongStateOf(currentColorHex) }
    var isBottom by remember { mutableStateOf(currentBottom) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Subtitle Settings", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Font Size", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(14 to "Small", 18 to "Normal", 22 to "Large", 26 to "Extra").forEach { (sz, label) ->
                        androidx.compose.material3.FilterChip(
                            selected = size == sz,
                            onClick = { size = sz },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Color", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        0xFFFFFFFF to Color.White,
                        0xFFFFEB3B to Color(0xFFFFEB3B),
                        0xFF00E676 to Color(0xFF00E676),
                        0xFF00E5FF to Color(0xFF00E5FF)
                    ).forEach { (hex, col) ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (color == hex) 3.dp else 1.dp,
                                    color = if (color == hex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { color = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Position", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = isBottom,
                        onClick = { isBottom = true },
                        label = { Text("Bottom", fontSize = 12.sp) }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = !isBottom,
                        onClick = { isBottom = false },
                        label = { Text("Top", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Sync Adjustment", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    androidx.compose.material3.OutlinedButton(onClick = { onSyncAdjust(-500L) }) {
                        Text("-0.5s", fontSize = 12.sp)
                    }
                    androidx.compose.material3.OutlinedButton(onClick = { onSyncAdjust(0L) }) {
                        Text("Reset", fontSize = 12.sp)
                    }
                    androidx.compose.material3.OutlinedButton(onClick = { onSyncAdjust(500L) }) {
                        Text("+0.5s", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    onSaveSettings(size, color, isBottom)
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun IptvQuickChannelDialog(
    channels: List<com.example.data.model.IptvChannel>,
    currentIndex: Int,
    onSelectChannel: (com.example.data.model.IptvChannel) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, channels) {
        if (searchQuery.isBlank()) channels else channels.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
        }
    }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = (currentIndex - 2).coerceAtLeast(0)
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF00E676), CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Live IPTV Channels (${channels.size})", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter channels...", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(filtered) { channel ->
                        val isCurrent = channels.indexOf(channel) == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    onSelectChannel(channel)
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF00E676), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = channel.name,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (channel.category.isNotBlank()) {
                                Text(
                                    text = channel.category,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp)
    )
}
