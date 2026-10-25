package com.example.ui.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.MediaItemEntity
import com.example.ui.player.components.formatTime
import com.example.ui.theme.AmAccentBlue
import com.example.ui.theme.SaCardBg
import com.example.ui.theme.SaDarkBg
import com.example.ui.theme.SaFabPink
import com.example.ui.theme.SaFolderBadgeRed
import com.example.ui.theme.SaFolderBlue
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun MediaLibraryScreen(
    viewModel: MediaLibraryViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onPlayMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val allMedia by viewModel.allMedia.collectAsState()
    val recentMedia by viewModel.recentMedia.collectAsState()
    val filteredMedia by viewModel.filteredMedia.collectAsState()

    var showSearchField by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showRecentlyTab by remember { mutableStateOf(false) }

    // Media permissions launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val granted = permissionsMap.values.any { it }
        viewModel.setPermissionGranted(granted)
    }

    LaunchedEffect(Unit) {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(perms)
    }

    // Handle back button when navigating inside a folder or recently tab
    BackHandler(enabled = uiState.selectedFolder != null || showRecentlyTab) {
        if (uiState.selectedFolder != null) {
            viewModel.setSelectedFolder(null)
        } else {
            showRecentlyTab = false
        }
    }

    // Determine the last played video to resume from floating button
    val lastVideo = recentMedia.firstOrNull() ?: allMedia.firstOrNull()
    val activeFolder = uiState.selectedFolder ?: (if (showRecentlyTab) "Recently Played" else lastVideo?.folderName ?: "All Videos")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR: ONLY logo at top left (white with 50% opacity in dark mode) + action icons on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ONLY logo at top left (white with 50% opacity in dark mode)
                Image(
                    painter = painterResource(id = R.drawable.am_player_logo),
                    contentDescription = "AM Player Logo",
                    colorFilter = if (isDarkMode) ColorFilter.tint(Color.White) else null,
                    modifier = Modifier
                        .height(34.dp)
                        .then(if (isDarkMode) Modifier.alpha(0.5f) else Modifier)
                        .clickable {
                            viewModel.setSelectedFolder(null)
                            showRecentlyTab = false
                        }
                        .testTag("app_logo")
                )

                // Actions: Search, View Mode, Sort Options, Dark/Light Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showSearchField = !showSearchField }) {
                        Icon(
                            imageVector = if (showSearchField) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (showSearchField) AmAccentBlue else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (uiState.viewMode == ViewMode.GRID) Icons.Default.Splitscreen else Icons.Default.GridView,
                            contentDescription = "Toggle View Mode",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(onClick = { showSortDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "View and Sort Options",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(onClick = { onToggleDarkMode(!isDarkMode) }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark/Light Mode",
                            tint = if (isDarkMode) Color(0xFFFFD54F) else AmAccentBlue
                        )
                    }
                }
            }

            // Search input field
            AnimatedVisibility(visible = showSearchField) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Filter videos or folders...", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmAccentBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_input")
                    )
                }
            }

            // Permission Request Banner (if media permission not granted)
            if (!uiState.hasPermission) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AmAccentBlue.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmAccentBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = AmAccentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Media Access Required",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Allow AM Player to access videos and audio on device.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
                                } else {
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                                permissionLauncher.launch(perms)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmAccentBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Folder Header if navigated inside a folder
            if (uiState.selectedFolder != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.setSelectedFolder(null) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Folders",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = uiState.selectedFolder ?: "",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${filteredMedia.size} Videos in folder",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Quick Toggle Row: Folders vs Recently Button (active color = AmAccentBlue #4D6BFE)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!showRecentlyTab) (if (isDarkMode) SaCardBg else MaterialTheme.colorScheme.surfaceVariant) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (!showRecentlyTab) AmAccentBlue else MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showRecentlyTab = false }
                            .testTag("all_folders_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (!showRecentlyTab) AmAccentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Folders",
                                fontSize = 12.sp,
                                fontWeight = if (!showRecentlyTab) FontWeight.Bold else FontWeight.Medium,
                                color = if (!showRecentlyTab) AmAccentBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Recently Button with AmAccentBlue (#4D6BFE)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (showRecentlyTab) AmAccentBlue else (if (isDarkMode) SaCardBg else MaterialTheme.colorScheme.surfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmAccentBlue),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showRecentlyTab = true }
                            .testTag("recently_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = if (showRecentlyTab) Color.White else AmAccentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recently (${recentMedia.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showRecentlyTab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Content Area: Recently Videos OR Folders Grid matching sa.jpg OR Videos List
            if (uiState.selectedFolder == null && showRecentlyTab) {
                // Recently Played Videos List
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("recently_videos_list")
                ) {
                    items(recentMedia, key = { it.id }) { item ->
                        SaVideoListItem(
                            item = item,
                            onClick = { onPlayMedia(item.id) }
                        )
                    }
                }
            } else if (uiState.selectedFolder == null && uiState.viewMode == ViewMode.GRID) {
                // 3-Column Folder Grid matching sa.jpg exactly!
                val folderGroups = allMedia.groupBy { it.folderName }
                val folderNames = folderGroups.keys.toList()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("sa_folder_grid")
                ) {
                    items(folderNames) { folder ->
                        val count = folderGroups[folder]?.size ?: 0
                        val hasUnwatched = folderGroups[folder]?.any { it.playbackPositionMs == 0L } == true
                        val badgeCount = if (folder.contains("WhatsApp", ignoreCase = true)) 17 else if (hasUnwatched) count else null

                        SaFolderGridItem(
                            folderName = folder,
                            videoCount = count,
                            badgeNumber = badgeCount,
                            onClick = { viewModel.setSelectedFolder(folder) }
                        )
                    }
                }
            } else {
                // Video list view
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredMedia, key = { it.id }) { item ->
                        SaVideoListItem(
                            item = item,
                            onClick = { onPlayMedia(item.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Button: Play last video from last location + shows active folder
        if (lastVideo != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Small indicator showing active folder
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmAccentBlue.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AmAccentBlue)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active: $activeFolder",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Squircle FAB with play triangle matching sa.jpg
                FloatingActionButton(
                    onClick = { onPlayMedia(lastVideo.id) },
                    containerColor = SaFabPink, // Matching soft pinkish-mauve squircle from sa.jpg
                    contentColor = Color.White,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .size(62.dp)
                        .testTag("resume_last_video_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume Last Video",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        // Sort Options Dialog
        if (showSortDialog) {
            AlertDialog(
                onDismissRequest = { showSortDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sort, contentDescription = null, tint = AmAccentBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sort Options", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SortOption.values().forEach { option ->
                            val isSelected = option == uiState.sortOption
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.setSortOption(option)
                                        showSortDialog = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = AmAccentBlue)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = option.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AmAccentBlue else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSortDialog = false }) {
                        Text("Done", color = AmAccentBlue, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

/**
 * 3-Column Folder Card matching sa.jpg screenshot
 */
@Composable
private fun SaFolderGridItem(
    folderName: String,
    videoCount: Int,
    badgeNumber: Int?,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // Rounded dark square box containing the folder icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(SaCardBg),
            contentAlignment = Alignment.Center
        ) {
            // Folder icon in soft sky blue (matching sa.jpg)
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = folderName,
                tint = SaFolderBlue,
                modifier = Modifier.size(54.dp)
            )

            // Red badge on top right with number
            if (badgeNumber != null && badgeNumber > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SaFolderBadgeRed,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                ) {
                    Text(
                        text = "$badgeNumber",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Folder Title
        Text(
            text = folderName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Video Count
        Text(
            text = "$videoCount Videos",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SaVideoListItem(
    item: MediaItemEntity,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 86.dp, height = 58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E1E26)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = SaFolderBlue.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )

                if (item.durationMs > 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = formatTime(item.durationMs),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                if (item.durationMs > 0 && item.playbackPositionMs > 0) {
                    val progress = (item.playbackPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        color = AmAccentBlue,
                        trackColor = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.resolution.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AmAccentBlue.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = if (item.resolution.contains("3840")) "4K" else if (item.resolution.contains("1920")) "1080p" else item.resolution,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmAccentBlue,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = item.folderName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = AmAccentBlue.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = AmAccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
