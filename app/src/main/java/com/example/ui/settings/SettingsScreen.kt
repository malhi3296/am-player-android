package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SettingsRepository
import com.example.player.MusicPlayerManager
import com.example.ui.theme.AmAccentBlue
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SaCardBgElevated

private enum class SettingsCategory {
    APPEARANCE,
    MEDIA_LIBRARY,
    PLAYER,
    DECODER,
    AUDIO,
    SUBTITLE,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
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

    val settings by settingsRepository.settings.collectAsState()
    var activeSheet by remember { mutableStateOf<SettingsCategory?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Dialog states for options
    var showSeekDialog by remember { mutableStateOf(false) }
    var showEncodingDialog by remember { mutableStateOf(false) }
    var showSubtitleSizeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP APP BAR: Back Arrow + "Settings" matching reference b.jpg
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
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

        // "Made with love by Arwinder Singh Malhi" banner at top of settings
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, AmAccentBlue.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
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
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Made with love by Arwinder Singh Malhi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // MINIMAL SETTINGS CATEGORIES matching b.jpg exactly
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp)
        ) {
            // 1. Appearance
            SettingsListItem(
                icon = Icons.Default.Palette,
                title = "Appearance",
                subtitle = "Adjust the app to your liking",
                testTag = "setting_item_appearance",
                onClick = { activeSheet = SettingsCategory.APPEARANCE }
            )

            // 2. Media library
            SettingsListItem(
                icon = Icons.Default.Movie,
                title = "Media library",
                subtitle = "Scan and manage media library",
                testTag = "setting_item_media_library",
                onClick = { activeSheet = SettingsCategory.MEDIA_LIBRARY }
            )

            // 3. Player
            SettingsListItem(
                icon = Icons.Default.PlayCircle,
                title = "Player",
                subtitle = "Player appearance, playback controls",
                testTag = "setting_item_player",
                onClick = { activeSheet = SettingsCategory.PLAYER }
            )

            // 4. Decoder
            SettingsListItem(
                icon = Icons.Default.Memory,
                title = "Decoder",
                subtitle = "Decoder priority, playback decoder options",
                testTag = "setting_item_decoder",
                onClick = { activeSheet = SettingsCategory.DECODER }
            )

            // 5. Audio
            SettingsListItem(
                icon = Icons.Default.Audiotrack,
                title = "Audio",
                subtitle = "Audio playback options",
                testTag = "setting_item_audio",
                onClick = { activeSheet = SettingsCategory.AUDIO }
            )

            // 6. Subtitle
            SettingsListItem(
                icon = Icons.Default.Subtitles,
                title = "Subtitle",
                subtitle = "Subtitle appearance, playback options",
                testTag = "setting_item_subtitle",
                onClick = { activeSheet = SettingsCategory.SUBTITLE }
            )

            // 7. About
            SettingsListItem(
                icon = Icons.Default.Info,
                title = "About",
                subtitle = "Version, support, credits",
                testTag = "setting_item_about",
                onClick = { activeSheet = SettingsCategory.ABOUT }
            )
        }
    }

    // Modal Bottom Sheet with clean, organized controls for each category
    if (activeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = if (settings.isDarkMode) SaCardBgElevated else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 36.dp)
            ) {
                when (activeSheet) {
                    SettingsCategory.APPEARANCE -> {
                        SheetHeader(title = "Appearance", subtitle = "Adjust the app to your liking")
                        Spacer(modifier = Modifier.height(18.dp))

                        SettingToggleRow(
                            title = "Dark Theme (OLED Pitch Black)",
                            subtitle = if (settings.isDarkMode) "Pitch Black (#000000) active" else "Light Theme active",
                            checked = settings.isDarkMode,
                            onCheckedChange = {
                                settingsRepository.setDarkMode(it)
                                onToggleDarkMode(it)
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        SettingActionRow(
                            title = "Accent Color",
                            subtitle = "AM Electric Blue (#4D6BFE) active",
                            actionWidget = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(AmAccentBlue)
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        SettingActionRow(
                            title = "Logo Dark Mode Opacity",
                            subtitle = "50% opacity in dark theme",
                            actionWidget = {
                                Text("50%", fontWeight = FontWeight.Bold, color = AmAccentBlue, fontSize = 13.sp)
                            }
                        )
                    }

                    SettingsCategory.MEDIA_LIBRARY -> {
                        SheetHeader(title = "Media Library", subtitle = "Scan and manage media library")
                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                onRescanLibrary()
                                Toast.makeText(context, "Rescanning local videos...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmAccentBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_rescan_media")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rescan Media Files Now", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onClearCache()
                                Toast.makeText(context, "Media and subtitle cache cleared (0 KB cached)", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_clear_cache")
                        ) {
                            Icon(
                                Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Cache & Temp Files", color = MaterialTheme.colorScheme.onSurface)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        SettingToggleRow(
                            title = "Auto-Scan on Startup",
                            subtitle = "Check for new camera and downloaded videos automatically",
                            checked = settings.autoScanStartup,
                            onCheckedChange = { settingsRepository.setAutoScanStartup(it) }
                        )
                    }

                    SettingsCategory.PLAYER -> {
                        SheetHeader(title = "Player", subtitle = "Player appearance, playback controls")
                        Spacer(modifier = Modifier.height(18.dp))

                        SettingToggleRow(
                            title = "Auto-Resume Videos",
                            subtitle = "Resume video from last saved position",
                            checked = settings.autoResumeEnabled,
                            onCheckedChange = { settingsRepository.setAutoResume(it) }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        SettingActionRow(
                            title = "Double Tap to Seek",
                            subtitle = "${settings.seekDurationSeconds} seconds forward / rewind (Tap to change)",
                            actionWidget = {
                                Text(
                                    text = "${settings.seekDurationSeconds}s",
                                    fontWeight = FontWeight.Bold,
                                    color = AmAccentBlue,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = { showSeekDialog = true }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        SettingActionRow(
                            title = "Gesture Controls",
                            subtitle = "Swipe left for brightness, right for volume, horizontal to seek",
                            actionWidget = {
                                Text("Active", fontWeight = FontWeight.SemiBold, color = AmAccentBlue, fontSize = 12.sp)
                            }
                        )
                    }

                    SettingsCategory.DECODER -> {
                        SheetHeader(title = "Decoder", subtitle = "Decoder priority, playback decoder options")
                        Spacer(modifier = Modifier.height(18.dp))

                        SettingToggleRow(
                            title = "Hardware Acceleration (HW+)",
                            subtitle = "Prioritize GPU hardware decoder for 4K and 60fps videos",
                            checked = settings.hwDecodingEnabled,
                            onCheckedChange = { settingsRepository.setHwDecoding(it) }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        SettingToggleRow(
                            title = "Software Fallback (SW)",
                            subtitle = "Auto-fallback to software engine if codec not supported",
                            checked = settings.swFallbackEnabled,
                            onCheckedChange = { settingsRepository.setSwFallback(it) }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Video Color Profile",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Normal", "Vivid", "Cinema", "HDR Boost").forEach { mode ->
                                val selected = settings.colorMode == mode
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) AmAccentBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selected) AmAccentBlue else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { settingsRepository.setColorMode(mode) }
                                ) {
                                    Text(
                                        text = mode,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    SettingsCategory.AUDIO -> {
                        SheetHeader(title = "Audio", subtitle = "Audio playback options")
                        Spacer(modifier = Modifier.height(18.dp))

                        SettingToggleRow(
                            title = "Audio Boost (+10 dB)",
                            subtitle = "Boost volume up to 200% using native LoudnessEnhancer",
                            checked = settings.audioBoostEnabled,
                            onCheckedChange = { settingsRepository.setAudioBoost(it) }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        SettingToggleRow(
                            title = "Play Music When Phone is Locked",
                            subtitle = "Allow music player to stream in background when screen off",
                            checked = settings.playWhenLocked,
                            onCheckedChange = { settingsRepository.setPlayWhenLocked(it) }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Equalizer Preset",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Flat", "Bass Boost", "Vocals", "Rock").forEach { preset ->
                                val selected = settings.equalizerPreset.equals(preset, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) AmAccentBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            settingsRepository.setEqualizerPreset(preset)
                                            musicPlayerManager?.setEqualizerPreset(preset)
                                        }
                                ) {
                                    Text(
                                        text = preset,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    SettingsCategory.SUBTITLE -> {
                        SheetHeader(title = "Subtitle", subtitle = "Subtitle appearance, playback options")
                        Spacer(modifier = Modifier.height(18.dp))

                        SettingActionRow(
                            title = "Subtitle Encoding",
                            subtitle = "Current: ${settings.subtitleEncoding} (Tap to change)",
                            actionWidget = {
                                Text(settings.subtitleEncoding, fontWeight = FontWeight.Bold, color = AmAccentBlue, fontSize = 12.sp)
                            },
                            onClick = { showEncodingDialog = true }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        SettingToggleRow(
                            title = "Online Providers",
                            subtitle = "OpenSubtitles, SubDL, and Subscene auto-search",
                            checked = settings.subtitleProvidersEnabled,
                            onCheckedChange = { settingsRepository.setSubtitleProviders(it) }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        SettingActionRow(
                            title = "Text Styling & Size",
                            subtitle = "Size: ${settings.subtitleSize} • White text with semi-transparent backdrop",
                            actionWidget = {
                                Text(settings.subtitleSize, color = AmAccentBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            },
                            onClick = { showSubtitleSizeDialog = true }
                        )
                    }

                    SettingsCategory.ABOUT -> {
                        SheetHeader(title = "About", subtitle = "Version, support, credits")
                        Spacer(modifier = Modifier.height(18.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmAccentBlue.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = DangerRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Made with love by Arwinder Singh Malhi",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "AM Player Pro • Version 2.4.0 (Build 2026)",
                                    fontSize = 12.sp,
                                    color = AmAccentBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Advanced video, audio & IPTV player featuring HW+ hardware acceleration, 200% volume boost, multi-provider subtitles, custom m3u playlists, and secret vault security.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    null -> {}
                }
            }
        }
    }

    // Dialog for Double Tap to Seek
    if (showSeekDialog) {
        AlertDialog(
            onDismissRequest = { showSeekDialog = false },
            title = { Text("Double Tap to Seek", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(5, 10, 15, 30).forEach { sec ->
                        val selected = settings.seekDurationSeconds == sec
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepository.setSeekDuration(sec)
                                    showSeekDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    settingsRepository.setSeekDuration(sec)
                                    showSeekDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = AmAccentBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$sec Seconds", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSeekDialog = false }) { Text("Cancel", color = AmAccentBlue) }
            }
        )
    }

    // Dialog for Subtitle Encoding
    if (showEncodingDialog) {
        AlertDialog(
            onDismissRequest = { showEncodingDialog = false },
            title = { Text("Subtitle Encoding", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("UTF-8", "ISO-8859-1", "Windows-1252", "ASCII").forEach { enc ->
                        val selected = settings.subtitleEncoding == enc
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepository.setSubtitleEncoding(enc)
                                    showEncodingDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    settingsRepository.setSubtitleEncoding(enc)
                                    showEncodingDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = AmAccentBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(enc, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEncodingDialog = false }) { Text("Cancel", color = AmAccentBlue) }
            }
        )
    }

    // Dialog for Subtitle Size
    if (showSubtitleSizeDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleSizeDialog = false },
            title = { Text("Subtitle Size", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("Small", "Medium", "Large", "Extra Large").forEach { size ->
                        val selected = settings.subtitleSize == size
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepository.setSubtitleSize(size)
                                    showSubtitleSizeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    settingsRepository.setSubtitleSize(size)
                                    showSubtitleSizeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = AmAccentBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(size, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleSizeDialog = false }) { Text("Cancel", color = AmAccentBlue) }
            }
        )
    }
}

/**
 * Minimal list item row matching reference b.jpg:
 * Clean icon on left, title in primary typography, subtitle below
 */
@Composable
private fun SettingsListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Monochrome / muted icon matching b.jpg
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            modifier = Modifier.size(26.dp)
        )

        Spacer(modifier = Modifier.width(22.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Text(
        text = subtitle,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                checkedTrackColor = AmAccentBlue
            )
        )
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    actionWidget: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp),
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
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        actionWidget()
    }
}
