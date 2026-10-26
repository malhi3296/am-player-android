package com.example.data.model

data class AudioTrack(
    val id: String,
    val uri: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val folderName: String = "Music",
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val isFavorite: Boolean = false
)
