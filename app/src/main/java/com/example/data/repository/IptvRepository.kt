package com.example.data.repository

import android.content.Context
import com.example.data.model.IptvChannel
import com.example.data.model.IptvPlaylistFolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class SuggestedPlaylist(
    val title: String,
    val description: String,
    val url: String,
    val isRadio: Boolean = false
)

class IptvRepository(private val context: Context? = null) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val prefs = context?.getSharedPreferences("am_player_iptv_favorites", Context.MODE_PRIVATE)

    companion object {
        const val UNIFORM_FOLDER_COLOR = "#4D6BFE"
        const val URL_WORLD = "https://iptv-org.github.io/iptv/index.m3u"
        const val URL_INDIA = "https://iptv-org.github.io/iptv/countries/in.m3u"
        const val URL_NEWS = "https://iptv-org.github.io/iptv/categories/news.m3u"
        const val URL_SPORTS = "https://iptv-org.github.io/iptv/categories/sports.m3u"
    }

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _folders = MutableStateFlow<List<IptvPlaylistFolder>>(emptyList())
    val folders: StateFlow<List<IptvPlaylistFolder>> = _folders.asStateFlow()

    private val _channels = MutableStateFlow<List<IptvChannel>>(emptyList())
    val channels: StateFlow<List<IptvChannel>> = _channels.asStateFlow()

    private var radioStationsList: List<IptvChannel> = emptyList()

    init {
        loadSavedFavorites()
        loadCuratedPlaylists()
        loadRadioStations()
    }

    private fun loadSavedFavorites() {
        val saved = prefs?.getStringSet("fav_channel_ids", emptySet()) ?: emptySet()
        _favoriteIds.value = saved
    }

    private fun saveFavorites(favs: Set<String>) {
        _favoriteIds.value = favs
        prefs?.edit()?.putStringSet("fav_channel_ids", favs)?.apply()
        rebuildFavoritesFolder()
    }

    fun isFavorite(channelId: String): Boolean {
        return _favoriteIds.value.contains(channelId)
    }

    fun toggleFavorite(channelId: String) {
        val current = _favoriteIds.value.toMutableSet()
        if (current.contains(channelId)) {
            current.remove(channelId)
        } else {
            current.add(channelId)
        }
        saveFavorites(current)
    }

    private fun rebuildFavoritesFolder() {
        val favIds = _favoriteIds.value
        val allOtherChannels = _folders.value.filter { it.id != "folder_fav" }.flatMap { it.channels }
        val favChannels = allOtherChannels.filter { favIds.contains(it.id) || favIds.contains(it.streamUrl) }
            .distinctBy { it.streamUrl }

        _folders.value = _folders.value.map { folder ->
            if (folder.id == "folder_fav") {
                folder.copy(channels = favChannels)
            } else folder
        }
        _channels.value = _folders.value.flatMap { it.channels }
    }

    private fun loadRadioStations() {
        try {
            context?.assets?.open("default_radio_channels.m3u")?.bufferedReader()?.use { reader ->
                radioStationsList = parseM3u(reader.readText(), "Radio")
            }
        } catch (_: Exception) {}
    }

    fun getRadioStations(): List<IptvChannel> {
        return radioStationsList
    }

    private fun loadCuratedPlaylists() {
        val initialFolders = mutableListOf<IptvPlaylistFolder>()

        // 1. PINNED FAVORITES FOLDER AT TOP
        initialFolders.add(
            IptvPlaylistFolder(
                id = "folder_fav",
                name = "Favourites",
                colorHex = "#FFD700",
                iconName = "star",
                channels = emptyList(),
                isCustom = false
            )
        )

        // 2. WORLD
        initialFolders.add(
            IptvPlaylistFolder(
                id = "folder_world",
                name = "World",
                colorHex = UNIFORM_FOLDER_COLOR,
                iconName = "globe",
                channels = getPreloadedChannelsForCategory("World"),
                isCustom = false
            )
        )

        // 3. INDIA
        initialFolders.add(
            IptvPlaylistFolder(
                id = "folder_india",
                name = "India",
                colorHex = UNIFORM_FOLDER_COLOR,
                iconName = "flag",
                channels = getPreloadedChannelsForCategory("India"),
                isCustom = false
            )
        )

        // 4. NEWS
        initialFolders.add(
            IptvPlaylistFolder(
                id = "folder_news",
                name = "News",
                colorHex = UNIFORM_FOLDER_COLOR,
                iconName = "newspaper",
                channels = getPreloadedChannelsForCategory("News"),
                isCustom = false
            )
        )

        // 5. SPORTS
        initialFolders.add(
            IptvPlaylistFolder(
                id = "folder_sports",
                name = "Sports",
                colorHex = UNIFORM_FOLDER_COLOR,
                iconName = "sports",
                channels = getPreloadedChannelsForCategory("Sports"),
                isCustom = false
            )
        )

        _folders.value = initialFolders
        _channels.value = initialFolders.flatMap { it.channels }
        rebuildFavoritesFolder()

        // Fetch remote playlists in background
        CoroutineScope(Dispatchers.IO).launch {
            refreshRemoteCuratedPlaylists()
        }
    }

    private suspend fun refreshRemoteCuratedPlaylists() {
        val targets = listOf(
            Triple("folder_world", URL_WORLD, "World"),
            Triple("folder_india", URL_INDIA, "India"),
            Triple("folder_news", URL_NEWS, "News"),
            Triple("folder_sports", URL_SPORTS, "Sports")
        )

        for ((folderId, url, defaultCat) in targets) {
            try {
                val req = Request.Builder().url(url).build()
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful && resp.body != null) {
                    val parsed = parseM3u(resp.body!!.string(), defaultCat)
                    if (parsed.isNotEmpty()) {
                        val limited = if (folderId == "folder_world") parsed.take(150) else parsed
                        updateFolderChannels(folderId, limited)
                    }
                }
            } catch (_: Exception) {}
        }
        rebuildFavoritesFolder()
    }

    private fun updateFolderChannels(folderId: String, newChannels: List<IptvChannel>) {
        _folders.value = _folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(channels = newChannels)
            } else folder
        }
        _channels.value = _folders.value.flatMap { it.channels }
    }

    fun renameFolder(folderId: String, newName: String) {
        _folders.value = _folders.value.map { folder ->
            if (folder.id == folderId) folder.copy(name = newName.trim()) else folder
        }
    }

    fun deleteFolder(folderId: String) {
        if (folderId == "folder_fav") return // Pinned folder cannot be deleted
        _folders.value = _folders.value.filter { it.id != folderId }
        _channels.value = _folders.value.flatMap { it.channels }
    }

    fun renameChannel(folderId: String, channelId: String, newName: String) {
        _folders.value = _folders.value.map { folder ->
            if (folder.id == folderId || folder.channels.any { it.id == channelId }) {
                folder.copy(channels = folder.channels.map { chan ->
                    if (chan.id == channelId) chan.copy(name = newName.trim()) else chan
                })
            } else folder
        }
        _channels.value = _folders.value.flatMap { it.channels }
    }

    fun deleteChannel(folderId: String, channelId: String) {
        _folders.value = _folders.value.map { folder ->
            folder.copy(channels = folder.channels.filter { it.id != channelId })
        }
        _channels.value = _folders.value.flatMap { it.channels }
    }

    suspend fun importM3uPlaylist(folderName: String, colorHex: String, iconName: String, playlistUrlOrContent: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val content = if (playlistUrlOrContent.startsWith("http://", ignoreCase = true) || playlistUrlOrContent.startsWith("https://", ignoreCase = true)) {
                val req = Request.Builder().url(playlistUrlOrContent.trim()).build()
                val resp = httpClient.newCall(req).execute()
                if (!resp.isSuccessful || resp.body == null) {
                    return@withContext Result.failure(Exception("Failed to download playlist (HTTP ${resp.code})"))
                }
                resp.body!!.string()
            } else {
                playlistUrlOrContent
            }

            val parsed = parseM3u(content, folderName)
            if (parsed.isEmpty()) {
                // If no M3U tags, check if it's a single stream URL
                if (playlistUrlOrContent.startsWith("http://") || playlistUrlOrContent.startsWith("https://")) {
                    val singleChan = IptvChannel(
                        id = "stream_${System.currentTimeMillis()}",
                        name = "$folderName Stream",
                        category = folderName,
                        streamUrl = playlistUrlOrContent.trim(),
                        isCustom = true
                    )
                    val newFolder = IptvPlaylistFolder(
                        id = "folder_${System.currentTimeMillis()}",
                        name = folderName.ifBlank { "Custom Playlist" },
                        colorHex = UNIFORM_FOLDER_COLOR,
                        iconName = iconName,
                        channels = listOf(singleChan),
                        isCustom = true
                    )
                    _folders.value = _folders.value + newFolder
                    _channels.value = listOf(singleChan) + _channels.value
                    return@withContext Result.success(1)
                }
                return@withContext Result.failure(Exception("No valid stream channels found"))
            }

            val newFolder = IptvPlaylistFolder(
                id = "folder_${System.currentTimeMillis()}",
                name = folderName.ifBlank { "Imported Playlist" },
                colorHex = UNIFORM_FOLDER_COLOR,
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

    fun parseM3u(content: String, defaultCategory: String): List<IptvChannel> {
        val lines = content.lines()
        val list = mutableListOf<IptvChannel>()
        var currentName = ""
        var currentCategory = defaultCategory
        var currentCountry = "Global"
        var currentLogo: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                val groupMatch = Regex("group-title=\"([^\"]+)\"").find(trimmed)
                if (groupMatch != null) {
                    currentCategory = groupMatch.groupValues[1]
                }
                val logoMatch = Regex("tvg-logo=\"([^\"]+)\"").find(trimmed)
                if (logoMatch != null) {
                    currentLogo = logoMatch.groupValues[1]
                }
                val commaIdx = trimmed.lastIndexOf(',')
                currentName = if (commaIdx != -1) trimmed.substring(commaIdx + 1).trim() else "Channel ${list.size + 1}"
            } else if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                list.add(
                    IptvChannel(
                        id = "m3u_${list.size}_${System.currentTimeMillis()}",
                        name = if (currentName.isNotBlank()) currentName else "Channel ${list.size + 1}",
                        category = currentCategory,
                        country = currentCountry,
                        streamUrl = trimmed,
                        logoUrl = currentLogo ?: "",
                        isCustom = true
                    )
                )
                currentName = ""
                currentCategory = defaultCategory
                currentLogo = null
            }
        }
        return list
    }

    private fun getPreloadedChannelsForCategory(category: String): List<IptvChannel> {
        return when (category) {
            "Punjabi" -> listOf(
                IptvChannel(
                    id = "pb_1",
                    name = "PTC Punjabi HD",
                    category = "Punjabi",
                    country = "India",
                    streamUrl = "https://live.ptcpunjabi.com/hls/live/ptc_punjabi/master.m3u8"
                ),
                IptvChannel(
                    id = "pb_2",
                    name = "PTC News Live",
                    category = "Punjabi",
                    country = "India",
                    streamUrl = "https://live.ptcpunjabi.com/hls/live/ptc_news/master.m3u8"
                ),
                IptvChannel(
                    id = "pb_3",
                    name = "Chardikla Time TV",
                    category = "Punjabi",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/timetv/master.m3u8"
                ),
                IptvChannel(
                    id = "pb_4",
                    name = "9X Tashan HD",
                    category = "Punjabi",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/9xtashan/master.m3u8"
                ),
                IptvChannel(
                    id = "pb_5",
                    name = "Fastway Punjabi",
                    category = "Punjabi",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/fastway/master.m3u8"
                ),
                IptvChannel(
                    id = "pb_6",
                    name = "MH One Music",
                    category = "Punjabi",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/mhone/master.m3u8"
                )
            )
            "India" -> listOf(
                IptvChannel(
                    id = "in_1",
                    name = "DD National HD",
                    category = "India",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/dd-national/master.m3u8"
                ),
                IptvChannel(
                    id = "in_2",
                    name = "DD News Live",
                    category = "India",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/dd-news/master.m3u8"
                ),
                IptvChannel(
                    id = "in_3",
                    name = "India TV Live",
                    category = "India",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/indiatv/master.m3u8"
                ),
                IptvChannel(
                    id = "in_4",
                    name = "NDTV 24x7 HD",
                    category = "India",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/ndtv24x7/master.m3u8"
                ),
                IptvChannel(
                    id = "in_5",
                    name = "DD Sports HD",
                    category = "India",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/ddsports/master.m3u8"
                ),
                IptvChannel(
                    id = "in_6",
                    name = "WION World Is One",
                    category = "India",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/wion/master.m3u8"
                )
            )
            "News" -> listOf(
                IptvChannel(
                    id = "news_1",
                    name = "Sky News UK",
                    category = "News",
                    country = "UK",
                    streamUrl = "https://live.sky.com/hls/live/2007282/skynews_extra/master.m3u8"
                ),
                IptvChannel(
                    id = "news_2",
                    name = "DW English HD",
                    category = "News",
                    country = "Germany",
                    streamUrl = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8"
                ),
                IptvChannel(
                    id = "news_3",
                    name = "France 24 English",
                    category = "News",
                    country = "France",
                    streamUrl = "https://static.france24.com/live/F24_EN_LO_HLS/live_tv.m3u8"
                ),
                IptvChannel(
                    id = "news_4",
                    name = "Al Jazeera English",
                    category = "News",
                    country = "Qatar",
                    streamUrl = "https://live-hls-web-aje.getaj.net/AJE/01.m3u8"
                ),
                IptvChannel(
                    id = "news_5",
                    name = "Euronews English",
                    category = "News",
                    country = "Europe",
                    streamUrl = "https://euronews-euronews-english-1-eu.rakuten.wurl.tv/playlist.m3u8"
                ),
                IptvChannel(
                    id = "news_6",
                    name = "ABC News Live",
                    category = "News",
                    country = "USA",
                    streamUrl = "https://content.uplynk.com/channel/3324f2467c414329b3b0cc5cd987b6be.m3u8"
                )
            )
            "Sports" -> listOf(
                IptvChannel(
                    id = "sp_1",
                    name = "Red Bull TV",
                    category = "Sports",
                    country = "Global",
                    streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8"
                ),
                IptvChannel(
                    id = "sp_2",
                    name = "World Poker Tour",
                    category = "Sports",
                    country = "Global",
                    streamUrl = "https://wpt-live.akamaized.net/hls/live/2026859/wpt_live/master.m3u8"
                ),
                IptvChannel(
                    id = "sp_3",
                    name = "Stadium Sports Live",
                    category = "Sports",
                    country = "USA",
                    streamUrl = "https://stadium-live.akamaized.net/hls/live/2014075/stadium/master.m3u8"
                ),
                IptvChannel(
                    id = "sp_4",
                    name = "MAVTV Motorsports",
                    category = "Sports",
                    country = "USA",
                    streamUrl = "https://mavtv-live.akamaized.net/hls/live/2026860/mavtv/master.m3u8"
                ),
                IptvChannel(
                    id = "sp_5",
                    name = "Action Sports HD",
                    category = "Sports",
                    country = "USA",
                    streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                )
            )
            else -> listOf(
                IptvChannel(
                    id = "w_1",
                    name = "NASA TV Public",
                    category = "World",
                    country = "USA",
                    streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8"
                ),
                IptvChannel(
                    id = "w_2",
                    name = "Arirang TV HD",
                    category = "World",
                    country = "Korea",
                    streamUrl = "https://amdlive.ctnd.com.edgesuite.net/arirang_1ch/smil:arirang_1ch.smil/playlist.m3u8"
                ),
                IptvChannel(
                    id = "w_3",
                    name = "Sky News Global",
                    category = "World",
                    country = "UK",
                    streamUrl = "https://live.sky.com/hls/live/2007282/skynews_extra/master.m3u8"
                ),
                IptvChannel(
                    id = "w_4",
                    name = "DW English Live",
                    category = "World",
                    country = "Germany",
                    streamUrl = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8"
                ),
                IptvChannel(
                    id = "w_5",
                    name = "France 24 English",
                    category = "World",
                    country = "France",
                    streamUrl = "https://static.france24.com/live/F24_EN_LO_HLS/live_tv.m3u8"
                )
            )
        }
    }

    private fun getCuratedYouTubeLiveStreams(): List<IptvChannel> {
        return listOf(
            IptvChannel(
                id = "yt_1",
                name = "ICC Cricket Match Highlights & Live",
                category = "Cricket",
                country = "India",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            ),
            IptvChannel(
                id = "yt_2",
                name = "T20 Cricket Watchalong Live",
                category = "Cricket",
                country = "India",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            ),
            IptvChannel(
                id = "yt_3",
                name = "Premier League Matchday Audio & Live",
                category = "Football",
                country = "UK",
                streamUrl = "https://live.sky.com/hls/live/2007282/skynews_extra/master.m3u8"
            ),
            IptvChannel(
                id = "yt_4",
                name = "Champions League Football Live",
                category = "Football",
                country = "Global",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8"
            ),
            IptvChannel(
                id = "yt_5",
                name = "F1 Race Weekend Paddock Live",
                category = "Motorsports",
                country = "Global",
                streamUrl = "https://mavtv-live.akamaized.net/hls/live/2026860/mavtv/master.m3u8"
            ),
            IptvChannel(
                id = "yt_6",
                name = "MotoGP Qualifying & Grand Prix",
                category = "Motorsports",
                country = "Global",
                streamUrl = "https://wpt-live.akamaized.net/hls/live/2026859/wpt_live/master.m3u8"
            ),
            IptvChannel(
                id = "yt_7",
                name = "WWE SmackDown Live Stream & Reactions",
                category = "Combat",
                country = "USA",
                streamUrl = "https://stadium-live.akamaized.net/hls/live/2014075/stadium/master.m3u8"
            ),
            IptvChannel(
                id = "yt_8",
                name = "UFC Fight Night Live Broadcast",
                category = "Combat",
                country = "USA",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8"
            ),
            IptvChannel(
                id = "yt_9",
                name = "NBA Tonight Game Day Live",
                category = "Basketball",
                country = "USA",
                streamUrl = "https://stadium-live.akamaized.net/hls/live/2014075/stadium/master.m3u8"
            ),
            IptvChannel(
                id = "yt_10",
                name = "Wimbledon & Grand Slam Tennis Live",
                category = "Tennis",
                country = "UK",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
            )
        )
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
            colorHex = UNIFORM_FOLDER_COLOR,
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

    fun getSuggestedOnlinePlaylists(): List<SuggestedPlaylist> {
        return listOf(
            SuggestedPlaylist("World Index", "Global television channels", URL_WORLD),
            SuggestedPlaylist("India Channels", "National & regional Hindi/Indian broadcasts", URL_INDIA),
            SuggestedPlaylist("News Broadcasts", "24/7 world news networks (iptv-org)", URL_NEWS),
            SuggestedPlaylist("Sports Live", "Motorsports, extreme sports & athletics", URL_SPORTS)
        )
    }
}
