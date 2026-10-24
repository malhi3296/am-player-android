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

    suspend fun rescanAudio(): List<AudioTrack> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AudioTrack>()

        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE
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

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = Uri.withAppendedPath(queryUri, id.toString()).toString()
                    val title = cursor.getString(titleCol) ?: "Audio Track $id"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)

                    list.add(
                        AudioTrack(
                            id = contentUri,
                            uri = contentUri,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            sizeBytes = size
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // Add pre-configured high quality demo music streams
        val demos = getDemoTracks()
        for (demo in demos) {
            if (list.none { it.id == demo.id }) {
                list.add(demo)
            }
        }

        _tracks.value = list
        list
    }

    fun getDemoTracks(): List<AudioTrack> {
        return listOf(
            AudioTrack(
                id = "music_demo_synthwave",
                uri = "https://commondatastorage.googleapis.com/codeskulptor-demos/riceracer_soundtrack.mp3",
                title = "Neon Highway (Synthwave Drive)",
                artist = "AM Studio Synth",
                album = "Cyber Dreams Vol. 1",
                durationMs = 184000L,
                sizeBytes = 4200000L
            ),
            AudioTrack(
                id = "music_demo_ambient",
                uri = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.ogg",
                title = "Lepidoptera (Deep Chill Out)",
                artist = "Epoq Ambient",
                album = "Sonic Relax",
                durationMs = 215000L,
                sizeBytes = 3800000L
            ),
            AudioTrack(
                id = "music_demo_space",
                uri = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Flowers__short_edit.mp3",
                title = "Flowers (Microtonal Groove)",
                artist = "Sevish",
                album = "Golden Ratio",
                durationMs = 142000L,
                sizeBytes = 2900000L
            ),
            AudioTrack(
                id = "music_demo_piano",
                uri = "https://commondatastorage.googleapis.com/codeskulptor-assets/sounddogs/soundtrack.mp3",
                title = "Acoustic Reflection",
                artist = "Classical Piano Duo",
                album = "Quiet Horizons",
                durationMs = 120000L,
                sizeBytes = 2400000L
            )
        )
    }
}
