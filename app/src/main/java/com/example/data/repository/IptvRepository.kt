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
    val group: String = "Genre",
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
        const val URL_PUNJABI = "https://iptv-org.github.io/iptv/languages/pan.m3u"
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
        loadRadioStations()
        loadCuratedPlaylists()
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

        // 4. PUNJABI (User Requirement 11)
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

        val customFolders = loadCustomFoldersFromDisk()
        _folders.value = initialFolders + customFolders
        _channels.value = (initialFolders + customFolders).flatMap { it.channels }
        rebuildFavoritesFolder()

        // Fetch remote playlists in background
        CoroutineScope(Dispatchers.IO).launch {
            refreshRemoteCuratedPlaylists()
        }
    }

    private fun saveCustomFoldersToDisk() {
        val ctx = context ?: return
        try {
            val customFolders = _folders.value.filter { it.isCustom }
            val array = org.json.JSONArray()
            for (folder in customFolders) {
                val fObj = org.json.JSONObject().apply {
                    put("id", folder.id)
                    put("name", folder.name)
                    put("colorHex", folder.colorHex)
                    put("iconName", folder.iconName)
                    val cArray = org.json.JSONArray()
                    for (ch in folder.channels) {
                        val cObj = org.json.JSONObject().apply {
                            put("id", ch.id)
                            put("name", ch.name)
                            put("category", ch.category)
                            put("streamUrl", ch.streamUrl)
                            put("logoUrl", ch.logoUrl)
                            put("country", ch.country)
                            put("isCustom", ch.isCustom)
                        }
                        cArray.put(cObj)
                    }
                    put("channels", cArray)
                }
                array.put(fObj)
            }
            val file = java.io.File(ctx.filesDir, "iptv_custom_folders.json")
            file.writeText(array.toString())
        } catch (_: Exception) {}
    }

    private fun loadCustomFoldersFromDisk(): List<IptvPlaylistFolder> {
        val ctx = context ?: return emptyList()
        val file = java.io.File(ctx.filesDir, "iptv_custom_folders.json")
        if (!file.exists()) return emptyList()
        val result = mutableListOf<IptvPlaylistFolder>()
        try {
            val array = org.json.JSONArray(file.readText())
            for (i in 0 until array.length()) {
                val fObj = array.getJSONObject(i)
                val cArray = fObj.optJSONArray("channels") ?: org.json.JSONArray()
                val chList = mutableListOf<IptvChannel>()
                for (j in 0 until cArray.length()) {
                    val cObj = cArray.getJSONObject(j)
                    chList.add(
                        IptvChannel(
                            id = cObj.getString("id"),
                            name = cObj.getString("name"),
                            category = cObj.optString("category", "Custom"),
                            streamUrl = cObj.getString("streamUrl"),
                            logoUrl = cObj.optString("logoUrl", ""),
                            country = cObj.optString("country", "Global"),
                            isCustom = cObj.optBoolean("isCustom", true)
                        )
                    )
                }
                result.add(
                    IptvPlaylistFolder(
                        id = fObj.getString("id"),
                        name = fObj.getString("name"),
                        colorHex = fObj.optString("colorHex", UNIFORM_FOLDER_COLOR),
                        iconName = fObj.optString("iconName", "tv"),
                        channels = chList,
                        isCustom = true
                    )
                )
            }
        } catch (_: Exception) {}
        return result
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
                        // User Requirement 10: Include all channels from index.m3u (no 150 limit)
                        updateFolderChannels(folderId, parsed)
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
        saveCustomFoldersToDisk()
    }

    fun deleteFolder(folderId: String) {
        if (folderId == "folder_fav") return // Pinned folder cannot be deleted
        _folders.value = _folders.value.filter { it.id != folderId }
        _channels.value = _folders.value.flatMap { it.channels }
        saveCustomFoldersToDisk()
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
        saveCustomFoldersToDisk()
    }

    fun deleteChannel(folderId: String, channelId: String) {
        _folders.value = _folders.value.map { folder ->
            folder.copy(channels = folder.channels.filter { it.id != channelId })
        }
        _channels.value = _folders.value.flatMap { it.channels }
        saveCustomFoldersToDisk()
    }

    suspend fun importM3uPlaylist(folderName: String, colorHex: String, iconName: String, playlistUrlOrContent: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val content = if (playlistUrlOrContent.startsWith("http://", ignoreCase = true) || playlistUrlOrContent.startsWith("https://", ignoreCase = true)) {
                val req = Request.Builder()
                    .url(playlistUrlOrContent.trim())
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; IPTV Pro v9.1.25) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "*/*")
                    .build()
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
                // If no M3U tags, check if it's a single direct stream URL
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
                    saveCustomFoldersToDisk()
                    return@withContext Result.success(1)
                }
                return@withContext Result.failure(Exception("No playable channels found in this playlist"))
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
            saveCustomFoldersToDisk()
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
                val groupMatch = Regex("""group-title="([^"]+)"""").find(trimmed)
                    ?: Regex("""group-title=([^, ]+)""").find(trimmed)
                if (groupMatch != null) {
                    currentCategory = groupMatch.groupValues[1]
                }
                val logoMatch = Regex("""tvg-logo="([^"]+)"""").find(trimmed)
                    ?: Regex("""tvg-logo=([^, ]+)""").find(trimmed)
                if (logoMatch != null) {
                    currentLogo = logoMatch.groupValues[1]
                }
                val nameMatch = Regex("""tvg-name="([^"]+)"""").find(trimmed)
                val commaIdx = trimmed.lastIndexOf(',')
                currentName = when {
                    commaIdx != -1 && commaIdx < trimmed.length - 1 -> trimmed.substring(commaIdx + 1).trim()
                    nameMatch != null -> nameMatch.groupValues[1].trim()
                    else -> "Channel ${list.size + 1}"
                }
            } else if (trimmed.startsWith("#EXTGRP:", ignoreCase = true)) {
                val grp = trimmed.substringAfter("#EXTGRP:").trim()
                if (grp.isNotBlank()) currentCategory = grp
            } else if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("rtmp://", ignoreCase = true) ||
                trimmed.startsWith("rtsp://", ignoreCase = true)
            ) {
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
            "Punjabi" -> listOf(
                IptvChannel(
                    id = "pan_1",
                    name = "PTC Punjabi Live",
                    category = "Punjabi",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/ptc-punjabi/master.m3u8"
                ),
                IptvChannel(
                    id = "pan_2",
                    name = "PTC News HD",
                    category = "Punjabi",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/ptc-news/master.m3u8"
                ),
                IptvChannel(
                    id = "pan_3",
                    name = "Chardikla Time TV",
                    category = "Punjabi",
                    country = "India",
                    streamUrl = "https://d2q8540dc9qy4m.cloudfront.net/live/master.m3u8"
                ),
                IptvChannel(
                    id = "pan_4",
                    name = "Prime Asia TV Punjabi",
                    category = "Punjabi",
                    country = "Canada",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/prime-asia/master.m3u8"
                ),
                IptvChannel(
                    id = "pan_5",
                    name = "Manoranjan Movies",
                    category = "Punjabi",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/manoranjan/master.m3u8"
                ),
                IptvChannel(
                    id = "pan_6",
                    name = "Living India News",
                    category = "Punjabi",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/living-india/master.m3u8"
                ),
                IptvChannel(
                    id = "pan_7",
                    name = "Fateh TV Gurbani",
                    category = "Punjabi",
                    country = "India",
                    streamUrl = "https://d35j504z0x2vu2.cloudfront.net/v1/manifest/0414e72f4963e77c126554f4938eb5b260d8f347/fateh-tv/master.m3u8"
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
        saveCustomFoldersToDisk()
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
        saveCustomFoldersToDisk()
    }

    fun getSuggestedOnlinePlaylists(): List<SuggestedPlaylist> {
        return listOf(
            // --- Grouped by Language (from iptv-org PLAYLISTS.md) ---
            SuggestedPlaylist("Punjabi", "All Punjabi entertainment, news & music channels", "https://iptv-org.github.io/iptv/languages/pan.m3u", group = "Language"),
            SuggestedPlaylist("Hindi", "Hindi national networks, movies & regional TV", "https://iptv-org.github.io/iptv/languages/hin.m3u", group = "Language"),
            SuggestedPlaylist("English", "Global English broadcasts, networks & shows", "https://iptv-org.github.io/iptv/languages/eng.m3u", group = "Language"),
            SuggestedPlaylist("Urdu", "Urdu news, drama series & entertainment", "https://iptv-org.github.io/iptv/languages/urd.m3u", group = "Language"),
            SuggestedPlaylist("Bengali", "Bengali regional broadcasts & news", "https://iptv-org.github.io/iptv/languages/ben.m3u", group = "Language"),
            SuggestedPlaylist("Tamil", "Tamil movies, serials & music streams", "https://iptv-org.github.io/iptv/languages/tam.m3u", group = "Language"),
            SuggestedPlaylist("Telugu", "Telugu entertainment, movies & live news", "https://iptv-org.github.io/iptv/languages/tel.m3u", group = "Language"),
            SuggestedPlaylist("Spanish", "Spanish TV from Spain & Latin America", "https://iptv-org.github.io/iptv/languages/spa.m3u", group = "Language"),
            SuggestedPlaylist("French", "French television channels & culture", "https://iptv-org.github.io/iptv/languages/fra.m3u", group = "Language"),
            SuggestedPlaylist("German", "German public & private broadcasters", "https://iptv-org.github.io/iptv/languages/deu.m3u", group = "Language"),
            SuggestedPlaylist("Arabic", "Arabic news, documentaries & entertainment", "https://iptv-org.github.io/iptv/languages/ara.m3u", group = "Language"),
            SuggestedPlaylist("Russian", "Russian television channels & movies", "https://iptv-org.github.io/iptv/languages/rus.m3u", group = "Language"),
            SuggestedPlaylist("Portuguese", "Portuguese channels from Brazil & Portugal", "https://iptv-org.github.io/iptv/languages/por.m3u", group = "Language"),
            SuggestedPlaylist("Turkish", "Turkish live news, dramas & music", "https://iptv-org.github.io/iptv/languages/tur.m3u", group = "Language"),
            SuggestedPlaylist("Italian", "Italian national & regional channels", "https://iptv-org.github.io/iptv/languages/ita.m3u", group = "Language"),
            SuggestedPlaylist("Japanese", "Japanese news, anime & variety TV", "https://iptv-org.github.io/iptv/languages/jpn.m3u", group = "Language"),
            SuggestedPlaylist("Korean", "Korean K-dramas, music & news", "https://iptv-org.github.io/iptv/languages/kor.m3u", group = "Language"),
            SuggestedPlaylist("Chinese", "Chinese CCTV, regional & variety broadcasts", "https://iptv-org.github.io/iptv/languages/zho.m3u", group = "Language"),

            // --- Grouped by Genre / Category (from iptv-org PLAYLISTS.md) ---
            SuggestedPlaylist("News Broadcasts", "24/7 world news networks (iptv-org)", "https://iptv-org.github.io/iptv/categories/news.m3u", group = "Genre"),
            SuggestedPlaylist("Sports Live", "Motorsports, extreme sports & athletics", "https://iptv-org.github.io/iptv/categories/sports.m3u", group = "Genre"),
            SuggestedPlaylist("Movies", "Feature films, blockbusters & classics", "https://iptv-org.github.io/iptv/categories/movies.m3u", group = "Genre"),
            SuggestedPlaylist("Music TV", "Music videos, concerts & live charts", "https://iptv-org.github.io/iptv/categories/music.m3u", group = "Genre"),
            SuggestedPlaylist("Entertainment", "General entertainment, shows & talk TV", "https://iptv-org.github.io/iptv/categories/entertainment.m3u", group = "Genre"),
            SuggestedPlaylist("Kids & Cartoons", "Children programming, animation & shows", "https://iptv-org.github.io/iptv/categories/kids.m3u", group = "Genre"),
            SuggestedPlaylist("Animation & Anime", "Animated features, series & anime", "https://iptv-org.github.io/iptv/categories/animation.m3u", group = "Genre"),
            SuggestedPlaylist("Documentary", "Science, history, nature & discoveries", "https://iptv-org.github.io/iptv/categories/documentary.m3u", group = "Genre"),
            SuggestedPlaylist("Comedy", "Comedy sketches, sitcoms & stand-up", "https://iptv-org.github.io/iptv/categories/comedy.m3u", group = "Genre"),
            SuggestedPlaylist("Series & Dramas", "Binge-worthy drama serials & episodes", "https://iptv-org.github.io/iptv/categories/series.m3u", group = "Genre"),
            SuggestedPlaylist("Education & Tech", "Educational lectures & learning streams", "https://iptv-org.github.io/iptv/categories/education.m3u", group = "Genre"),
            SuggestedPlaylist("Classic TV", "Golden era television & vintage shows", "https://iptv-org.github.io/iptv/categories/classic.m3u", group = "Genre"),
            SuggestedPlaylist("Cooking & Food", "Culinary shows, chefs & food recipes", "https://iptv-org.github.io/iptv/categories/cooking.m3u", group = "Genre"),
            SuggestedPlaylist("Auto & Racing", "Automotive reviews, motorsports & cars", "https://iptv-org.github.io/iptv/categories/auto.m3u", group = "Genre"),
            SuggestedPlaylist("Travel & Nature", "Travel guides, destinations & landscapes", "https://iptv-org.github.io/iptv/categories/travel.m3u", group = "Genre"),
            SuggestedPlaylist("Weather Live", "Live radar, satellite & weather forecasts", "https://iptv-org.github.io/iptv/categories/weather.m3u", group = "Genre"),

            // --- Grouped by Broadcast Area / Country (from iptv-org PLAYLISTS.md) ---
            SuggestedPlaylist("India Channels", "National & regional Hindi/Indian broadcasts", "https://iptv-org.github.io/iptv/countries/in.m3u", group = "Region"),
            SuggestedPlaylist("United States", "American national & local television", "https://iptv-org.github.io/iptv/countries/us.m3u", group = "Region"),
            SuggestedPlaylist("United Kingdom", "British television, news & entertainment", "https://iptv-org.github.io/iptv/countries/uk.m3u", group = "Region"),
            SuggestedPlaylist("Canada", "Canadian news, sports & cultural TV", "https://iptv-org.github.io/iptv/countries/ca.m3u", group = "Region"),
            SuggestedPlaylist("Australia", "Australian television, news & sports", "https://iptv-org.github.io/iptv/countries/au.m3u", group = "Region"),
            SuggestedPlaylist("Germany", "German national channels & state TV", "https://iptv-org.github.io/iptv/countries/de.m3u", group = "Region"),
            SuggestedPlaylist("France", "French networks & cultural broadcasting", "https://iptv-org.github.io/iptv/countries/fr.m3u", group = "Region"),
            SuggestedPlaylist("Spain", "Spanish national, regional & sports TV", "https://iptv-org.github.io/iptv/countries/es.m3u", group = "Region"),
            SuggestedPlaylist("Italy", "Italian public & private broadcast channels", "https://iptv-org.github.io/iptv/countries/it.m3u", group = "Region"),
            SuggestedPlaylist("Pakistan", "Pakistani news, dramas & cricket live", "https://iptv-org.github.io/iptv/countries/pk.m3u", group = "Region"),
            SuggestedPlaylist("United Arab Emirates", "UAE, Dubai & Gulf Arabic streams", "https://iptv-org.github.io/iptv/countries/ae.m3u", group = "Region"),
            SuggestedPlaylist("Brazil", "Brazilian TV, telenovelas & football", "https://iptv-org.github.io/iptv/countries/br.m3u", group = "Region"),
            SuggestedPlaylist("World Complete Index", "Master global television index (all channels)", URL_WORLD, group = "Region")
        )
    }
}
