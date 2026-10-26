package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.data.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    private val _tracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val tracks: StateFlow<List<AudioTrack>> = _tracks.asStateFlow()

    private val _isOpenSourcePermitted = MutableStateFlow(false)
    val isOpenSourcePermitted: StateFlow<Boolean> = _isOpenSourcePermitted.asStateFlow()

    suspend fun setOpenSourceStreamingPermitted(permitted: Boolean) = withContext(Dispatchers.IO) {
        _isOpenSourcePermitted.value = permitted
        rescanAudio()
    }

    suspend fun rescanAudio(): List<AudioTrack> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AudioTrack>()

        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATA
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
            val queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            context.contentResolver.query(queryUri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = Uri.withAppendedPath(queryUri, id.toString()).toString()
                    val title = cursor.getString(titleCol) ?: "Audio Track $id"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val path = if (dataCol != -1) cursor.getString(dataCol) else null
                    val folder = path?.let { java.io.File(it).parentFile?.name } ?: "Music"

                    list.add(
                        AudioTrack(
                            id = contentUri,
                            uri = contentUri,
                            title = title,
                            artist = artist,
                            album = album,
                            folderName = folder,
                            durationMs = duration,
                            sizeBytes = size
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // If online streaming is explicitly enabled by user in settings, allow streaming tracks
        if (_isOpenSourcePermitted.value) {
            val customStreams = getCustomStreamTracks()
            for (cs in customStreams) {
                if (list.none { it.id == cs.id }) {
                    list.add(cs)
                }
            }
        }

        _tracks.value = list
        list
    }

    private val customStreamUrls = mutableListOf<AudioTrack>()

    fun addOnlineStream(title: String, artist: String, streamUrl: String) {
        val newTrack = AudioTrack(
            id = "stream_${System.currentTimeMillis()}",
            uri = streamUrl.trim(),
            title = title.ifBlank { "Online Stream" },
            artist = artist.ifBlank { "Streaming Radio / Music" },
            album = "Online Streams",
            durationMs = 0L,
            sizeBytes = 0L
        )
        customStreamUrls.add(newTrack)
        _tracks.value = _tracks.value + newTrack
    }

    fun addCustomStream(title: String, url: String, artist: String = "Online Radio") {
        addOnlineStream(title, artist, url)
    }

    private fun getCustomStreamTracks(): List<AudioTrack> = customStreamUrls.toList()

    fun getOpenSourceLibraryTracks(): List<AudioTrack> {
        return listOf(
            AudioTrack(
                id = "os_beethoven_moonlight",
                uri = "https://upload.wikimedia.org/wikipedia/commons/e/eb/Beethoven_Moonlight_1st_movement.ogg",
                title = "Moonlight Sonata (Adagio sostenuto)",
                artist = "Ludwig van Beethoven",
                album = "Open Source Classical (Musopen)",
                durationMs = 360000L,
                sizeBytes = 5400000L
            ),
            AudioTrack(
                id = "os_vivaldi_spring",
                uri = "https://upload.wikimedia.org/wikipedia/commons/3/3c/Vivaldi_Spring_mvt_1_Allegro_-_John_Harrison_violin.ogg",
                title = "The Four Seasons: Spring (Allegro)",
                artist = "Antonio Vivaldi (John Harrison)",
                album = "Creative Commons Masterpieces",
                durationMs = 216000L,
                sizeBytes = 4100000L
            ),
            AudioTrack(
                id = "os_chopin_nocturne",
                uri = "https://upload.wikimedia.org/wikipedia/commons/9/90/Chopin_Nocturne_Op9_No2.ogg",
                title = "Nocturne in E-flat major, Op. 9 No. 2",
                artist = "Frédéric Chopin",
                album = "Open Piano Archive",
                durationMs = 265000L,
                sizeBytes = 4800000L
            ),
            AudioTrack(
                id = "os_mozart_nachtmusik",
                uri = "https://upload.wikimedia.org/wikipedia/commons/b/b2/Mozart_-_Serenade_No._13_in_G_Major%2C_K._525_-_I._Allegro.ogg",
                title = "Eine kleine Nachtmusik (I. Allegro)",
                artist = "Wolfgang Amadeus Mozart",
                album = "European Open Archives",
                durationMs = 345000L,
                sizeBytes = 5900000L
            ),
            AudioTrack(
                id = "os_bach_air",
                uri = "https://upload.wikimedia.org/wikipedia/commons/3/30/Bach_-_Air_on_the_G_String.ogg",
                title = "Air on the G String (BWV 1068)",
                artist = "Johann Sebastian Bach",
                album = "Baroque Treasures Open Source",
                durationMs = 290000L,
                sizeBytes = 4600000L
            )
        )
    }
}
