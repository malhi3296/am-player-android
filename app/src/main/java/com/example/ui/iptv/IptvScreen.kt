package com.example.ui.iptv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IptvChannel
import com.example.data.model.IptvPlaylistFolder
import com.example.data.repository.IptvRepository
import com.example.ui.theme.AmAccentBlue
import com.example.ui.theme.SaCardBg
import com.example.ui.theme.SaCardBgElevated
import com.example.ui.theme.SaFolderBadgeRed

fun parseHexColor(hex: String, fallback: Color = AmAccentBlue): Color {
    return try {
        val clean = hex.replace("#", "")
        val longVal = clean.toLong(16)
        if (clean.length == 6) {
            Color(longVal or 0x00000000FF000000)
        } else {
            Color(longVal)
        }
    } catch (_: Exception) {
        fallback
    }
}

fun getIconForName(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "sports" -> Icons.Default.SportsSoccer
        "news" -> Icons.Default.Newspaper
        "movie" -> Icons.Default.Movie
        "music" -> Icons.Default.MusicNote
        "science" -> Icons.Default.RocketLaunch
        "globe" -> Icons.Default.Public
        "star" -> Icons.Default.Star
        else -> Icons.Default.LiveTv
    }
}

val availableFolderColors = listOf(
    "#4D6BFE", // AM Blue
    "#00E5FF", // Neon Cyan
    "#EF4444", // Coral Red
    "#10B981", // Emerald
    "#8B5CF6", // Purple
    "#F59E0B", // Amber
    "#EC4899", // Rose Pink
    "#64748B"  // Slate
)

val availableFolderIcons = listOf(
    "tv" to "TV Live",
    "news" to "News",
    "sports" to "Sports",
    "movie" to "Cinema",
    "music" to "Music",
    "science" to "Science",
    "globe" to "World",
    "star" to "Featured"
)

@Composable
fun IptvScreen(
    folders: List<IptvPlaylistFolder>,
    repository: IptvRepository,
    onPlayChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var showEditFolderDialog by remember { mutableStateOf<IptvPlaylistFolder?>(null) }
    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var showAddChannelDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }

    // Handle back button when inside a playlist folder
    BackHandler(enabled = selectedFolderId != null) {
        selectedFolderId = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("iptv_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedFolder == null) {
            // PLAYLIST FOLDERS ROOT VIEW (just like home page for videos!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "IPTV Playlists",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${folders.size} M3U Playlist Folders • Live Streams",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmAccentBlue,
                    modifier = Modifier
                        .clickable { showAddPlaylistDialog = true }
                        .testTag("add_m3u_playlist_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add M3U Playlist",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add M3U",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3-Column Playlist Folders Grid (matching video folders UI!)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("iptv_folders_grid")
            ) {
                items(folders, key = { it.id }) { folder ->
                    IptvFolderGridItem(
                        folder = folder,
                        onClick = { selectedFolderId = folder.id },
                        onEdit = { showEditFolderDialog = folder }
                    )
                }
            }
        } else {
            // INSIDE PLAYLIST FOLDER VIEW
            val folderColor = parseHexColor(selectedFolder.colorHex)
            val folderIcon = getIconForName(selectedFolder.iconName)

            // Header with back button & customize button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { selectedFolderId = null },
                        modifier = Modifier.testTag("iptv_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Playlists",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(folderColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = folderIcon,
                            contentDescription = null,
                            tint = folderColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = selectedFolder.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${selectedFolder.channels.size} Live Channels",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showEditFolderDialog = selectedFolder }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Playlist Folder",
                            tint = folderColor
                        )
                    }
                    IconButton(onClick = { showAddChannelDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Channel",
                            tint = AmAccentBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search ${selectedFolder.name} channels...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = folderColor
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
                    focusedBorderColor = folderColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("iptv_channel_search")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Channels List
            val filteredChannels = selectedFolder.channels.filter { chan ->
                searchQuery.isBlank() || chan.name.contains(searchQuery, ignoreCase = true)
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredChannels, key = { it.id }) { channel ->
                    IptvChannelCard(
                        channel = channel,
                        accentColor = folderColor,
                        onPlay = { onPlayChannel(channel.streamUrl) }
                    )
                }
            }
        }
    }

    // DIALOG: EDIT PLAYLIST FOLDER (Change Name, Color, Icon)
    if (showEditFolderDialog != null) {
        val target = showEditFolderDialog!!
        var editedName by remember { mutableStateOf(target.name) }
        var editedColorHex by remember { mutableStateOf(target.colorHex) }
        var editedIconName by remember { mutableStateOf(target.iconName) }

        AlertDialog(
            onDismissRequest = { showEditFolderDialog = null },
            containerColor = SaCardBgElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Customize Playlist Folder",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Folder Name",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = parseHexColor(editedColorHex),
                            unfocusedBorderColor = Color(0xFF3F3F46)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Folder Accent Color",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableFolderColors) { hex ->
                            val color = parseHexColor(hex)
                            val isSelected = hex.equals(editedColorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { editedColorHex = hex }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Folder Icon",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableFolderIcons) { (iconKey, label) ->
                            val iconVec = getIconForName(iconKey)
                            val isSelected = iconKey == editedIconName
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) parseHexColor(editedColorHex).copy(alpha = 0.25f) else Color(0xFF27272A),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, parseHexColor(editedColorHex)) else null,
                                modifier = Modifier.clickable { editedIconName = iconKey }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = iconVec,
                                        contentDescription = label,
                                        tint = if (isSelected) parseHexColor(editedColorHex) else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        color = if (isSelected) parseHexColor(editedColorHex) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.updateFolder(
                            target.id,
                            editedName,
                            editedColorHex,
                            editedIconName
                        )
                        showEditFolderDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = parseHexColor(editedColorHex)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditFolderDialog = null }) {
                    Text("Cancel", color = Color(0xFFA1A1AA))
                }
            }
        )
    }

    // DIALOG: CREATE NEW M3U PLAYLIST FOLDER
    if (showAddPlaylistDialog) {
        var newFolderName by remember { mutableStateOf("") }
        var newStreamOrM3uUrl by remember { mutableStateOf("") }
        var chosenColorHex by remember { mutableStateOf("#4D6BFE") }
        var chosenIconName by remember { mutableStateOf("tv") }

        AlertDialog(
            onDismissRequest = { showAddPlaylistDialog = false },
            containerColor = SaCardBgElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "New M3U Playlist Folder",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter a name, choose an icon & color, and enter direct HLS (.m3u8) or M3U stream URL:",
                        fontSize = 12.sp,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = { Text("Playlist Name (e.g. European Sports)", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = parseHexColor(chosenColorHex),
                            unfocusedBorderColor = Color(0xFF3F3F46)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newStreamOrM3uUrl,
                        onValueChange = { newStreamOrM3uUrl = it },
                        placeholder = { Text("https://example.com/live.m3u8", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = parseHexColor(chosenColorHex),
                            unfocusedBorderColor = Color(0xFF3F3F46)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Pick Folder Color",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableFolderColors) { hex ->
                            val color = parseHexColor(hex)
                            val isSelected = hex == chosenColorHex
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { chosenColorHex = hex }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Pick Folder Icon",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableFolderIcons) { (iconKey, label) ->
                            val iconVec = getIconForName(iconKey)
                            val isSelected = iconKey == chosenIconName
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) parseHexColor(chosenColorHex).copy(alpha = 0.25f) else Color(0xFF27272A),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, parseHexColor(chosenColorHex)) else null,
                                modifier = Modifier.clickable { chosenIconName = iconKey }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = iconVec,
                                        contentDescription = label,
                                        tint = if (isSelected) parseHexColor(chosenColorHex) else Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = if (isSelected) parseHexColor(chosenColorHex) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val created = repository.createFolder(
                            name = newFolderName,
                            colorHex = chosenColorHex,
                            iconName = chosenIconName,
                            initialStreamUrl = newStreamOrM3uUrl
                        )
                        selectedFolderId = created.id
                        showAddPlaylistDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = parseHexColor(chosenColorHex)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create Folder", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlaylistDialog = false }) {
                    Text("Cancel", color = Color(0xFFA1A1AA))
                }
            }
        )
    }

    // DIALOG: ADD STREAM TO CURRENT FOLDER
    if (showAddChannelDialog && selectedFolder != null) {
        var chanName by remember { mutableStateOf("") }
        var chanUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddChannelDialog = false },
            containerColor = SaCardBgElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Add Channel to ${selectedFolder.name}",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = chanName,
                        onValueChange = { chanName = it },
                        placeholder = { Text("Channel Name", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = parseHexColor(selectedFolder.colorHex),
                            unfocusedBorderColor = Color(0xFF3F3F46)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = chanUrl,
                        onValueChange = { chanUrl = it },
                        placeholder = { Text("https://example.com/live.m3u8", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = parseHexColor(selectedFolder.colorHex),
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
                        if (chanUrl.isNotBlank()) {
                            repository.addCustomStreamToFolder(
                                selectedFolder.id,
                                chanName,
                                chanUrl
                            )
                        }
                        showAddChannelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = parseHexColor(selectedFolder.colorHex)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Stream", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChannelDialog = false }) {
                    Text("Cancel", color = Color(0xFFA1A1AA))
                }
            }
        )
    }
}

/**
 * 3-Column Playlist Folder Card matching the Video Folders UI from sa.jpg!
 */
@Composable
private fun IptvFolderGridItem(
    folder: IptvPlaylistFolder,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val folderColor = parseHexColor(folder.colorHex)
    val folderIcon = getIconForName(folder.iconName)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // Rounded dark square box containing custom colored folder icon + edit button + badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(SaCardBg),
            contentAlignment = Alignment.Center
        ) {
            // Folder icon with custom color and icon type
            Icon(
                imageVector = folderIcon,
                contentDescription = folder.name,
                tint = folderColor,
                modifier = Modifier.size(52.dp)
            )

            // Red badge on top right with channel count
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SaFolderBadgeRed,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
            ) {
                Text(
                    text = "${folder.channels.size}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Small edit icon button at top left to quickly customize name, color & icon
            Surface(
                shape = CircleShape,
                color = Color(0x66000000),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp, start = 8.dp)
                    .size(26.dp)
                    .clickable { onEdit() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Folder",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Folder Title
        Text(
            text = folder.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Channel count
        Text(
            text = "${folder.channels.size} Channels",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun IptvChannelCard(
    channel: IptvChannel,
    accentColor: Color,
    onPlay: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SaCardBgElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27272A)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("channel_card_${channel.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0x33EF4444)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "${channel.category} • ${channel.country}",
                        fontSize = 11.sp,
                        color = Color(0xFFA1A1AA)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Live Stream",
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
