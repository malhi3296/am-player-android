package com.example.ui.iptv

import android.widget.Toast
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IptvChannel
import com.example.data.model.IptvPlaylistFolder
import com.example.data.repository.IptvRepository
import com.example.data.repository.SuggestedPlaylist
import com.example.ui.theme.SaCardBg
import com.example.ui.theme.SaCardBgElevated
import android.content.Context
import androidx.compose.material.icons.filled.Delete

@Composable
fun IptvScreen(
    folders: List<IptvPlaylistFolder>,
    repository: IptvRepository,
    onPlayChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var showAddChannelDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }

    val context = androidx.compose.ui.platform.LocalContext.current
    var contextMenuFolder by remember { mutableStateOf<IptvPlaylistFolder?>(null) }
    var contextMenuChannel by remember { mutableStateOf<IptvChannel?>(null) }
    var renameTargetFolder by remember { mutableStateOf<IptvPlaylistFolder?>(null) }
    var renameTargetChannel by remember { mutableStateOf<IptvChannel?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var deleteFolderConfirm by remember { mutableStateOf<IptvPlaylistFolder?>(null) }
    var deleteChannelConfirm by remember { mutableStateOf<IptvChannel?>(null) }
    val favoriteIds by repository.favoriteIds.collectAsState()

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
            // PLAYLIST FOLDERS ROOT VIEW - Clean heading without subheadings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "IPTV",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
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

            // 3-Column Playlist Folders Grid (uniform folder icon color, no colourful rainbow)
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
                        onLongClick = { contextMenuFolder = folder }
                    )
                }
            }
        } else {
            // INSIDE PLAYLIST FOLDER VIEW
            val isRadio = selectedFolder.iconName.contains("radio", ignoreCase = true) ||
                    selectedFolder.name.contains("radio", ignoreCase = true)

            // Header with back button & clean title (no subheadings)
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
                    Icon(
                        imageVector = if (isRadio) Icons.Default.Radio else Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedFolder.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { showAddChannelDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Channel",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search ${selectedFolder.name}...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
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
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
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
                    val isFav = favoriteIds.contains(channel.id) || favoriteIds.contains(channel.streamUrl)
                    IptvChannelCard(
                        channel = channel,
                        isRadio = isRadio,
                        isFav = isFav,
                        onPlay = { onPlayChannel(channel.streamUrl) },
                        onToggleFav = { repository.toggleFavorite(channel.id) },
                        onLongClick = { contextMenuChannel = channel }
                    )
                }
            }
        }
    }

    // DIALOG: CREATE NEW M3U PLAYLIST FOLDER WITH INTERNET SEARCH & SUGGESTIONS
    if (showAddPlaylistDialog) {
        val coroutineScope = rememberCoroutineScope()
        var isScanningM3u by remember { mutableStateOf(false) }
        var newFolderName by remember { mutableStateOf("") }
        var newStreamOrM3uUrl by remember { mutableStateOf("") }
        var internetSearchQuery by remember { mutableStateOf("") }
        val suggestedPlaylists = remember { repository.getSuggestedOnlinePlaylists() }
        val filteredSuggestions = remember(internetSearchQuery) {
            if (internetSearchQuery.isBlank()) suggestedPlaylists else {
                suggestedPlaylists.filter {
                    it.title.contains(internetSearchQuery, ignoreCase = true) ||
                            it.description.contains(internetSearchQuery, ignoreCase = true)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddPlaylistDialog = false },
            containerColor = SaCardBgElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Add M3U Playlist",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter a playlist name and direct M3U URL, or choose from online playlists below:",
                        fontSize = 12.sp,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = { Text("Playlist Name", fontSize = 13.sp) },
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
                        value = newStreamOrM3uUrl,
                        onValueChange = { newStreamOrM3uUrl = it },
                        placeholder = { Text("https://example.com/playlist.m3u", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF3F3F46)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Browse Online Playlists (Tap to auto-fill):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = internetSearchQuery,
                        onValueChange = { internetSearchQuery = it },
                        placeholder = { Text("Search online playlists...", fontSize = 11.sp) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF2E2E36)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredSuggestions) { item ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SaCardBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E36)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        newFolderName = item.title
                                        newStreamOrM3uUrl = item.url
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (item.isRadio) Icons.Default.Radio else Icons.Default.LiveTv,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(item.description, fontSize = 10.sp, color = Color(0xFF9E9EA7), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newStreamOrM3uUrl.isBlank()) {
                            Toast.makeText(context, "Please enter an M3U playlist URL", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val iconName = if (newFolderName.contains("news", ignoreCase = true)) "newspaper"
                            else if (newFolderName.contains("sport", ignoreCase = true)) "sports"
                            else "tv"
                        val folderTitle = newFolderName.ifBlank { "IPTV Playlist" }
                        isScanningM3u = true
                        coroutineScope.launch {
                            val result = repository.importM3uPlaylist(
                                folderName = folderTitle,
                                colorHex = "#4D6BFE",
                                iconName = iconName,
                                playlistUrlOrContent = newStreamOrM3uUrl.trim()
                            )
                            isScanningM3u = false
                            result.onSuccess { count ->
                                Toast.makeText(context, "Scanned & added $count channels!", Toast.LENGTH_SHORT).show()
                                showAddPlaylistDialog = false
                                val newlyAdded = repository.folders.value.lastOrNull()
                                if (newlyAdded != null) {
                                    selectedFolderId = newlyAdded.id
                                }
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to scan playlist", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isScanningM3u,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isScanningM3u) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning Channels...", color = Color.White, fontSize = 12.sp)
                        }
                    } else {
                        Text("Add Playlist", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isScanningM3u) showAddPlaylistDialog = false }) {
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
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
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
                        if (chanUrl.isNotBlank()) {
                            repository.addCustomStreamToFolder(
                                selectedFolder.id,
                                chanName,
                                chanUrl
                            )
                        }
                        showAddChannelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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


    // Folder Context Menu Dialog
    contextMenuFolder?.let { folder ->
        val isFavFolder = folder.id == "folder_fav"
        AlertDialog(
            onDismissRequest = { contextMenuFolder = null },
            title = { Text(folder.name, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!isFavFolder) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                renameTargetFolder = folder
                                renameInput = folder.name
                                contextMenuFolder = null
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Rename Playlist Folder", color = Color.White, fontSize = 14.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                deleteFolderConfirm = folder
                                contextMenuFolder = null
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Delete Playlist Folder", color = Color(0xFFEF4444), fontSize = 14.sp)
                        }
                    }
                    Text("${folder.channels.size} channels in playlist", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { contextMenuFolder = null }) { Text("Close", color = Color.White) }
            }
        )
    }

    // Channel Context Menu Dialog
    contextMenuChannel?.let { chan ->
        val isFav = favoriteIds.contains(chan.id) || favoriteIds.contains(chan.streamUrl)
        AlertDialog(
            onDismissRequest = { contextMenuChannel = null },
            title = { Text(chan.name, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            repository.toggleFavorite(chan.id)
                            contextMenuChannel = null
                            val msg = if (isFav) "Removed from Favourites" else "Added to Favourites"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (isFav) "Remove from Favourites" else "Add to Favourites", color = Color.White, fontSize = 14.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val clip = ClipData.newPlainText("Stream URL", chan.streamUrl)
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(clip)
                            Toast.makeText(context, "Stream URL copied", Toast.LENGTH_SHORT).show()
                            contextMenuChannel = null
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Copy Stream URL", color = Color.White, fontSize = 14.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            renameTargetChannel = chan
                            renameInput = chan.name
                            contextMenuChannel = null
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Rename Channel", color = Color.White, fontSize = 14.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            deleteChannelConfirm = chan
                            contextMenuChannel = null
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Delete Channel", color = Color(0xFFEF4444), fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { contextMenuChannel = null }) { Text("Close", color = Color.White) }
            }
        )
    }

    // Rename Folder Dialog
    renameTargetFolder?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTargetFolder = null },
            title = { Text("Rename Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameInput.isNotBlank()) repository.renameFolder(target.id, renameInput)
                    renameTargetFolder = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetFolder = null }) { Text("Cancel") }
            }
        )
    }

    // Rename Channel Dialog
    renameTargetChannel?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTargetChannel = null },
            title = { Text("Rename Channel", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Channel Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameInput.isNotBlank()) {
                        selectedFolder?.let { f -> repository.renameChannel(f.id, target.id, renameInput) }
                    }
                    renameTargetChannel = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetChannel = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Folder Confirm
    deleteFolderConfirm?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteFolderConfirm = null },
            title = { Text("Delete Playlist?", fontWeight = FontWeight.Bold) },
            text = { Text("Delete '${target.name}' and all its channels?") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteFolder(target.id)
                        deleteFolderConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteFolderConfirm = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Channel Confirm
    deleteChannelConfirm?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteChannelConfirm = null },
            title = { Text("Delete Channel?", fontWeight = FontWeight.Bold) },
            text = { Text("Delete '${target.name}' from this playlist?") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedFolder?.let { f -> repository.deleteChannel(f.id, target.id) }
                        deleteChannelConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteChannelConfirm = null }) { Text("Cancel") }
            }
        )
    }

}

/**
 * 3-Column Playlist Folder Card matching Video Folders UI:
 * Uniform Accent color, transparent/clean background, channel count below (no red badge).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IptvFolderGridItem(
    folder: IptvPlaylistFolder,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val isFav = folder.id == "folder_fav"
    val isRadio = folder.iconName.contains("radio", ignoreCase = true) || folder.name.contains("radio", ignoreCase = true)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        // Rounded dark square box containing uniform folder icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(SaCardBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isFav -> Icons.Default.Star
                    isRadio -> Icons.Default.Radio
                    else -> Icons.Default.Folder
                },
                contentDescription = folder.name,
                tint = when {
                    isFav -> Color(0xFFFFD700)
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Folder Title
        Text(
            text = folder.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Channel count below title (Item 22: mentioning under folder is enough)
        Text(
            text = "${folder.channels.size} items",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IptvChannelCard(
    channel: IptvChannel,
    isRadio: Boolean,
    isFav: Boolean = false,
    onPlay: () -> Unit,
    onToggleFav: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SaCardBgElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27272A)),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onPlay, onLongClick = onLongClick)
            .testTag("channel_card_${channel.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRadio) Icons.Default.Radio else Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
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

            IconButton(
                onClick = onToggleFav,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFav) Color(0xFFFFD700) else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Live Stream",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
