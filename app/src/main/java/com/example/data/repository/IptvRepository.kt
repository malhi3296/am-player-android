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

    companion object {
        const val DEFAULT_TV_M3U_URL = "https://raw.githubusercontent.com/malhi3296/TV-garden-M3u-playlists/refs/heads/main/tv_channels.m3u"
        const val DEFAULT_RADIO_M3U_URL = "https://raw.githubusercontent.com/malhi3296/TV-garden-M3u-playlists/refs/heads/main/tv_radio.m3u"
        const val UNIFORM_FOLDER_COLOR = "#4D6BFE"
    }

    private val _folders = MutableStateFlow<List<IptvPlaylistFolder>>(emptyList())
    val folders: StateFlow<List<IptvPlaylistFolder>> = _folders.asStateFlow()

    private val _channels = MutableStateFlow<List<IptvChannel>>(emptyList())
    val channels: StateFlow<List<IptvChannel>> = _channels.asStateFlow()

    init {
        loadDefaultPlaylists()
    }

    private fun loadDefaultPlaylists() {
        val initialFolders = mutableListOf<IptvPlaylistFolder>()

        // 1. Load TV channels from asset or initial default
        var tvChannels = emptyList<IptvChannel>()
        try {
            context?.assets?.open("default_tv_channels.m3u")?.bufferedReader()?.use { reader ->
                tvChannels = parseM3u(reader.readText(), "Live TV")
            }
        } catch (_: Exception) {}

        val tvFolder = IptvPlaylistFolder(
            id = "folder_tv_channels",
            name = "Live TV Channels",
            colorHex = UNIFORM_FOLDER_COLOR,
            iconName = "tv",
            channels = tvChannels
        )
        initialFolders.add(tvFolder)

        // 2. Load Radio channels from asset or initial default
        var radioChannels = emptyList<IptvChannel>()
        try {
            context?.assets?.open("default_radio_channels.m3u")?.bufferedReader()?.use { reader ->
                radioChannels = parseM3u(reader.readText(), "Radio")
            }
        } catch (_: Exception) {}

        val radioFolder = IptvPlaylistFolder(
            id = "folder_radio_channels",
            name = "Live Radio Channels",
            colorHex = UNIFORM_FOLDER_COLOR,
            iconName = "radio",
            channels = radioChannels
        )
        initialFolders.add(radioFolder)

        // 3. IPTV-Org World Index Folder
        val worldFolder = IptvPlaylistFolder(
            id = "folder_iptv_world",
            name = "IPTV-Org World Index",
            colorHex = UNIFORM_FOLDER_COLOR,
            iconName = "globe",
            channels = emptyList()
        )
        initialFolders.add(worldFolder)

        _folders.value = initialFolders
        _channels.value = initialFolders.flatMap { it.channels }

        // Fetch fresh channels from GitHub in background
        CoroutineScope(Dispatchers.IO).launch {
            refreshRemotePlaylists()
        }
    }

    private suspend fun refreshRemotePlaylists() {
        try {
            // Fetch World Index
            val worldReq = Request.Builder().url("https://iptv-org.github.io/iptv/index.m3u").build()
            val worldResp = httpClient.newCall(worldReq).execute()
            if (worldResp.isSuccessful && worldResp.body != null) {
                val worldParsed = parseM3u(worldResp.body!!.string(), "World Index")
                if (worldParsed.isNotEmpty()) {
                    updateFolderChannels("folder_iptv_world", worldParsed.take(200))
                }
            }
        } catch (_: Exception) {}
        try {
            // Refresh TV
            val tvRequest = Request.Builder().url(DEFAULT_TV_M3U_URL).build()
            val tvResponse = httpClient.newCall(tvRequest).execute()
            if (tvResponse.isSuccessful && tvResponse.body != null) {
                val tvContent = tvResponse.body!!.string()
                val parsedTv = parseM3u(tvContent, "Live TV")
                if (parsedTv.isNotEmpty()) {
                    updateFolderChannels("folder_tv_channels", parsedTv)
                }
            }
        } catch (_: Exception) {}

        try {
            // Refresh Radio
            val radioRequest = Request.Builder().url(DEFAULT_RADIO_M3U_URL).build()
            val radioResponse = httpClient.newCall(radioRequest).execute()
            if (radioResponse.isSuccessful && radioResponse.body != null) {
                val radioContent = radioResponse.body!!.string()
                val parsedRadio = parseM3u(radioContent, "Radio")
                if (parsedRadio.isNotEmpty()) {
                    updateFolderChannels("folder_radio_channels", parsedRadio)
                }
            }
        } catch (_: Exception) {}
    }

    private fun updateFolderChannels(folderId: String, newChannels: List<IptvChannel>) {
        _folders.value = _folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(channels = newChannels)
            } else folder
        }
        _channels.value = _folders.value.flatMap { it.channels }
    }

    fun getSuggestedOnlinePlaylists(): List<SuggestedPlaylist> {
        return listOf(
            SuggestedPlaylist(
                title = "IPTV-Org World Index",
                description = "Thousands of public broadcast channels categorized globally",
                url = "https://iptv-org.github.io/iptv/index.m3u",
                isRadio = false
            ),
            SuggestedPlaylist(
                title = "TV Garden Channels (Official)",
                description = "Curated worldwide news, entertainment & live television",
                url = DEFAULT_TV_M3U_URL,
                isRadio = false
            ),
            SuggestedPlaylist(
                title = "TV Garden Radio (Official)",
                description = "Global FM, talk, cultural & public radio broadcasts",
                url = DEFAULT_RADIO_M3U_URL,
                isRadio = true
            ),
            SuggestedPlaylist(
                title = "YuppTV & Desi Channels",
                description = "South Asian public entertainment, regional news & broadcasts",
                url = "https://iptv-org.github.io/iptv/languages/hin.m3u",
                isRadio = false
            ),
            SuggestedPlaylist(
                title = "PiShow Global Entertainment",
                description = "Free international comedy, drama, and lifestyle channels",
                url = "https://iptv-org.github.io/iptv/categories/entertainment.m3u",
                isRadio = false
            ),
            SuggestedPlaylist(
                title = "RunnTV Movies & Series",
                description = "Continuous live classic movies, retro cinema, and shows",
                url = "https://iptv-org.github.io/iptv/categories/movies.m3u",
                isRadio = false
            ),
            SuggestedPlaylist(
                title = "Samsung TV Plus Free Streams",
                description = "Curated television networks from Samsung TV Plus",
                url = "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/us_samsung.m3u",
                isRadio = false
            ),
            SuggestedPlaylist(
                title = "Pluto TV Broadcasts",
                description = "Live themed 24/7 networks & public streaming channels",
                url = "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/us_pluto.m3u",
                isRadio = false
            ),
            SuggestedPlaylist(
                title = "TV Garden Radio (Official)",
                description = "Global FM, talk, cultural & public radio broadcasts",
                url = DEFAULT_RADIO_M3U_URL,
                isRadio = true
            ),
            SuggestedPlaylist(
                title = "IPTV-Org Free Global News",
                description = "Public news channels worldwide in English and international languages",
                url = "https://iptv-org.github.io/iptv/categories/news.m3u",
                isRadio = false
            ),
            SuggestedPlaylist(
                title = "IPTV-Org Classical & Music TV",
                description = "Non-stop music video broadcasts and concert streams",
                url = "https://iptv-org.github.io/iptv/categories/music.m3u",
                isRadio = false
            ),
            SuggestedPlaylist(
                title = "Free Sports & Outdoor TV",
                description = "Motorsports, extreme adventures, outdoor & fitness broadcasts",
                url = "https://iptv-org.github.io/iptv/categories/sports.m3u",
                isRadio = false
            )
        )
    }

    fun updateFolder(folderId: String, newName: String, newColorHex: String, newIconName: String) {
        _folders.value = _folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(
                    name = newName.ifBlank { folder.name },
                    colorHex = UNIFORM_FOLDER_COLOR,
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

            // Each M3U playlist gets its own dedicated folder
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

    private fun parseM3u(content: String, defaultCategory: String): List<IptvChannel> {
        val lines = content.lines()
        val list = mutableListOf<IptvChannel>()
        var currentName = ""
        var currentCategory = defaultCategory
        var currentCountry = "Global"

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                val groupMatch = Regex("group-title=\"([^\"]+)\"").find(trimmed)
                if (groupMatch != null) {
                    currentCategory = groupMatch.groupValues[1]
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
