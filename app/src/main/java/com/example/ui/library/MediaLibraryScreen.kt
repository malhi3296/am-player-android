package com.example.ui.library

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.MediaItemEntity
import com.example.ui.theme.AmAccentBlue
import com.example.ui.theme.DangerRed
import com.example.ui.vault.SecretVaultDialog

@Composable
fun MediaLibraryScreen(
    viewModel: MediaLibraryViewModel,
    isDarkMode: Boolean,
    logoDarkModeOpacity: Float = 1.0f,
    onToggleDarkMode: (Boolean) -> Unit,
    onPlayMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val uiState by viewModel.uiState.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val filteredMedia by viewModel.filteredMedia.collectAsState()
    val lastVideo by viewModel.lastPlayedVideo.collectAsState()
    val allRawMedia by viewModel.allRawMedia.collectAsState()

    var showSearchField by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showSecretVaultDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 5-tap detection on top-left logo to open hidden locked video vault
    var logoTapCount by remember { mutableIntStateOf(0) }
    var lastLogoTapTime by remember { mutableLongStateOf(0L) }

    val onLogoClick: () -> Unit = {
        val now = System.currentTimeMillis()
        if (now - lastLogoTapTime > 2500L) {
            logoTapCount = 1
        } else {
            logoTapCount++
        }
        lastLogoTapTime = now

        if (logoTapCount in 2..4) {
            Toast.makeText(
                context,
                "Tap ${5 - logoTapCount} more times to open Secret Vault",
                Toast.LENGTH_SHORT
            ).show()
        } else if (logoTapCount >= 5) {
            logoTapCount = 0
            showSecretVaultDialog = true
        } else {
            if (uiState.selectedFolder != null) {
                viewModel.setSelectedFolder(null)
            }
        }
    }

    // Media permissions launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val granted = permissionsMap.values.any { it }
        viewModel.setPermissionGranted(granted)
    }

    BackHandler(enabled = uiState.selectedFolder != null || uiState.isSelectionMode) {
        if (uiState.isSelectionMode) {
            viewModel.toggleSelectionMode(false)
        } else {
            viewModel.setSelectedFolder(null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR
            if (uiState.isSelectionMode) {
                // Selection Mode Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.toggleSelectionMode(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${uiState.selectedVideoIds.size} Selected",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.selectAllVideos(filteredMedia.map { it.id }) }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        if (uiState.selectedVideoIds.isNotEmpty()) {
                            IconButton(onClick = { showDeleteConfirmDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed)
                            }
                        }
                    }
                }
            } else {
                // Standard Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (uiState.selectedFolder == null) {
                        // Root Library Header with logo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onLogoClick() }
                                .testTag("top_left_app_logo")
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.am_player_logo),
                                contentDescription = "AM Player Logo",
                                alpha = if (isDarkMode) logoDarkModeOpacity else 1.0f,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "AM Player",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        // Inside Folder Header (clean, no duplicate folder box, just back arrow and name)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.setSelectedFolder(null) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = uiState.selectedFolder ?: "",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Top Action Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Search
                        IconButton(
                            onClick = { showSearchField = !showSearchField },
                            modifier = Modifier.testTag("action_search_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (showSearchField) AmAccentBlue else MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Grid / List Toggle
                        IconButton(
                            onClick = { viewModel.toggleViewMode() },
                            modifier = Modifier.testTag("action_toggle_view_mode")
                        ) {
                            Icon(
                                imageVector = if (uiState.viewMode == ViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle Grid / List View",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Sort Options
                        IconButton(
                            onClick = { showSortDialog = true },
                            modifier = Modifier.testTag("action_sort_options")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Dark/Light Theme Toggle (Just toggle icon from sun to moon cleanly with white icon)
                        IconButton(
                            onClick = { onToggleDarkMode(!isDarkMode) },
                            modifier = Modifier.testTag("action_toggle_theme")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            // Search input field
            AnimatedVisibility(visible = showSearchField) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search folders or videos...", fontSize = 13.sp) },
                        singleLine = true,
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
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

            // Permission Request Banner
            if (!uiState.hasPermission) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AmAccentBlue.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmAccentBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
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
                                text = "Allow AM Player to access videos on device.",
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

            // MAIN CONTENT: Folders View OR Videos Inside Folder View
            if (uiState.selectedFolder == null) {
                // ROOT VIEW: Folders
                if (folders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) "No matching folders found" else "No video folders on device",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (uiState.viewMode == ViewMode.GRID) {
                    // Folder Grid: 3 columns portrait, 6 columns landscape
                    LazyVerticalGrid(
                        columns = if (isLandscape) GridCells.Fixed(6) else GridCells.Fixed(3),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 14.dp),
                        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("sa_folder_grid")
                    ) {
                        items(folders, key = { it.name }) { folder ->
                            SaFolderGridItem(
                                folder = folder,
                                isLandscape = isLandscape,
                                isDarkMode = isDarkMode,
                                onClick = { viewModel.setSelectedFolder(folder.name) }
                            )
                        }
                    }
                } else {
                    // Folder List View
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("sa_folder_list")
                    ) {
                        items(folders, key = { it.name }) { folder ->
                            SaFolderListItem(
                                folder = folder,
                                isDarkMode = isDarkMode,
                                onClick = { viewModel.setSelectedFolder(folder.name) }
                            )
                        }
                    }
                }
            } else {
                // INSIDE FOLDER: Videos (Respects Grid or List view mode!)
                if (filteredMedia.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No videos found in this folder",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (uiState.viewMode == ViewMode.GRID) {
                    // Videos Grid View
                    LazyVerticalGrid(
                        columns = if (isLandscape) GridCells.Fixed(4) else GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("folder_videos_grid")
                    ) {
                        items(filteredMedia, key = { it.id }) { item ->
                            val isSelected = item.id in uiState.selectedVideoIds
                            SaVideoGridItem(
                                item = item,
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = isSelected,
                                isDarkMode = isDarkMode,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleVideoSelection(item.id)
                                    } else {
                                        viewModel.recordVideoClick(item.id)
                                        onPlayMedia(item.id)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelectionMode(true)
                                    viewModel.toggleVideoSelection(item.id)
                                }
                            )
                        }
                    }
                } else {
                    // Videos List View
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("folder_videos_list")
                    ) {
                        items(filteredMedia, key = { it.id }) { item ->
                            val isSelected = item.id in uiState.selectedVideoIds
                            SaVideoListItem(
                                item = item,
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleVideoSelection(item.id)
                                    } else {
                                        viewModel.recordVideoClick(item.id)
                                        onPlayMedia(item.id)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelectionMode(true)
                                    viewModel.toggleVideoSelection(item.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        // REDESIGNED FLOATING ACTION BUTTON (Recently Played Video)
        // a) No text on top of button
        // b) Button circle is accent color, and play icon is pure white for both dark and light mode
        if (lastVideo != null && !uiState.isSelectionMode) {
            FloatingActionButton(
                onClick = {
                    viewModel.recordVideoClick(lastVideo!!.id)
                    onPlayMedia(lastVideo!!.id)
                },
                containerColor = AmAccentBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
                    .size(58.dp)
                    .testTag("resume_last_video_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume Video",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
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
                        Text("Sort By", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                        Text("Close", color = AmAccentBlue)
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("Delete Selected Videos?", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("Are you sure you want to delete ${uiState.selectedVideoIds.size} video(s)? This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSelectedVideos()
                            showDeleteConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Secret Vault Dialog (PIN Protected, with subfolders support)
        if (showSecretVaultDialog) {
            SecretVaultDialog(
                vaultRepository = viewModel.vaultRepository,
                allAvailableMedia = allRawMedia,
                onPlayMedia = { videoId ->
                    showSecretVaultDialog = false
                    viewModel.recordVideoClick(videoId)
                    onPlayMedia(videoId)
                },
                onDismiss = { showSecretVaultDialog = false }
            )
        }
    }
}

/**
 * Minimalist Folder Grid Item
 * - Background: Transparent (or White in light mode)
 * - Icon: Accent color, size 38dp portrait, 26dp landscape
 * - Red dot on top-right for recent video (no border or highlight)
 * - Video count under folder name (no red badge)
 */
@Composable
private fun SaFolderGridItem(
    folder: FolderUiModel,
    isLandscape: Boolean = false,
    isDarkMode: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        val iconSize = if (isLandscape) 26.dp else 38.dp
        val cornerRadius = if (isLandscape) 12.dp else 16.dp

        // Transparent / clean card box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(cornerRadius))
                .background(if (isDarkMode) Color.Transparent else Color.White),
            contentAlignment = Alignment.Center
        ) {
            // Folder icon in accent color
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = folder.name,
                tint = AmAccentBlue,
                modifier = Modifier.size(iconSize)
            )

            // Very small red dot on top right for recent video (Requirement 20)
            if (folder.hasRecentVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Folder Title
        Text(
            text = folder.name,
            fontSize = if (isLandscape) 11.sp else 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(1.dp))

        // Video Count (Text only, no red badge)
        Text(
            text = "${folder.videoCount} Videos",
            fontSize = if (isLandscape) 10.sp else 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Vertical Folder List Item
 */
@Composable
private fun SaFolderListItem(
    folder: FolderUiModel,
    isDarkMode: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = AmAccentBlue,
                    modifier = Modifier.size(28.dp)
                )

                if (folder.hasRecentVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folder.videoCount} Videos",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Video Item in Grid View: Clean thumbnail + Video Title ONLY (Requirement 36)
 */
@Composable
private fun SaVideoGridItem(
    item: MediaItemEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) AmAccentBlue else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E26)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = AmAccentBlue.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp)
                )

                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) AmAccentBlue else Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Video Name ONLY (Requirement 36)
            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Video Item in List View: Clean row with Thumbnail + Video Title ONLY (Requirement 36)
 */
@Composable
private fun SaVideoListItem(
    item: MediaItemEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) AmAccentBlue else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E26)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = AmAccentBlue.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Video Name ONLY (Requirement 36)
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) AmAccentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
