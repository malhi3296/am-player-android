package com.example.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.player.PlayerHudState
import com.example.ui.theme.BoostOrange
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.PurpleNeon

@Composable
fun PlayerHud(
    hudState: PlayerHudState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = hudState !is PlayerHudState.None,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        when (hudState) {
            is PlayerHudState.Volume -> VolumeHudCard(hudState.value, hudState.isBoost)
            is PlayerHudState.Brightness -> BrightnessHudCard(hudState.value)
            is PlayerHudState.Seek -> SeekHudCard(hudState.targetTimeMs, hudState.totalDurationMs, hudState.diffMs)
            PlayerHudState.None -> Unit
        }
    }
}

@Composable
private fun VolumeHudCard(volume: Float, isBoost: Boolean) {
    val percentage = (volume * 100).toInt()
    val icon = when {
        volume == 0f -> Icons.Default.VolumeMute
        volume < 0.5f -> Icons.Default.VolumeDown
        else -> Icons.Default.VolumeUp
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xDD0C1017),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isBoost) BoostOrange.copy(alpha = 0.6f) else DarkBorder),
        modifier = Modifier.testTag("volume_hud_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Volume",
                tint = if (isBoost) BoostOrange else CyanNeon,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.width(140.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBoost) "AUDIO BOOST" else "VOLUME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBoost) BoostOrange else Color(0xFF94A3B8)
                    )
                    Text(
                        text = "$percentage%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom bar with 200% threshold
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (volume / 2.0f).coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isBoost) {
                                    Brush.horizontalGradient(listOf(CyanNeon, BoostOrange))
                                } else {
                                    Brush.horizontalGradient(listOf(CyanNeon, PurpleNeon))
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun BrightnessHudCard(brightness: Float) {
    val percentage = (brightness * 100).toInt()
    val icon = if (brightness < 0.5f) Icons.Default.BrightnessLow else Icons.Default.BrightnessHigh

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xDD0C1017),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.testTag("brightness_hud_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Brightness",
                tint = CyanNeon,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.width(140.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BRIGHTNESS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "$percentage%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = brightness.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(CyanNeon, PurpleNeon)))
                    )
                }
            }
        }
    }
}

@Composable
private fun SeekHudCard(targetTimeMs: Long, totalDurationMs: Long, diffMs: Long) {
    val isForward = diffMs >= 0
    val diffSec = (diffMs / 1000).toInt()

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xDD0C1017),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
        modifier = Modifier.testTag("seek_hud_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                contentDescription = if (isForward) "Fast Forward" else "Rewind",
                tint = CyanNeon,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "${if (isForward) "+" else ""}${diffSec}s",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyanNeon
                )
                Text(
                    text = "${formatTime(targetTimeMs)} / ${formatTime(totalDurationMs)}",
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}

fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
