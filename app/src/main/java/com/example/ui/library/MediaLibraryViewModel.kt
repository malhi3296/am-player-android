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

enum class LibraryTab(val displayName: String) {
    ALL("All Videos"),
    FOLDERS("Folders"),
    FAVORITES("Favorites"),
    DEMO("Demo Showcase")
}

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.ALL,
    val searchQuery: String = "",
    val isScanning: Boolean = false,
    val selectedFolder: String? = null
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

        // Tab filter
        list = when (state.selectedTab) {
            LibraryTab.ALL -> list
            LibraryTab.FOLDERS -> {
                if (state.selectedFolder != null) {
                    list.filter { it.folderName == state.selectedFolder }
                } else {
                    list
                }
            }
            LibraryTab.FAVORITES -> list.filter { it.isFavorite }
            LibraryTab.DEMO -> list.filter { it.folderName == "AM Showcase Clips" }
        }

        // Search query filter
        if (state.searchQuery.isNotBlank()) {
            list = list.filter { it.title.contains(state.searchQuery, ignoreCase = true) }
        }

        list
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

    fun setTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab, selectedFolder = null) }
    }

    fun setSelectedFolder(folder: String?) {
        _uiState.update { it.copy(selectedFolder = folder) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
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
