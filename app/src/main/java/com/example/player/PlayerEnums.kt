package com.example.player

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
