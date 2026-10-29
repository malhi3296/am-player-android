package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppContainer
import com.example.ui.iptv.IptvScreen
import com.example.ui.library.MediaLibraryScreen
import com.example.ui.library.MediaLibraryViewModel
import com.example.ui.music.MusicScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AmAccentBlue
import com.example.ui.theme.SaCardBg
import com.example.ui.theme.SaDarkBg

enum class BottomNavTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    MUSIC("Music", Icons.Default.Audiotrack),
    IPTV("IPTV", Icons.Default.LiveTv),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun MainHomeScreen(
    libraryViewModel: MediaLibraryViewModel,
    container: AppContainer,
    isDarkMode: Boolean,
    currentTab: BottomNavTab = BottomNavTab.HOME,
    onTabChange: (BottomNavTab) -> Unit = {},
    onToggleDarkMode: (Boolean) -> Unit,
    onPlayMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val musicTracks by container.musicRepository.tracks.collectAsState()
    val iptvFolders by container.iptvRepository.folders.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = if (isDarkMode) SaDarkBg else MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                BottomNavTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onTabChange(tab) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentTab,
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    BottomNavTab.HOME -> {
                        val settings by container.settingsRepository.settings.collectAsState()
                        MediaLibraryScreen(
                            viewModel = libraryViewModel,
                            isDarkMode = isDarkMode,
                            logoDarkModeOpacity = settings.logoDarkModeOpacity,
                            onToggleDarkMode = onToggleDarkMode,
                            onPlayMedia = onPlayMedia,
                            showVideoThumbnails = settings.showVideoThumbnails,
                            dynamicAppIconWithAccent = settings.dynamicAppIconWithAccent,
                            accentColorHex = settings.accentColorHex
                        )
                    }
                    BottomNavTab.MUSIC -> {
                        MusicScreen(
                            tracks = musicTracks,
                            playerManager = container.musicPlayerManager,
                            musicRepository = container.musicRepository,
                            settingsRepository = container.settingsRepository,
                            radioStations = container.iptvRepository.getRadioStations(),
                            onRescan = {
                                // Trigger rescan of audio
                            }
                        )
                    }
                    BottomNavTab.IPTV -> {
                        IptvScreen(
                            folders = iptvFolders,
                            repository = container.iptvRepository,
                            onPlayChannel = onPlayMedia
                        )
                    }
                    BottomNavTab.SETTINGS -> {
                        SettingsScreen(
                            settingsRepository = container.settingsRepository,
                            musicPlayerManager = container.musicPlayerManager,
                            onToggleDarkMode = onToggleDarkMode,
                            onRescanLibrary = {
                                libraryViewModel.rescan()
                            },
                            onClearCache = {
                                // Cache clean
                            },
                            onNavigateBack = {
                                onTabChange(BottomNavTab.HOME)
                            }
                        )
                    }
                }
            }
        }
        androidx.activity.compose.BackHandler(enabled = currentTab != BottomNavTab.HOME) {
            onTabChange(BottomNavTab.HOME)
        }
    }
}
