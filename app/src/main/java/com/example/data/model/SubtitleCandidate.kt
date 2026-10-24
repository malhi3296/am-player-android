package com.example.data.model

data class SubtitleCandidate(
    val id: String,
    val provider: String,
    val title: String,
    val language: String,
    val downloadUrl: String,
    val accuracyScore: Int, // 100% exact hash, 85% title+episode, 70% fuzzy
    val downloadsCount: Int,
    val format: String = "srt",
    val isExactHashMatch: Boolean = false
)
