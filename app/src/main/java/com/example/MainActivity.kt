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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import com.example.ui.BottomNavTab
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
            var currentTab by rememberSaveable { mutableStateOf(BottomNavTab.HOME) }

            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                val granted = permissionsMap.values.any { it }
                libraryViewModel.setPermissionGranted(granted)
                if (granted) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        container.mediaRepository.rescanLocalMedia()
                        container.musicRepository.rescanAudio()
                    }
                }
            }

            LaunchedEffect(Unit) {
                val hasPerm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.READ_MEDIA_VIDEO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
                libraryViewModel.setPermissionGranted(hasPerm)
                if (!hasPerm) {
                    val perms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(
                            android.Manifest.permission.READ_MEDIA_VIDEO,
                            android.Manifest.permission.READ_MEDIA_AUDIO,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    permissionLauncher.launch(perms)
                }
            }

            LaunchedEffect(intentUri) {
                if (!intentUri.isNullOrEmpty()) {
                    currentPlayingUri = intentUri
                }
            }

            AMPlayerTheme(
                darkTheme = settings.isDarkMode,
                accentColor = accentColor
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MainHomeScreen(
                        libraryViewModel = libraryViewModel,
                        container = container,
                        isDarkMode = settings.isDarkMode,
                        currentTab = currentTab,
                        onTabChange = { currentTab = it },
                        onToggleDarkMode = { container.settingsRepository.setDarkMode(it) },
                        onPlayMedia = { selectedUri ->
                            currentPlayingUri = selectedUri
                        }
                    )

                    if (currentPlayingUri != null) {
                        PlayerScreen(
                            mediaUri = currentPlayingUri!!,
                            viewModel = playerViewModel,
                            onNavigateBack = {
                                currentPlayingUri = null
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.checkPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.toString()?.let { uriStr ->
            playerViewModel.loadMedia(uriStr)
        }
    }
}
