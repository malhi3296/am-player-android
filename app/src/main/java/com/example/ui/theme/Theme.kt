package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AmPlayerDarkColorScheme = darkColorScheme(
    primary = AmAccentBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E2D69),
    onPrimaryContainer = Color(0xFFDBE1FF),
    secondary = SaFolderBlue,
    onSecondary = Color.Black,
    secondaryContainer = SaCardBgElevated,
    onSecondaryContainer = Color.White,
    tertiary = SaFolderBadgeRed,
    background = SaDarkBg,
    onBackground = TextPrimaryDark,
    surface = SaDarkBg,
    onSurface = TextPrimaryDark,
    surfaceVariant = SaCardBg,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF2E2E36)
)

private val AmPlayerLightColorScheme = lightColorScheme(
    primary = AmAccentBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE1FF),
    onPrimaryContainer = Color(0xFF001550),
    secondary = Color(0xFF3856E8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDF1FC),
    onSecondaryContainer = Color(0xFF101217),
    tertiary = SaFolderBadgeRed,
    background = SaLightBg,
    onBackground = TextPrimaryLight,
    surface = SaLightCard,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFECEFF5),
    onSurfaceVariant = TextSecondaryLight,
    outline = SaLightCardBorder
)

@Composable
fun AMPlayerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AmPlayerDarkColorScheme else AmPlayerLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
