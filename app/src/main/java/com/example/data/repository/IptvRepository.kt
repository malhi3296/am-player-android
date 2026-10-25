package com.example.data.repository

import com.example.data.model.IptvChannel
import com.example.data.model.IptvPlaylistFolder
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

    private val newsChannels = listOf(
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
            id = "iptv_aljazeera",
            name = "Al Jazeera English HD",
            category = "News",
            streamUrl = "https://live-hls-web-aje.getaj.net/AJE/01.m3u8",
            country = "Global"
        )
    )

    private val sportsChannels = listOf(
        IptvChannel(
            id = "iptv_redbull",
            name = "Red Bull Extreme Sports",
            category = "Sports",
            streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
            country = "Global"
        ),
        IptvChannel(
            id = "iptv_extreme_action",
            name = "Extreme Action Live HD",
            category = "Sports",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            country = "USA"
        )
    )

    private val scienceChannels = listOf(
        IptvChannel(
            id = "iptv_nasa",
            name = "NASA TV UltraHD",
            category = "Science",
            streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
            country = "USA"
        ),
        IptvChannel(
            id = "iptv_space_station",
            name = "ISS Live Earth Stream",
            category = "Science",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            country = "Global"
        )
    )

    private val movieChannels = listOf(
        IptvChannel(
            id = "iptv_demo_stream",
            name = "AM Cinema Showcase HLS",
            category = "Movies",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            country = "Global"
        ),
        IptvChannel(
            id = "iptv_classic_films",
            name = "Retro Hollywood Classics",
            category = "Movies",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            country = "USA"
        )
    )

    private val initialFolders = listOf(
        IptvPlaylistFolder(
            id = "folder_news",
            name = "World News",
            colorHex = "#4D6BFE", // AM Accent Blue
            iconName = "news",
            channels = newsChannels
        ),
        IptvPlaylistFolder(
            id = "folder_sports",
            name = "Sports Live",
            colorHex = "#EF4444", // Coral Red
            iconName = "sports",
            channels = sportsChannels
        ),
        IptvPlaylistFolder(
            id = "folder_science",
            name = "Science & Space",
            colorHex = "#10B981", // Emerald Green
            iconName = "science",
            channels = scienceChannels
        ),
        IptvPlaylistFolder(
            id = "folder_cinema",
            name = "Cinema & Movies",
            colorHex = "#8B5CF6", // Purple Neon
            iconName = "movie",
            channels = movieChannels
        )
    )

    private val _folders = MutableStateFlow<List<IptvPlaylistFolder>>(initialFolders)
    val folders: StateFlow<List<IptvPlaylistFolder>> = _folders.asStateFlow()

    private val _channels = MutableStateFlow<List<IptvChannel>>(
        initialFolders.flatMap { it.channels }
    )
    val channels: StateFlow<List<IptvChannel>> = _channels.asStateFlow()

    fun updateFolder(folderId: String, newName: String, newColorHex: String, newIconName: String) {
        _folders.value = _folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(
                    name = newName.ifBlank { folder.name },
                    colorHex = newColorHex,
                    iconName = newIconName
                )
            } else folder
        }
    }

    fun createFolder(name: String, colorHex: String, iconName: String, initialStreamUrl: String? = null): IptvPlaylistFolder {
        val initialChannelList = if (!initialStreamUrl.isNullOrBlank()) {
            listOf(
                IptvChannel(
                    id = "custom_chan_${System.currentTimeMillis()}",
                    name = "$name Channel 1",
                    category = name,
                    streamUrl = initialStreamUrl.trim(),
                    isCustom = true
                )
            )
        } else emptyList()

        val newFolder = IptvPlaylistFolder(
            id = "folder_${System.currentTimeMillis()}",
            name = name.ifBlank { "Custom Playlist" },
            colorHex = colorHex,
            iconName = iconName,
            channels = initialChannelList,
            isCustom = true
        )

        _folders.value = _folders.value + newFolder
        _channels.value = _channels.value + initialChannelList
        return newFolder
    }

    fun addCustomStreamToFolder(folderId: String, channelName: String, streamUrl: String) {
        val newChan = IptvChannel(
            id = "custom_${System.currentTimeMillis()}",
            name = channelName.ifBlank { "Custom Live Stream" },
            category = "Custom",
            streamUrl = streamUrl.trim(),
            isCustom = true
        )

        _folders.value = _folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(channels = listOf(newChan) + folder.channels)
            } else folder
        }
        _channels.value = listOf(newChan) + _channels.value
    }

    suspend fun importM3uPlaylist(folderName: String, colorHex: String, iconName: String, playlistUrl: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(playlistUrl).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                return@withContext Result.failure(Exception("Failed to download playlist (HTTP ${response.code})"))
            }

            val text = response.body!!.string()
            val parsed = parseM3u(text, folderName)
            if (parsed.isEmpty()) {
                return@withContext Result.failure(Exception("No valid stream channels found in M3U file"))
            }

            val newFolder = IptvPlaylistFolder(
                id = "folder_${System.currentTimeMillis()}",
                name = folderName.ifBlank { "Imported M3U Playlist" },
                colorHex = colorHex,
                iconName = iconName,
                channels = parsed,
                isCustom = true
            )

            _folders.value = _folders.value + newFolder
            _channels.value = parsed + _channels.value
            Result.success(parsed.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseM3u(content: String, defaultCategory: String): List<IptvChannel> {
        val lines = content.lines()
        val list = mutableListOf<IptvChannel>()
        var currentName = ""
        var currentCategory = defaultCategory

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
                currentCategory = defaultCategory
            }
        }
        return list
    }
}
