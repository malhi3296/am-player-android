package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.ui.MainHomeScreen
import com.example.ui.library.MediaLibraryViewModel
import com.example.ui.player.PlayerScreen
import com.example.ui.player.PlayerViewModel
import com.example.ui.theme.AMPlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val libraryViewModel: MediaLibraryViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as AMPlayerApplication).container
        val intentUri = intent?.data?.toString()

        // Pre-scan music repository
        lifecycleScope.launch(Dispatchers.IO) {
            container.musicRepository.rescanAudio()
        }

        setContent {
            val settings by container.settingsRepository.settings.collectAsState()
            val accentColor = remember(settings.accentColorHex) {
                com.example.ui.theme.parseHexColor(settings.accentColorHex)
            }
            var currentPlayingUri by remember { mutableStateOf(intentUri) }

            LaunchedEffect(intentUri) {
                if (!intentUri.isNullOrEmpty()) {
                    currentPlayingUri = intentUri
                }
            }

            AMPlayerTheme(
                darkTheme = settings.isDarkMode,
                accentColor = accentColor
            ) {
                Crossfade(
                    targetState = currentPlayingUri,
                    label = "screen_transition",
                    modifier = Modifier.fillMaxSize()
                ) { playingUri ->
                    if (playingUri != null) {
                        PlayerScreen(
                            mediaUri = playingUri,
                            viewModel = playerViewModel,
                            onNavigateBack = {
                                currentPlayingUri = null
                            }
                        )
                    } else {
                        MainHomeScreen(
                            libraryViewModel = libraryViewModel,
                            container = container,
                            isDarkMode = settings.isDarkMode,
                            onToggleDarkMode = { container.settingsRepository.setDarkMode(it) },
                            onPlayMedia = { selectedUri ->
                                currentPlayingUri = selectedUri
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.toString()?.let { uriStr ->
            playerViewModel.loadMedia(uriStr)
        }
    }
}
