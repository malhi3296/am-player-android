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
        const val URL_PUNJABI = "https://raw.githubusercontent.com/malhi3296/TV-garden-M3u-playlists/refs/heads/main/punjabi.m3u"
        const val URL_NEWS = "https://raw.githubusercontent.com/malhi3296/TV-garden-M3u-playlists/refs/heads/main/news.m3u"
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

        // 4. PUNJABI
        initialFolders.add(
            IptvPlaylistFolder(
                id = "folder_punjabi",
                name = "Punjabi",
                colorHex = UNIFORM_FOLDER_COLOR,
                iconName = "tv",
                channels = getPreloadedChannelsForCategory("Punjabi"),
                isCustom = false
            )
        )

        // 5. NEWS
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

        // 6. SPORTS
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

        // 7. YOUTUBE LIVE (Filterable by Country & Sports)
        initialFolders.add(
            IptvPlaylistFolder(
                id = "folder_youtube_live",
                name = "YouTube Live",
                colorHex = "#FF0000",
                iconName = "youtube",
                channels = getCuratedYouTubeLiveStreams(),
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
            Triple("folder_punjabi", URL_PUNJABI, "Punjabi"),
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
                IptvChannel("pb_1", "PTC Punjabi HD", "Punjabi", "India", "http://stream.tvgarden.world/live/ptcpunjabi.m3u8"),
                IptvChannel("pb_2", "PTC News Live", "Punjabi", "India", "http://stream.tvgarden.world/live/ptcnews.m3u8"),
                IptvChannel("pb_3", "Zee Punjabi HD", "Punjabi", "India", "http://stream.tvgarden.world/live/zeepunjabi.m3u8"),
                IptvChannel("pb_4", "MH One Music", "Punjabi", "India", "http://stream.tvgarden.world/live/mhone.m3u8"),
                IptvChannel("pb_5", "9X Tashan HD", "Punjabi", "India", "http://stream.tvgarden.world/live/9xtashan.m3u8"),
                IptvChannel("pb_6", "Chardikla Time TV", "Punjabi", "India", "http://stream.tvgarden.world/live/timetv.m3u8"),
                IptvChannel("pb_7", "Living India News", "Punjabi", "India", "http://stream.tvgarden.world/live/livingindia.m3u8"),
                IptvChannel("pb_8", "Fastway Punjabi", "Punjabi", "India", "http://stream.tvgarden.world/live/fastway.m3u8")
            )
            "India" -> listOf(
                IptvChannel("in_1", "DD National HD", "India", "India", "http://stream.tvgarden.world/live/ddnational.m3u8"),
                IptvChannel("in_2", "Aaj Tak Live", "India", "India", "http://stream.tvgarden.world/live/aajtak.m3u8"),
                IptvChannel("in_3", "NDTV India", "India", "India", "http://stream.tvgarden.world/live/ndtvindia.m3u8"),
                IptvChannel("in_4", "Zee News HD", "India", "India", "http://stream.tvgarden.world/live/zeenews.m3u8"),
                IptvChannel("in_5", "India TV Live", "India", "India", "http://stream.tvgarden.world/live/indiatv.m3u8"),
                IptvChannel("in_6", "Republic Bharat", "India", "India", "http://stream.tvgarden.world/live/republicbharat.m3u8"),
                IptvChannel("in_7", "News18 India", "India", "India", "http://stream.tvgarden.world/live/news18india.m3u8"),
                IptvChannel("in_8", "DD Sports HD", "India", "India", "http://stream.tvgarden.world/live/ddsports.m3u8")
            )
            "News" -> listOf(
                IptvChannel("news_1", "BBC News HD", "News", "UK", "http://stream.tvgarden.world/live/bbcnews.m3u8"),
                IptvChannel("news_2", "CNN International", "News", "USA", "http://stream.tvgarden.world/live/cnn.m3u8"),
                IptvChannel("news_3", "Al Jazeera English", "News", "Qatar", "http://stream.tvgarden.world/live/aljazeera_en.m3u8"),
                IptvChannel("news_4", "DW English HD", "News", "Germany", "http://stream.tvgarden.world/live/dw_en.m3u8"),
                IptvChannel("news_5", "Sky News UK", "News", "UK", "https://live.sky.com/hls/live/2007282/skynews_extra/master.m3u8"),
                IptvChannel("news_6", "France 24 English", "News", "France", "http://stream.tvgarden.world/live/france24_en.m3u8"),
                IptvChannel("news_7", "Euronews English", "News", "Europe", "http://stream.tvgarden.world/live/euronews_en.m3u8"),
                IptvChannel("news_8", "NDTV 24x7 HD", "News", "India", "http://stream.tvgarden.world/live/ndtv24x7.m3u8"),
                IptvChannel("news_9", "WION World Is One", "News", "India", "http://stream.tvgarden.world/live/wion.m3u8")
            )
            "Sports" -> listOf(
                IptvChannel("sp_1", "Red Bull TV", "Sports", "Global", "http://stream.tvgarden.world/live/redbulltv.m3u8"),
                IptvChannel("sp_2", "Motorsport TV", "Sports", "Global", "http://stream.tvgarden.world/live/motorsporttv.m3u8"),
                IptvChannel("sp_3", "Unbeaten Sports HD", "Sports", "USA", "http://stream.tvgarden.world/live/unbeaten.m3u8"),
                IptvChannel("sp_4", "World Poker Tour", "Sports", "Global", "http://stream.tvgarden.world/live/wpt.m3u8"),
                IptvChannel("sp_5", "Fight Network HD", "Sports", "USA", "http://stream.tvgarden.world/live/fightnet.m3u8"),
                IptvChannel("sp_6", "Impact Wrestling Live", "Sports", "USA", "http://stream.tvgarden.world/live/impact.m3u8"),
                IptvChannel("sp_7", "MAVTV Motorsports", "Sports", "USA", "http://stream.tvgarden.world/live/mavtv.m3u8"),
                IptvChannel("sp_8", "Stadium Sports Live", "Sports", "USA", "http://stream.tvgarden.world/live/stadium.m3u8")
            )
            else -> listOf(
                IptvChannel("w_1", "NHK World Japan", "World", "Japan", "http://stream.tvgarden.world/live/nhk_world.m3u8"),
                IptvChannel("w_2", "CGTN Documentary", "World", "China", "http://stream.tvgarden.world/live/cgtn_doc.m3u8"),
                IptvChannel("w_3", "NASA TV Public", "World", "USA", "http://stream.tvgarden.world/live/nasatv.m3u8"),
                IptvChannel("w_4", "Arirang TV HD", "World", "Korea", "https://amdlive.ctnd.com.edgesuite.net/arirang_1ch/smil:arirang_1ch.smil/playlist.m3u8"),
                IptvChannel("w_5", "WildEarth Safari", "World", "Global", "http://stream.tvgarden.world/live/wildearth.m3u8")
            )
        }
    }

    private fun getCuratedYouTubeLiveStreams(): List<IptvChannel> {
        return listOf(
            IptvChannel("yt_1", "ICC Cricket Live Stream", "Cricket", "India", "http://stream.tvgarden.world/live/cricket_live.m3u8"),
            IptvChannel("yt_2", "T20 Match Highlights & Watchalong", "Cricket", "India", "http://stream.tvgarden.world/live/t20_live.m3u8"),
            IptvChannel("yt_3", "Premier League Live Matchday Audio & Reactions", "Football", "UK", "http://stream.tvgarden.world/live/pl_live.m3u8"),
            IptvChannel("yt_4", "Champions League Live Stream", "Football", "Global", "http://stream.tvgarden.world/live/ucl_live.m3u8"),
            IptvChannel("yt_5", "F1 Race Weekend Paddock Live", "Motorsports", "Global", "http://stream.tvgarden.world/live/f1_live.m3u8"),
            IptvChannel("yt_6", "MotoGP Qualifying & Race Live", "Motorsports", "Global", "http://stream.tvgarden.world/live/motogp_live.m3u8"),
            IptvChannel("yt_7", "WWE SmackDown Live Stream & Reactions", "Combat", "USA", "http://stream.tvgarden.world/live/wwe_live.m3u8"),
            IptvChannel("yt_8", "UFC Fight Night Live Stream", "Combat", "USA", "http://stream.tvgarden.world/live/ufc_live.m3u8"),
            IptvChannel("yt_9", "NBA Tonight Game Day Live", "Basketball", "USA", "http://stream.tvgarden.world/live/nba_live.m3u8"),
            IptvChannel("yt_10", "Wimbledon & Grand Slam Tennis Live", "Tennis", "UK", "http://stream.tvgarden.world/live/tennis_live.m3u8")
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
            SuggestedPlaylist("Punjabi TV", "Music, news & entertainment from Punjab", URL_PUNJABI),
            SuggestedPlaylist("News Broadcasts", "24/7 world news networks", URL_NEWS),
            SuggestedPlaylist("Sports Live", "Motorsports, extreme sports & athletics", URL_SPORTS)
        )
    }
}
