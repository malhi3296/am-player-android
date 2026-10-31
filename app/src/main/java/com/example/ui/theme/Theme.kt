package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun parseHexColor(hex: String, fallback: Color = AmAccentBlue): Color {
    return try {
        val clean = hex.removePrefix("#")
        val longVal = clean.toLong(16)
        if (clean.length == 6) {
            Color(longVal or 0xFF000000)
        } else {
            Color(longVal)
        }
    } catch (e: Exception) {
        fallback
    }
}

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
    accentColor: Color = AmAccentBlue,
    darkBgTheme: String = "OLED Black",
    lightBgTheme: String = "Cool Gray",
    content: @Composable () -> Unit
) {
    val darkBgColor = when (darkBgTheme) {
        "Midnight Navy" -> Color(0xFF0A0E1A)
        "Deep Charcoal" -> Color(0xFF121216)
        "Cyber Slate" -> Color(0xFF161922)
        else -> SaDarkBg // OLED Black (0xFF000000)
    }

    val lightBgColor = when (lightBgTheme) {
        "Pure White" -> Color(0xFFFFFFFF)
        "Warm Cream" -> Color(0xFFFDFBF7)
        "Soft Silver" -> Color(0xFFEDEDF2)
        else -> SaLightBg // Cool Gray (0xFFF6F8FC)
    }

    val baseScheme = if (darkTheme) {
        AmPlayerDarkColorScheme.copy(
            background = darkBgColor,
            surface = darkBgColor
        )
    } else {
        AmPlayerLightColorScheme.copy(
            background = lightBgColor,
            surface = if (lightBgColor == Color(0xFFFFFFFF)) Color(0xFFF8FAFC) else Color(0xFFFFFFFF)
        )
    }

    val colorScheme = baseScheme.copy(
        primary = accentColor
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
