package com.example.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AMPlayerApplication
import com.example.data.model.MediaItemEntity
import com.example.data.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ViewMode {
    GRID, LIST
}

enum class SortOption(val displayName: String) {
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    DATE_DESC("Date (Newest First)"),
    DATE_ASC("Date (Oldest First)"),
    SIZE_DESC("File Size (Largest First)"),
    SIZE_ASC("File Size (Smallest First)"),
    VIDEOS_COUNT_DESC("Video Count (Most Videos)"),
    VIDEOS_COUNT_ASC("Video Count (Fewest Videos)")
}

data class FolderUiModel(
    val name: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val latestTimestamp: Long,
    val hasRecentVideo: Boolean,
    val badgeNumber: Int?
)

data class LibraryUiState(
    val selectedFolder: String? = null,
    val searchQuery: String = "",
    val isScanning: Boolean = false,
    val viewMode: ViewMode = ViewMode.GRID,
    val sortOption: SortOption = SortOption.NAME_ASC,
    val hasPermission: Boolean = true
)

class MediaLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AMPlayerApplication).container
    private val mediaRepo = container.mediaRepository
    val vaultRepository: VaultRepository = container.vaultRepository

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    // All raw media including hidden items (used by vault)
    val allRawMedia: StateFlow<List<MediaItemEntity>> = mediaRepo.allMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // All public media (hidden videos excluded)
    val allMedia: StateFlow<List<MediaItemEntity>> = combine(
        allRawMedia,
        vaultRepository.hiddenVideoIds
    ) { mediaList, hiddenIds ->
        mediaList.filter { !hiddenIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val recentMedia: StateFlow<List<MediaItemEntity>> = combine(
        mediaRepo.recentMedia,
        vaultRepository.hiddenVideoIds
    ) { recentList, hiddenIds ->
        recentList.filter { !hiddenIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Reactive Folders list with Search and Sorting applied!
    val folders: StateFlow<List<FolderUiModel>> = combine(
        allMedia,
        recentMedia,
        _uiState
    ) { mediaList, recentList, state ->
        val query = state.searchQuery.trim()
        val mostRecentVideo = recentList.firstOrNull()

        // 1. Group by folder name
        val grouped = mediaList.groupBy { it.folderName }

        val folderModels = grouped.map { (folderName, videos) ->
            val count = videos.size
            val totalSize = videos.sumOf { it.sizeBytes }
            val latestTime = videos.maxOfOrNull { it.lastPlayedTimestamp } ?: 0L
            val hasRecent = mostRecentVideo != null && mostRecentVideo.folderName == folderName
            val hasUnwatched = videos.any { it.playbackPositionMs == 0L }
            val badge = if (folderName.contains("WhatsApp", ignoreCase = true)) 17 else if (hasUnwatched) count else null

            FolderUiModel(
                name = folderName,
                videoCount = count,
                totalSizeBytes = totalSize,
                latestTimestamp = latestTime,
                hasRecentVideo = hasRecent,
                badgeNumber = badge
            )
        }

        // 2. Filter by search query (folder matches name OR contains video matching title)
        val filtered = if (query.isNotBlank()) {
            folderModels.filter { folder ->
                folder.name.contains(query, ignoreCase = true) ||
                    (grouped[folder.name]?.any { it.title.contains(query, ignoreCase = true) } == true)
            }
        } else {
            folderModels
        }

        // 3. Sort folders according to chosen SortOption
        when (state.sortOption) {
            SortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.latestTimestamp }
            SortOption.DATE_ASC -> filtered.sortedBy { it.latestTimestamp }
            SortOption.SIZE_DESC -> filtered.sortedByDescending { it.totalSizeBytes }
            SortOption.SIZE_ASC -> filtered.sortedBy { it.totalSizeBytes }
            SortOption.VIDEOS_COUNT_DESC -> filtered.sortedByDescending { it.videoCount }
            SortOption.VIDEOS_COUNT_ASC -> filtered.sortedBy { it.videoCount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Filtered videos when inside a folder or searching
    val filteredMedia: StateFlow<List<MediaItemEntity>> = combine(
        allMedia,
        _uiState
    ) { mediaList, state ->
        var list = mediaList

        // Filter by folder if inside a folder
        if (state.selectedFolder != null) {
            list = list.filter { it.folderName == state.selectedFolder }
        }

        // Search query filter
        if (state.searchQuery.isNotBlank()) {
            list = list.filter { it.title.contains(state.searchQuery, ignoreCase = true) }
        }

        // Sorting
        when (state.sortOption) {
            SortOption.NAME_ASC -> list.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> list.sortedByDescending { it.title.lowercase() }
            SortOption.DATE_DESC -> list.sortedByDescending { it.lastPlayedTimestamp }
            SortOption.DATE_ASC -> list.sortedBy { it.lastPlayedTimestamp }
            SortOption.SIZE_DESC -> list.sortedByDescending { it.sizeBytes }
            SortOption.SIZE_ASC -> list.sortedBy { it.sizeBytes }
            SortOption.VIDEOS_COUNT_DESC -> list.sortedByDescending { it.durationMs }
            SortOption.VIDEOS_COUNT_ASC -> list.sortedBy { it.durationMs }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        rescan()
    }

    fun rescan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            mediaRepo.rescanLocalMedia()
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    fun setSelectedFolder(folder: String?) {
        _uiState.update { it.copy(selectedFolder = folder) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(viewMode = if (it.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID)
        }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) rescan()
    }

    fun toggleFavorite(item: MediaItemEntity) {
        viewModelScope.launch {
            mediaRepo.toggleFavorite(item.id, !item.isFavorite)
        }
    }

    fun importMediaUri(uri: Uri, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val item = mediaRepo.resolveMediaFromUri(uri)
            onReady(item.id)
        }
    }
}
