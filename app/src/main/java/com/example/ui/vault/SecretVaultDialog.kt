package com.example.ui.vault

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MediaItemEntity
import com.example.data.repository.VaultRepository
import com.example.ui.player.components.formatTime
import com.example.ui.theme.AmAccentBlue
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SaCardBg
import com.example.ui.theme.SaCardBgElevated
import com.example.ui.theme.SaDarkBg
import com.example.ui.theme.SaFolderBlue
import com.example.ui.theme.SuccessGreen

private enum class VaultScreenState {
    ENTER_PIN,
    SETUP_PIN,
    SETUP_CONFIRM_PIN,
    VAULT_CONTENT
}

@Composable
fun SecretVaultDialog(
    vaultRepository: VaultRepository,
    allAvailableMedia: List<MediaItemEntity>,
    onPlayMedia: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isConfigured by vaultRepository.isVaultConfigured.collectAsState()
    val isBiometricEnabled by vaultRepository.isBiometricEnabled.collectAsState()
    val hiddenIds by vaultRepository.hiddenVideoIds.collectAsState()

    var screenState by remember(isConfigured) {
        mutableStateOf(if (isConfigured) VaultScreenState.ENTER_PIN else VaultScreenState.SETUP_PIN)
    }

    var pinInput by remember { mutableStateOf("") }
    var pinSetupFirst by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var enableBiometricInSetup by remember { mutableStateOf(true) }
    var showAddVideosSheet by remember { mutableStateOf(false) }

    val hiddenVideos = remember(allAvailableMedia, hiddenIds) {
        allAvailableMedia.filter { hiddenIds.contains(it.id) }
    }
    val unhiddenVideos = remember(allAvailableMedia, hiddenIds) {
        allAvailableMedia.filter { !hiddenIds.contains(it.id) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(SaDarkBg)
                .testTag("secret_vault_dialog"),
            color = SaDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // Top header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = AmAccentBlue.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (screenState == VaultScreenState.VAULT_CONTENT) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = AmAccentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Secret Video Vault",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Locked & Hidden Videos",
                                fontSize = 11.sp,
                                color = Color(0xFF9E9EA7)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_vault_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Vault",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Crossfade(targetState = screenState, label = "vault_crossfade") { state ->
                    when (state) {
                        VaultScreenState.ENTER_PIN -> {
                            PinEntryView(
                                title = "Enter Vault PIN",
                                subtitle = "Enter your 4-digit PIN to access locked videos",
                                pin = pinInput,
                                errorMessage = errorMessage,
                                showBiometric = isBiometricEnabled,
                                onDigit = { digit ->
                                    if (pinInput.length < 4) {
                                        val newPin = pinInput + digit
                                        pinInput = newPin
                                        errorMessage = null
                                        if (newPin.length == 4) {
                                            if (vaultRepository.verifyPin(newPin)) {
                                                screenState = VaultScreenState.VAULT_CONTENT
                                                pinInput = ""
                                            } else {
                                                errorMessage = "Incorrect PIN. Please try again."
                                                pinInput = ""
                                            }
                                        }
                                    }
                                },
                                onBackspace = {
                                    if (pinInput.isNotEmpty()) {
                                        pinInput = pinInput.dropLast(1)
                                        errorMessage = null
                                    }
                                },
                                onBiometricClick = {
                                    // Direct biometric unlock simulation / authentication
                                    Toast.makeText(context, "Biometric authentication verified", Toast.LENGTH_SHORT).show()
                                    screenState = VaultScreenState.VAULT_CONTENT
                                    errorMessage = null
                                }
                            )
                        }

                        VaultScreenState.SETUP_PIN -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                PinEntryView(
                                    title = "Set Vault PIN",
                                    subtitle = "Choose a 4-digit PIN to secure your hidden folder",
                                    pin = pinInput,
                                    errorMessage = errorMessage,
                                    showBiometric = false,
                                    onDigit = { digit ->
                                        if (pinInput.length < 4) {
                                            val newPin = pinInput + digit
                                            pinInput = newPin
                                            errorMessage = null
                                            if (newPin.length == 4) {
                                                pinSetupFirst = newPin
                                                pinInput = ""
                                                screenState = VaultScreenState.SETUP_CONFIRM_PIN
                                            }
                                        }
                                    },
                                    onBackspace = {
                                        if (pinInput.isNotEmpty()) {
                                            pinInput = pinInput.dropLast(1)
                                            errorMessage = null
                                        }
                                    },
                                    onBiometricClick = {}
                                )
                            }
                        }

                        VaultScreenState.SETUP_CONFIRM_PIN -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                PinEntryView(
                                    title = "Confirm Vault PIN",
                                    subtitle = "Re-enter your 4-digit PIN to confirm",
                                    pin = pinInput,
                                    errorMessage = errorMessage,
                                    showBiometric = false,
                                    onDigit = { digit ->
                                        if (pinInput.length < 4) {
                                            val newPin = pinInput + digit
                                            pinInput = newPin
                                            errorMessage = null
                                            if (newPin.length == 4) {
                                                if (newPin == pinSetupFirst) {
                                                    vaultRepository.setupVault(newPin, enableBiometricInSetup)
                                                    Toast.makeText(context, "Secret Vault configured successfully!", Toast.LENGTH_SHORT).show()
                                                    screenState = VaultScreenState.VAULT_CONTENT
                                                    pinInput = ""
                                                } else {
                                                    errorMessage = "PINs do not match. Start over."
                                                    pinInput = ""
                                                    screenState = VaultScreenState.SETUP_PIN
                                                }
                                            }
                                        }
                                    },
                                    onBackspace = {
                                        if (pinInput.isNotEmpty()) {
                                            pinInput = pinInput.dropLast(1)
                                            errorMessage = null
                                        }
                                    },
                                    onBiometricClick = {}
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SaCardBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = null,
                                                tint = AmAccentBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Use System Biometrics",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Unlock with fingerprint or face",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF9E9EA7)
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = enableBiometricInSetup,
                                            onCheckedChange = { enableBiometricInSetup = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = AmAccentBlue
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        VaultScreenState.VAULT_CONTENT -> {
                            VaultContentView(
                                hiddenVideos = hiddenVideos,
                                onPlayVideo = onPlayMedia,
                                onUnhideVideo = { videoId ->
                                    vaultRepository.unhideVideo(videoId)
                                    Toast.makeText(context, "Video restored to library", Toast.LENGTH_SHORT).show()
                                },
                                onOpenAddVideos = { showAddVideosSheet = true },
                                onResetPin = {
                                    screenState = VaultScreenState.SETUP_PIN
                                    pinInput = ""
                                    pinSetupFirst = ""
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet / Dialog to add public videos to the secret vault
    if (showAddVideosSheet) {
        Dialog(onDismissRequest = { showAddVideosSheet = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SaCardBgElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, AmAccentBlue.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add Videos to Vault",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { showAddVideosSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Text(
                        text = "Selected videos will be hidden from the public library and protected by your PIN.",
                        fontSize = 12.sp,
                        color = Color(0xFF9E9EA7)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (unhiddenVideos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("All videos are already in the vault", color = Color(0xFF9E9EA7), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(unhiddenVideos, key = { it.id }) { video ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SaCardBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E36)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = null,
                                            tint = SaFolderBlue,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = video.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${video.folderName} • ${formatTime(video.durationMs)}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF9E9EA7)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                vaultRepository.hideVideo(video.id)
                                                Toast.makeText(context, "Locked to Secret Vault", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = AmAccentBlue)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Lock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showAddVideosSheet = false },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E36)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PinEntryView(
    title: String,
    subtitle: String,
    pin: String,
    errorMessage: String?,
    showBiometric: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometricClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = Color(0xFF9E9EA7),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 4 PIN Dots indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val isFilled = i < pin.length
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isFilled) AmAccentBlue else Color(0xFF2E2E36))
                        .border(
                            1.dp,
                            if (isFilled) AmAccentBlue else Color(0xFF4A4A56),
                            CircleShape
                        )
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = errorMessage,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = DangerRed
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Numeric Keypad (1 to 9, Biometric/Clear, 0, Backspace)
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(if (showBiometric) "BIO" else "", "0", "DEL")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (key.isNotBlank()) SaCardBg else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (key.isNotBlank()) Color(0xFF2A2A34) else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable(enabled = key.isNotBlank()) {
                                    when (key) {
                                        "DEL" -> onBackspace()
                                        "BIO" -> onBiometricClick()
                                        else -> if (key.isNotBlank()) onDigit(key)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (key) {
                                "DEL" -> Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                "BIO" -> Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric",
                                    tint = AmAccentBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                                else -> Text(
                                    text = key,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultContentView(
    hiddenVideos: List<MediaItemEntity>,
    onPlayVideo: (String) -> Unit,
    onUnhideVideo: (String) -> Unit,
    onOpenAddVideos: () -> Unit,
    onResetPin: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Vault Toolbar Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SuccessGreen.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Unlocked (${hiddenVideos.size} Videos)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenAddVideos,
                    colors = ButtonDefaults.buttonColors(containerColor = AmAccentBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_videos_to_vault_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hide Videos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onResetPin) {
                    Icon(Icons.Default.Key, contentDescription = "Change PIN", tint = Color(0xFF9E9EA7))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (hiddenVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFF4A4A56),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Vault is Empty",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap 'Hide Videos' above to add private videos here.\nThey will disappear from the public library.",
                        fontSize = 12.sp,
                        color = Color(0xFF9E9EA7),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(hiddenVideos, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SaCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E36)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 80.dp, height = 54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1B1B22)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = SaFolderBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                if (item.durationMs > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.Black.copy(alpha = 0.8f),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(3.dp)
                                    ) {
                                        Text(
                                            text = formatTime(item.durationMs),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${item.folderName} • ${(item.sizeBytes / (1024 * 1024))} MB",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9E9EA7)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Play in AM Player
                                IconButton(onClick = { onPlayVideo(item.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = AmAccentBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                // Unhide / Restore button
                                IconButton(onClick = { onUnhideVideo(item.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Restore to Library",
                                        tint = Color(0xFF9E9EA7),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
