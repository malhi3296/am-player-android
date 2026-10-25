package com.example.player

import androidx.compose.ui.graphics.Color

enum class AspectRatioMode(val displayName: String) {
    FIT("Fit to Screen"),
    FILL("Fill Screen"),
    ZOOM("100% Zoom"),
    RATIO_16_9("16:9 Widescreen"),
    RATIO_4_3("4:3 Standard"),
    STRETCH("Stretch")
}

enum class DecoderMode(val displayName: String) {
    HW_PLUS("HW+ (Hardware Accelerated)"),
    HW("HW (Standard Hardware)"),
    SW("SW (Software Decoded)")
}

enum class OrientationLock(val displayName: String) {
    AUTO("Sensor Auto"),
    LANDSCAPE("Landscape"),
    PORTRAIT("Portrait")
}

enum class AudioPreset(val displayName: String, val bassGainMb: Int, val trebleGainMb: Int) {
    NORMAL("Balanced", 0, 0),
    BASS_BOOST("Bass Boost", 800, 200),
    CINEMA("Cinema Theater", 500, 600),
    VOCAL_CLARITY("Vocal Clarity", -200, 800),
    NIGHT_MODE("Night Mode", -400, -300)
}

enum class PlayerSkin(
    val id: String,
    val displayName: String,
    val description: String,
    val accentColor: Color,
    val secondaryColor: Color,
    val surfaceColor: Color,
    val cardColor: Color
) {
    AM_ROYAL_BLUE(
        id = "am_blue",
        displayName = "AM Royal Blue",
        description = "Modern #4D6BFE accent on Pitch Black OLED",
        accentColor = Color(0xFF4D6BFE),
        secondaryColor = Color(0xFF8EC5FC),
        surfaceColor = Color(0xFF000000),
        cardColor = Color(0xFF19181D)
    ),
    OLED_CYAN(
        id = "oled_cyan",
        displayName = "OLED Neon Cyan",
        description = "Electric cyan & neon violet glow",
        accentColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFFB388FF),
        surfaceColor = Color(0xFF06090E),
        cardColor = Color(0xFF0E131B)
    ),
    MX_AMBER(
        id = "mx_amber",
        displayName = "MX Classic Amber",
        description = "Nostalgic golden amber & slate",
        accentColor = Color(0xFFFFB300),
        secondaryColor = Color(0xFFFFD54F),
        surfaceColor = Color(0xFF12141A),
        cardColor = Color(0xFF1C1E26)
    ),
    CYBERPUNK(
        id = "cyberpunk",
        displayName = "Cyberpunk Neon",
        description = "Vibrant magenta pink & neon turquoise",
        accentColor = Color(0xFFFF007F),
        secondaryColor = Color(0xFF00F0FF),
        surfaceColor = Color(0xFF0E0818),
        cardColor = Color(0xFF1B1228)
    ),
    EMERALD_CINEMA(
        id = "emerald",
        displayName = "Emerald Cinema",
        description = "Deep forest obsidian & vivid green",
        accentColor = Color(0xFF00E676),
        secondaryColor = Color(0xFF69F0AE),
        surfaceColor = Color(0xFF06120C),
        cardColor = Color(0xFF102117)
    )
}

enum class VideoColorMode(
    val id: String,
    val displayName: String,
    val description: String,
    val filterColor: Color = Color.Transparent,
    val saturation: Float = 1.0f,
    val contrast: Float = 1.0f
) {
    NORMAL("normal", "Original", "Default natural colors", Color.Transparent, 1.0f, 1.0f),
    VIVID("vivid", "Vivid & Dynamic", "Boosted contrast & richer colors", Color.Transparent, 1.35f, 1.15f),
    WARM("warm", "Warm Cinema", "Hollywood golden movie tones", Color(0x22FFA726), 1.1f, 1.05f),
    NIGHT_CARE("night", "Eye Care / Night", "Blue-light shield for dark rooms", Color(0x38FF9800), 0.95f, 1.0f),
    COOL("cool", "Cool Crisp", "Crisp whites and clear outdoor tones", Color(0x1F00E5FF), 1.05f, 1.05f),
    NOIR("noir", "B&W Noir", "Classic high-contrast monochrome", Color.Transparent, 0.0f, 1.25f)
}

