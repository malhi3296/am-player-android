package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.SettingsRepository
import com.example.player.MusicPlayerManager
import com.example.ui.theme.AmAccentBlue
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SaCardBg
import com.example.ui.theme.SaCardBgElevated
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.parseHexColor
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    musicPlayerManager: MusicPlayerManager? = null,
    onToggleDarkMode: (Boolean) -> Unit,
    onRescanLibrary: () -> Unit,
    onClearCache: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val settings by settingsRepository.settings.collectAsState()

    // Dialog visibility states
    var showAccentColorDialog by remember { mutableStateOf(false) }
    var showSeekDurationDialog by remember { mutableStateOf(false) }
    var showSeekbarStyleDialog by remember { mutableStateOf(false) }
    var showMpvProfileDialog by remember { mutableStateOf(false) }
    var showSubtitleEncodingDialog by remember { mutableStateOf(false) }
    var showSubtitleFontSizeDialog by remember { mutableStateOf(false) }
    var showSubtitleColorDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var showDeviceInfoDialog by remember { mutableStateOf(false) }
    var showLibrariesDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    // Heart fluttering animation state (Item 27)
    val heartScale = remember { Animatable(1f) }
    val heartRotation = remember { Animatable(0f) }

    fun triggerHeartFlutter() {
        scope.launch {
            Toast.makeText(context, "❤️ AM Player by Arwinder Singh Malhi", Toast.LENGTH_SHORT).show()
            launch {
                heartScale.animateTo(
                    targetValue = 1f,
                    animationSpec = keyframes {
                        durationMillis = 650
                        1.4f at 120
                        0.9f at 240
                        1.35f at 360
                        0.95f at 480
                        1.0f at 650
                    }
                )
            }
            launch {
                heartRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 650
                        (-18f) at 100
                        18f at 220
                        (-12f) at 340
                        12f at 460
                        0f at 650
                    }
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP APP BAR: Back Arrow + "Settings"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigateBack?.invoke() },
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // "Made with love by Arwinder Singh Malhi" banner with Fluttering Animated Heart (Item 27)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, parseHexColor(settings.accentColorHex).copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { triggerHeartFlutter() }
                .testTag("made_with_love_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Love",
                    tint = DangerRed,
                    modifier = Modifier
                        .size(18.dp)
                        .scale(heartScale.value)
                        .rotate(heartRotation.value)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Made with love by Arwinder Singh Malhi",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Version 2.4.4",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SETTINGS LIST WITH DISTINCT HEADINGS (Item 5)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 64.dp)
        ) {
            // CATEGORY 1: APPEARANCE & THEME
            CategoryHeader(title = "APPEARANCE & THEME")

            SettingToggleItem(
                title = "Dark Theme",
                subtitle = if (settings.isDarkMode) "Pitch Black (#000000) AMOLED" else "Light Theme active",
                checked = settings.isDarkMode,
                onCheckedChange = {
                    settingsRepository.setDarkMode(it)
                    onToggleDarkMode(it)
                }
            )

            SettingClickableItem(
                title = "Accent Color",
                subtitle = "Active: ${settings.accentColorHex}",
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(settings.accentColorHex))
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                },
                onClick = { showAccentColorDialog = true }
            )

            // Logo Dark Mode Opacity (Item 19a)
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Logo Dark Mode Opacity",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Adjust top-left AM logo transparency in dark theme",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${(settings.logoDarkModeOpacity * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = parseHexColor(settings.accentColorHex)
                    )
                }
                Slider(
                    value = settings.logoDarkModeOpacity,
                    onValueChange = { settingsRepository.setLogoDarkModeOpacity(it) },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = parseHexColor(settings.accentColorHex),
                        activeTrackColor = parseHexColor(settings.accentColorHex)
                    )
                )
            }

            // CATEGORY 2: PLAYER & GESTURES
            CategoryHeader(title = "PLAYER & GESTURES")

            // Gesture controls toggle (Item 19b)
            SettingToggleItem(
                title = "Gesture Controls",
                subtitle = if (settings.gestureControlsEnabled) "Swipe brightness, volume & seek active" else "Gestures disabled",
                checked = settings.gestureControlsEnabled,
                onCheckedChange = { settingsRepository.setGestureControls(it) }
            )

            SettingClickableItem(
                title = "Double Tap to Seek",
                subtitle = "${settings.seekDurationSeconds} seconds forward & rewind",
                trailing = {
                    Text("${settings.seekDurationSeconds}s", fontWeight = FontWeight.Bold, color = parseHexColor(settings.accentColorHex), fontSize = 12.sp)
                },
                onClick = { showSeekDurationDialog = true }
            )

            // Seekbar styles (Item 18c)
            SettingClickableItem(
                title = "Seekbar Style",
                subtitle = "Active: ${settings.playerSeekbarStyle}",
                trailing = {
                    Text(settings.playerSeekbarStyle, fontWeight = FontWeight.Bold, color = parseHexColor(settings.accentColorHex), fontSize = 12.sp)
                },
                onClick = { showSeekbarStyleDialog = true }
            )

            SettingToggleItem(
                title = "Auto-Resume Videos",
                subtitle = "Remember and resume playback position",
                checked = settings.autoResumeEnabled,
                onCheckedChange = { settingsRepository.setAutoResume(it) }
            )

            // CATEGORY 3: DECODER & MPV PROFILES
            CategoryHeader(title = "DECODER & MPV PROFILES")

            SettingToggleItem(
                title = "Hardware Decoding (HW+)",
                subtitle = "GPU acceleration for 4K / 60fps videos",
                checked = settings.hwDecodingEnabled,
                onCheckedChange = { settingsRepository.setHwDecoding(it) }
            )

            SettingToggleItem(
                title = "Software Fallback (SW)",
                subtitle = "Auto-fallback if codec not natively supported",
                checked = settings.swFallbackEnabled,
                onCheckedChange = { settingsRepository.setSwFallback(it) }
            )

            // MPV Profile (Item 18e)
            SettingClickableItem(
                title = "MPV Video Profile",
                subtitle = settings.mpvProfile,
                trailing = {
                    Text("Select", fontWeight = FontWeight.Bold, color = parseHexColor(settings.accentColorHex), fontSize = 12.sp)
                },
                onClick = { showMpvProfileDialog = true }
            )

            // Color Mode
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Video Color Profile", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Normal", "Vivid", "Cinema", "HDR Boost").forEach { mode ->
                        val isSelected = settings.colorMode == mode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) parseHexColor(settings.accentColorHex) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { settingsRepository.setColorMode(mode) }
                        ) {
                            Text(
                                text = mode,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // CATEGORY 4: AUDIO & STREAMING
            CategoryHeader(title = "AUDIO & STREAMING")

            SettingToggleItem(
                title = "Audio Boost (+10 dB)",
                subtitle = "Boost maximum volume up to 200%",
                checked = settings.audioBoostEnabled,
                onCheckedChange = { settingsRepository.setAudioBoost(it) }
            )

            SettingToggleItem(
                title = "Background Music Playback",
                subtitle = "Keep music playing when phone is locked / screen off",
                checked = settings.playWhenLocked,
                onCheckedChange = { settingsRepository.setPlayWhenLocked(it) }
            )

            // Online Music Option (Item 15)
            SettingToggleItem(
                title = "Online Streaming Music",
                subtitle = if (settings.onlineMusicEnabled) "Enabled (streaming options active in Music tab)" else "Off (Local device audio only)",
                checked = settings.onlineMusicEnabled,
                onCheckedChange = { settingsRepository.setOnlineMusic(it) }
            )

            // CATEGORY 5: SUBTITLE EDITING
            CategoryHeader(title = "SUBTITLE EDITING (ITEM 18J)")

            SettingClickableItem(
                title = "Subtitle Font Size",
                subtitle = "${settings.subtitleFontSize} sp (${settings.subtitleSize})",
                trailing = {
                    Text("${settings.subtitleFontSize}sp", fontWeight = FontWeight.Bold, color = parseHexColor(settings.accentColorHex), fontSize = 12.sp)
                },
                onClick = { showSubtitleFontSizeDialog = true }
            )

            SettingClickableItem(
                title = "Subtitle Colors",
                subtitle = "Text: ${settings.subtitleTextColorHex} • Background Box: ${settings.subtitleBackgroundColorHex}",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(parseHexColor(settings.subtitleTextColorHex)).border(1.dp, Color.Gray, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(parseHexColor(settings.subtitleBackgroundColorHex)).border(1.dp, Color.Gray, RoundedCornerShape(3.dp)))
                    }
                },
                onClick = { showSubtitleColorDialog = true }
            )

            SettingClickableItem(
                title = "Subtitle Character Encoding",
                subtitle = "Active: ${settings.subtitleEncoding}",
                trailing = {
                    Text(settings.subtitleEncoding, fontWeight = FontWeight.Bold, color = parseHexColor(settings.accentColorHex), fontSize = 12.sp)
                },
                onClick = { showSubtitleEncodingDialog = true }
            )

            SettingToggleItem(
                title = "Online Subtitle Providers",
                subtitle = "Search OpenSubtitles, SubDL & Subscene automatically",
                checked = settings.subtitleProvidersEnabled,
                onCheckedChange = { settingsRepository.setSubtitleProviders(it) }
            )

            // CATEGORY 6: FILE BROWSER & CACHE
            CategoryHeader(title = "FILE BROWSER & CACHE (ITEM 18B, 18G)")

            SettingToggleItem(
                title = "Show Video Thumbnails",
                subtitle = "Generate real video frame previews in browser",
                checked = settings.showVideoThumbnails,
                onCheckedChange = { settingsRepository.setShowVideoThumbnails(it) }
            )

            SettingToggleItem(
                title = "Show Hidden Video Files",
                subtitle = "Display hidden videos and dotfiles in browser",
                checked = settings.fileBrowserShowHidden,
                onCheckedChange = { settingsRepository.setFileBrowserShowHidden(it) }
            )

            SettingToggleItem(
                title = "Auto-Scan Media on Startup",
                subtitle = "Scan device storage for new videos when opening app",
                checked = settings.autoScanStartup,
                onCheckedChange = { settingsRepository.setAutoScanStartup(it) }
            )

            SettingClickableItem(
                title = "Rescan Media Files",
                subtitle = "Force fresh indexing of videos and audio",
                trailing = {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = parseHexColor(settings.accentColorHex), modifier = Modifier.size(20.dp))
                },
                onClick = {
                    onRescanLibrary()
                    Toast.makeText(context, "Rescanning storage...", Toast.LENGTH_SHORT).show()
                }
            )

            SettingClickableItem(
                title = "Clear App Cache & Temp Files",
                subtitle = "Purge cached thumbnails and subtitle temp files",
                trailing = {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                },
                onClick = { showClearCacheConfirm = true }
            )

            // CATEGORY 7: BACKUP & SETTINGS EXPORT
            CategoryHeader(title = "EXPORT & IMPORT SETTINGS (ITEM 18F)")

            SettingClickableItem(
                title = "Export Settings",
                subtitle = "Export configuration to JSON file or clipboard",
                trailing = {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = parseHexColor(settings.accentColorHex), modifier = Modifier.size(18.dp))
                },
                onClick = { showExportDialog = true }
            )

            SettingClickableItem(
                title = "Import Settings",
                subtitle = "Restore configuration from JSON",
                trailing = {
                    Text("Import", fontWeight = FontWeight.Bold, color = parseHexColor(settings.accentColorHex), fontSize = 12.sp)
                },
                onClick = { showImportDialog = true }
            )

            // CATEGORY 8: ABOUT & DONATION
            CategoryHeader(title = "ABOUT, CREDITS & DONATION (ITEM 18H, 18I)")

            SettingClickableItem(
                title = "Device Information",
                subtitle = "${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}",
                trailing = {
                    Icon(Icons.Default.Info, contentDescription = null, tint = parseHexColor(settings.accentColorHex), modifier = Modifier.size(18.dp))
                },
                onClick = { showDeviceInfoDialog = true }
            )

            SettingClickableItem(
                title = "Open Source Libraries Used",
                subtitle = "ExoPlayer/Media3, Jetpack Compose, Room, Coil, OkHttp",
                trailing = {
                    Text("View", fontWeight = FontWeight.Bold, color = parseHexColor(settings.accentColorHex), fontSize = 12.sp)
                },
                onClick = { showLibrariesDialog = true }
            )

            SettingClickableItem(
                title = "Developer GitHub Profile",
                subtitle = "https://github.com/malhi3296",
                trailing = {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = parseHexColor(settings.accentColorHex), modifier = Modifier.size(18.dp))
                },
                onClick = {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/malhi3296"))
                        context.startActivity(browserIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "GitHub: https://github.com/malhi3296", Toast.LENGTH_LONG).show()
                    }
                }
            )

            // Donation Option with UPI & QR Code (Item 18i)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SuccessGreen.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .clickable { showDonationDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Donate",
                        tint = SuccessGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Donate & Support AM Player",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Text(
                            text = "UPI: 8288909927@airtel • PayPal: malhi3296@gmail.com",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // DIALOG: ACCENT COLOR PICKER (Item 6)
    if (showAccentColorDialog) {
        val colors = listOf(
            "#4D6BFE" to "AM Blue",
            "#E53935" to "Crimson Red",
            "#9C27B0" to "Purple Neon",
            "#00E676" to "Emerald Green",
            "#FF9100" to "Amber Boost",
            "#00B0FF" to "Cyan Sky",
            "#FF4081" to "Hot Pink",
            "#FFD600" to "Vibrant Yellow"
        )
        var customHex by remember { mutableStateOf(settings.accentColorHex) }

        AlertDialog(
            onDismissRequest = { showAccentColorDialog = false },
            title = { Text("Choose Accent Color", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select a color palette for AM Player buttons & indicators:", fontSize = 12.sp, color = Color(0xFFA1A1AA))
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colors.take(4).forEach { (hex, name) ->
                            val isSel = settings.accentColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .border(if (isSel) 3.dp else 1.dp, if (isSel) Color.White else Color.Transparent, CircleShape)
                                    .clickable {
                                        settingsRepository.setAccentColor(hex)
                                        customHex = hex
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colors.takeLast(4).forEach { (hex, name) ->
                            val isSel = settings.accentColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .border(if (isSel) 3.dp else 1.dp, if (isSel) Color.White else Color.Transparent, CircleShape)
                                    .clickable {
                                        settingsRepository.setAccentColor(hex)
                                        customHex = hex
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = customHex,
                        onValueChange = { customHex = it },
                        label = { Text("Custom Hex Color") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customHex.isNotBlank()) {
                            settingsRepository.setAccentColor(customHex.trim())
                        }
                        showAccentColorDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = parseHexColor(settings.accentColorHex))
                ) {
                    Text("Apply", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccentColorDialog = false }) { Text("Cancel") }
            }
        )
    }

    // DIALOG: SEEKBAR STYLE (Item 18c)
    if (showSeekbarStyleDialog) {
        val styles = listOf("Classic Dot", "Sleek Bar", "Waveform Accent", "Thick Pill")
        AlertDialog(
            onDismissRequest = { showSeekbarStyleDialog = false },
            title = { Text("Player Seekbar Style", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    styles.forEach { style ->
                        val isSel = settings.playerSeekbarStyle == style
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepository.setPlayerSeekbarStyle(style)
                                    showSeekbarStyleDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = {
                                    settingsRepository.setPlayerSeekbarStyle(style)
                                    showSeekbarStyleDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = parseHexColor(settings.accentColorHex))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(style, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSeekbarStyleDialog = false }) { Text("Done") }
            }
        )
    }

    // DIALOG: MPV PROFILE (Item 18e)
    if (showMpvProfileDialog) {
        val profiles = listOf(
            "High Quality (HW+ / Vulkan)",
            "Fast / Battery Saver",
            "Compatibility (SW Fallback)",
            "Direct Passthrough"
        )
        AlertDialog(
            onDismissRequest = { showMpvProfileDialog = false },
            title = { Text("MPV Engine Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    profiles.forEach { profile ->
                        val isSel = settings.mpvProfile == profile
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepository.setMpvProfile(profile)
                                    showMpvProfileDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = {
                                    settingsRepository.setMpvProfile(profile)
                                    showMpvProfileDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = parseHexColor(settings.accentColorHex))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(profile, fontSize = 13.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMpvProfileDialog = false }) { Text("Done") }
            }
        )
    }

    // DIALOG: SEEK DURATION
    if (showSeekDurationDialog) {
        AlertDialog(
            onDismissRequest = { showSeekDurationDialog = false },
            title = { Text("Double Tap to Seek", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(5, 10, 15, 30).forEach { sec ->
                        val isSel = settings.seekDurationSeconds == sec
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepository.setSeekDuration(sec)
                                    showSeekDurationDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = {
                                    settingsRepository.setSeekDuration(sec)
                                    showSeekDurationDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = parseHexColor(settings.accentColorHex))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$sec Seconds", fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSeekDurationDialog = false }) { Text("Done") }
            }
        )
    }

    // DIALOG: SUBTITLE FONT SIZE
    if (showSubtitleFontSizeDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleFontSizeDialog = false },
            title = { Text("Subtitle Font Size", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(14 to "Small (14sp)", 16 to "Normal (16sp)", 18 to "Medium (18sp)", 22 to "Large (22sp)", 26 to "Extra Large (26sp)").forEach { (spVal, label) ->
                        val isSel = settings.subtitleFontSize == spVal
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepository.setSubtitleFontSize(spVal)
                                    showSubtitleFontSizeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = {
                                    settingsRepository.setSubtitleFontSize(spVal)
                                    showSubtitleFontSizeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = parseHexColor(settings.accentColorHex))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleFontSizeDialog = false }) { Text("Done") }
            }
        )
    }

    // DIALOG: SUBTITLE COLOR
    if (showSubtitleColorDialog) {
        val textColors = listOf("#FFFFFF", "#FFFF00", "#00E5FF", "#00E676")
        AlertDialog(
            onDismissRequest = { showSubtitleColorDialog = false },
            title = { Text("Subtitle Colors", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select subtitle text color:", fontSize = 12.sp, color = Color(0xFFA1A1AA))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        textColors.forEach { hex ->
                            val isSel = settings.subtitleTextColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .border(if (isSel) 3.dp else 1.dp, if (isSel) parseHexColor(settings.accentColorHex) else Color.Gray, CircleShape)
                                    .clickable { settingsRepository.setSubtitleTextColor(hex) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleColorDialog = false }) { Text("Done") }
            }
        )
    }

    // DIALOG: SUBTITLE ENCODING
    if (showSubtitleEncodingDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleEncodingDialog = false },
            title = { Text("Subtitle Character Encoding", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("UTF-8", "ISO-8859-1", "Windows-1252", "ASCII").forEach { enc ->
                        val isSel = settings.subtitleEncoding == enc
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepository.setSubtitleEncoding(enc)
                                    showSubtitleEncodingDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = {
                                    settingsRepository.setSubtitleEncoding(enc)
                                    showSubtitleEncodingDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = parseHexColor(settings.accentColorHex))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(enc, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleEncodingDialog = false }) { Text("Done") }
            }
        )
    }

    // DIALOG: DONATION & QR CODE (Item 18i)
    if (showDonationDialog) {
        val upiId = "8288909927@airtel"

        AlertDialog(
            onDismissRequest = { showDonationDialog = false },
            containerColor = SaCardBgElevated,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = DangerRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Donate & Support Developer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Thank you for supporting AM Player! Scan the QR code with any UPI app (GPay, PhonePe, Paytm, Airtel):",
                        fontSize = 12.sp,
                        color = Color(0xFFA1A1AA),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // QR Code image
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier.size(200.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.upi_qr_code),
                            contentDescription = "UPI Payment QR Code",
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SaCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, parseHexColor(settings.accentColorHex).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AM Player UPI", upiId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "UPI ID copied: $upiId", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("UPI ID", fontSize = 10.sp, color = Color(0xFF9E9EA7))
                                Text(upiId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = parseHexColor(settings.accentColorHex), modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val paypalId = "malhi3296@gmail.com"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SaCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, parseHexColor(settings.accentColorHex).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AM Player PayPal", paypalId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "PayPal account copied: $paypalId", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("PayPal", fontSize = 10.sp, color = Color(0xFF9E9EA7))
                                Text(paypalId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = parseHexColor(settings.accentColorHex), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("AM Player UPI", upiId)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "UPI ID copied: $upiId", Toast.LENGTH_SHORT).show()
                        showDonationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Copy UPI ID", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDonationDialog = false }) {
                    Text("Close", color = Color(0xFFA1A1AA))
                }
            }
        )
    }

    // DIALOG: EXPORT SETTINGS (Item 18f)
    if (showExportDialog) {
        val jsonStr = remember { settingsRepository.exportSettingsJson() }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Settings JSON (ready to backup or transfer):", fontSize = 12.sp, color = Color(0xFFA1A1AA))
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    ) {
                        Text(
                            text = jsonStr,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            try {
                                val backupFile = java.io.File(context.cacheDir, "AMPlayer_Settings_Backup.json")
                                backupFile.writeText(jsonStr)
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    backupFile
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "AM Player Settings Backup")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Save or Share Settings File"))
                                showExportDialog = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = parseHexColor(settings.accentColorHex))
                    ) {
                        Text("Export to File")
                    }
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("AM Player Settings", jsonStr)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Settings copied to clipboard", Toast.LENGTH_SHORT).show()
                            showExportDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Copy", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Close") }
            }
        )
    }

    // DIALOG: IMPORT SETTINGS (Item 18f)
    if (showImportDialog) {
        var importInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste settings JSON to restore:", fontSize = 12.sp, color = Color(0xFFA1A1AA))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importInput,
                        onValueChange = { importInput = it },
                        placeholder = { Text("Paste JSON here...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (settingsRepository.importSettingsJson(importInput)) {
                            Toast.makeText(context, "Settings imported successfully!", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        } else {
                            Toast.makeText(context, "Invalid JSON settings", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = parseHexColor(settings.accentColorHex))
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }

    // DIALOG: CLEAR CACHE (Item 18g)
    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clear App Cache?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will purge all temporary thumbnail files, subtitle cache, and IPTV stream buffers. It will not delete your local video files.", fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleared = settingsRepository.clearAppCache()
                        onClearCache()
                        val kb = cleared / 1024
                        Toast.makeText(context, "Cleared $kb KB of cached data", Toast.LENGTH_SHORT).show()
                        showClearCacheConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Clear Now", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // DIALOG: DEVICE INFO (Item 18h)
    if (showDeviceInfoDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceInfoDialog = false },
            title = { Text("Device Information", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Manufacturer: ${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }}", fontSize = 13.sp)
                    Text("• Model: ${Build.MODEL}", fontSize = 13.sp)
                    Text("• Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", fontSize = 13.sp)
                    Text("• Hardware: ${Build.HARDWARE}", fontSize = 13.sp)
                    Text("• Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceInfoDialog = false }) { Text("Close") }
            }
        )
    }

    // DIALOG: LIBRARIES USED (Item 18h)
    if (showLibrariesDialog) {
        AlertDialog(
            onDismissRequest = { showLibrariesDialog = false },
            title = { Text("Open Source Libraries", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("• AndroidX Media3 (ExoPlayer & HLS): 1.4.1", fontSize = 12.sp)
                    Text("• Jetpack Compose Material 3: Modern UI engine", fontSize = 12.sp)
                    Text("• AndroidX Room Persistence: Local high-speed caching", fontSize = 12.sp)
                    Text("• Coil (Compose): Asynchronous thumbnail loading", fontSize = 12.sp)
                    Text("• Kotlin Coroutines & Flow: Async reactive state", fontSize = 12.sp)
                    Text("• OkHttp & Retrofit: Fast network & stream client", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showLibrariesDialog = false }) { Text("Close") }
            }
        )
    }
}

/**
 * Distinct Non-Clickable Category Header (Item 5: doesn't look same to setting)
 */
@Composable
private fun CategoryHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SettingClickableItem(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        trailing()
    }
}
