package com.example.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AMPlayerApplication
import com.example.data.model.MediaItemEntity
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
    NAME("Name (A to Z)"),
    DATE("Recently Modified"),
    SIZE("File Size"),
    VIDEOS_COUNT("Video Count")
}

data class LibraryUiState(
    val selectedFolder: String? = null,
    val searchQuery: String = "",
    val isScanning: Boolean = false,
    val viewMode: ViewMode = ViewMode.GRID,
    val sortOption: SortOption = SortOption.NAME,
    val hasPermission: Boolean = true
)

class MediaLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepo = (application as AMPlayerApplication).container.mediaRepository

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    val allMedia: StateFlow<List<MediaItemEntity>> = mediaRepo.allMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val recentMedia: StateFlow<List<MediaItemEntity>> = mediaRepo.recentMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

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
            SortOption.NAME -> list.sortedBy { it.title.lowercase() }
            SortOption.DATE -> list.sortedByDescending { it.lastPlayedTimestamp }
            SortOption.SIZE -> list.sortedByDescending { it.sizeBytes }
            SortOption.VIDEOS_COUNT -> list
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
