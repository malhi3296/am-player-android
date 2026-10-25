package com.example.data.model

data class IptvPlaylistFolder(
    val id: String,
    val name: String,
    val colorHex: String = "#4D6BFE",
    val iconName: String = "tv", // tv, sports, news, movie, music, science, globe, star
    val channels: List<IptvChannel> = emptyList(),
    val isCustom: Boolean = false
)
