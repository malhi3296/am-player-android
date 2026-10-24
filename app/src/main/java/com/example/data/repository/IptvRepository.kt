package com.example.data.repository

import com.example.data.model.IptvChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class IptvRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val defaultChannels = listOf(
        IptvChannel(
            id = "iptv_nasa",
            name = "NASA TV UltraHD",
            category = "Science",
            streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
            country = "USA"
        ),
        IptvChannel(
            id = "iptv_bloomberg",
            name = "Bloomberg Financial News",
            category = "News",
            streamUrl = "https://bloomberg.com/media-manifest/streams/us.m3u8",
            country = "Global"
        ),
        IptvChannel(
            id = "iptv_dw",
            name = "Deutsche Welle (DW English)",
            category = "News",
            streamUrl = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
            country = "Germany"
        ),
        IptvChannel(
            id = "iptv_france24",
            name = "France 24 English",
            category = "News",
            streamUrl = "https://static.france24.com/live/F24_EN_LO_HLS/live_tv.m3u8",
            country = "France"
        ),
        IptvChannel(
            id = "iptv_euronews",
            name = "Euronews World",
            category = "News",
            streamUrl = "https://euronews-euronews-world-1-us.plex.tv/hls/main/index.m3u8",
            country = "Europe"
        ),
        IptvChannel(
            id = "iptv_redbull",
            name = "Red Bull Extreme Sports",
            category = "Sports",
            streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
            country = "Global"
        ),
        IptvChannel(
            id = "iptv_aljazeera",
            name = "Al Jazeera English HD",
            category = "News",
            streamUrl = "https://live-hls-web-aje.getaj.net/AJE/01.m3u8",
            country = "Global"
        ),
        IptvChannel(
            id = "iptv_demo_stream",
            name = "AM Cinema Showcase HLS",
            category = "Movies",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            country = "Global"
        )
    )

    private val _channels = MutableStateFlow<List<IptvChannel>>(defaultChannels)
    val channels: StateFlow<List<IptvChannel>> = _channels.asStateFlow()

    fun addCustomStream(name: String, url: String, category: String = "Custom") {
        val newChannel = IptvChannel(
            id = "custom_${System.currentTimeMillis()}",
            name = name.ifBlank { "Custom Live Stream" },
            category = category,
            streamUrl = url.trim(),
            isCustom = true
        )
        _channels.value = listOf(newChannel) + _channels.value
    }

    suspend fun importM3uPlaylist(playlistUrl: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(playlistUrl).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                return@withContext Result.failure(Exception("Failed to download playlist (HTTP ${response.code})"))
            }

            val text = response.body!!.string()
            val parsed = parseM3u(text)
            if (parsed.isEmpty()) {
                return@withContext Result.failure(Exception("No valid stream channels found in M3U file"))
            }

            _channels.value = parsed + _channels.value
            Result.success(parsed.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseM3u(content: String): List<IptvChannel> {
        val lines = content.lines()
        val list = mutableListOf<IptvChannel>()
        var currentName = ""
        var currentCategory = "General"

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                val groupMatch = Regex("group-title=\"([^\"]+)\"").find(trimmed)
                if (groupMatch != null) {
                    currentCategory = groupMatch.groupValues[1]
                }
                val commaIdx = trimmed.lastIndexOf(',')
                currentName = if (commaIdx != -1) trimmed.substring(commaIdx + 1).trim() else "Channel ${list.size + 1}"
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                list.add(
                    IptvChannel(
                        id = "m3u_${list.size}_${System.currentTimeMillis()}",
                        name = if (currentName.isNotBlank()) currentName else "Channel ${list.size + 1}",
                        category = currentCategory,
                        streamUrl = trimmed,
                        isCustom = true
                    )
                )
                currentName = ""
                currentCategory = "General"
            }
        }
        return list
    }
}
