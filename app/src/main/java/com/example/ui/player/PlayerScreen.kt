package com.example.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.player.AspectRatioMode
import com.example.ui.player.components.AspectRatioDialog
import com.example.ui.player.components.AudioTrackDialog
import com.example.ui.player.components.GestureOverlay
import com.example.ui.player.components.PlaybackSpeedDialog
import com.example.ui.player.components.PlayerControlsOverlay
import com.example.ui.player.components.PlayerHud
import com.example.ui.player.components.SubtitleSearchBottomSheet
import java.io.File
import java.io.FileOutputStream

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    mediaUri: String,
    viewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsState()

    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }

    // Launcher for manual subtitle file picking (.srt, .ass, .vtt)
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempSubFile = File(context.cacheDir, "manual_${System.currentTimeMillis()}.srt")
                inputStream?.use { input ->
                    FileOutputStream(tempSubFile).use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.playerManager.attachSubtitle(tempSubFile)
            } catch (_: Exception) {}
        }
    }

    // Load media on start
    LaunchedEffect(mediaUri) {
        viewModel.loadMedia(mediaUri)
    }

    // Window Insets immersive mode: hide system bars when controls hide
    LaunchedEffect(uiState.showControls) {
        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (uiState.showControls) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Apply brightness to window
    LaunchedEffect(uiState.brightness) {
        activity?.window?.let { window ->
            val lp = window.attributes
            lp.screenBrightness = uiState.brightness
            window.attributes = lp
        }
    }

    // Clean up window brightness on exit
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let { window ->
                val lp = window.attributes
                lp.screenBrightness = -1f // Reset to system default
                window.attributes = lp
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            viewModel.saveResumePosition()
        }
    }

    BackHandler {
        if (uiState.isLocked) {
            viewModel.toggleLock()
        } else {
            viewModel.saveResumePosition()
            onNavigateBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen_root")
    ) {
        // Player View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    player = viewModel.exoPlayer
                }
            },
            update = { playerView ->
                playerView.player = viewModel.exoPlayer
                playerView.resizeMode = when (uiState.aspectRatioMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = uiState.zoomScale,
                    scaleY = uiState.zoomScale,
                    translationX = uiState.panOffsetX,
                    translationY = uiState.panOffsetY
                )
        )

        // Gesture Overlay
        GestureOverlay(
            isLocked = uiState.isLocked,
            onSingleTap = { viewModel.toggleControls() },
            onDoubleTapLeft = { viewModel.seekBy(-10000L) },
            onDoubleTapRight = { viewModel.seekBy(10000L) },
            onDoubleTapCenter = { viewModel.togglePlayPause() },
            onVerticalSwipe = { isRight, delta -> viewModel.onVerticalSwipe(isRight, delta) },
            onHorizontalSeekDrag = { delta -> viewModel.onHorizontalSeekDrag(delta) },
            onHorizontalSeekEnd = { viewModel.onHorizontalSeekEnd() },
            onPinchZoom = { zoom, px, py -> viewModel.onPinchZoom(zoom, px, py) },
            modifier = Modifier.fillMaxSize()
        )

        // Floating HUD for volume / brightness / seek
        PlayerHud(
            hudState = uiState.hudState,
            modifier = Modifier.align(Alignment.Center)
        )

        // Interactive Controls Overlay
        PlayerControlsOverlay(
            uiState = uiState,
            onBackClick = {
                viewModel.saveResumePosition()
                onNavigateBack()
            },
            onPlayPauseClick = { viewModel.togglePlayPause() },
            onRewindClick = { viewModel.seekBy(-10000L) },
            onForwardClick = { viewModel.seekBy(10000L) },
            onSeek = { targetMs -> viewModel.seekTo(targetMs) },
            onLockToggle = { viewModel.toggleLock() },
            onAspectRatioClick = { showAspectRatioDialog = true },
            onSpeedClick = { showSpeedDialog = true },
            onSubtitleClick = { showSubtitleSheet = true },
            onAudioTrackClick = { showAudioDialog = true },
            onPipClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val params = PictureInPictureParams.Builder().build()
                    activity?.enterPictureInPictureMode(params)
                }
            },
            onTimeDisplayClick = { viewModel.toggleRemainingTime() }
        )

        // Subtitle Search Sheet
        if (showSubtitleSheet) {
            SubtitleSearchBottomSheet(
                isSearching = uiState.isSearchingSubtitles,
                candidates = uiState.subtitleCandidates,
                activeSubtitleTitle = uiState.currentSubtitleTitle,
                subtitleDelayMs = uiState.subtitleDelayMs,
                initialSearchQuery = uiState.mediaItem?.title ?: "",
                onSearch = { query, lang -> viewModel.searchSubtitlesManual(query, lang) },
                onSelectCandidate = { candidate ->
                    viewModel.applySubtitleCandidate(candidate)
                    showSubtitleSheet = false
                },
                onDelayChange = { delayMs -> viewModel.setSubtitleDelay(delayMs) },
                onPickLocalFile = {
                    subtitlePickerLauncher.launch(arrayOf("text/plain", "application/x-subrip", "*/*"))
                },
                onDismiss = { showSubtitleSheet = false }
            )
        }

        // Dialogs
        if (showAspectRatioDialog) {
            AspectRatioDialog(
                currentMode = uiState.aspectRatioMode,
                onSelectMode = { viewModel.setAspectRatio(it) },
                onDismiss = { showAspectRatioDialog = false }
            )
        }

        if (showSpeedDialog) {
            PlaybackSpeedDialog(
                currentSpeed = uiState.playbackSpeed,
                onSelectSpeed = { viewModel.setPlaybackSpeed(it) },
                onDismiss = { showSpeedDialog = false }
            )
        }

        if (showAudioDialog) {
            AudioTrackDialog(
                tracks = viewModel.playerManager.getAudioTracks(),
                selectedIndex = 0,
                onSelectTrack = { viewModel.playerManager.selectAudioTrack(it) },
                onDismiss = { showAudioDialog = false }
            )
        }
    }
}
