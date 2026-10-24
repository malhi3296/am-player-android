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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.library.MediaLibraryScreen
import com.example.ui.library.MediaLibraryViewModel
import com.example.ui.player.PlayerScreen
import com.example.ui.player.PlayerViewModel
import com.example.ui.theme.AMPlayerTheme

class MainActivity : ComponentActivity() {

    private val libraryViewModel: MediaLibraryViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val intentUri = intent?.data?.toString()

        setContent {
            AMPlayerTheme(darkTheme = true) {
                var currentPlayingUri by remember { mutableStateOf(intentUri) }

                LaunchedEffect(intentUri) {
                    if (!intentUri.isNullOrEmpty()) {
                        currentPlayingUri = intentUri
                    }
                }

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
                        MediaLibraryScreen(
                            viewModel = libraryViewModel,
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
