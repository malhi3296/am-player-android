package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.example.data.local.MediaDao
import com.example.data.model.MediaItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(
    private val context: Context,
    private val mediaDao: MediaDao
) {
    val allMedia: Flow<List<MediaItemEntity>> = mediaDao.getAllMedia()
    val recentMedia: Flow<List<MediaItemEntity>> = mediaDao.getRecentMedia()

    suspend fun getMediaById(id: String): MediaItemEntity? = withContext(Dispatchers.IO) {
        mediaDao.getMediaById(id)
    }

    suspend fun savePlaybackPosition(id: String, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        val completed = durationMs > 0 && positionMs >= (durationMs - 5000L)
        mediaDao.updatePlaybackPosition(id, positionMs, completed, System.currentTimeMillis())
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        mediaDao.toggleFavorite(id, isFavorite)
    }

    suspend fun addMediaItem(item: MediaItemEntity) = withContext(Dispatchers.IO) {
        mediaDao.insertOrUpdate(item)
    }

    suspend fun rescanLocalMedia(): List<MediaItemEntity> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<MediaItemEntity>()

        // 1. Scan device MediaStore
        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DATA
            )

            val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            val queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            context.contentResolver.query(queryUri, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val contentUri = Uri.withAppendedPath(queryUri, mediaId.toString()).toString()
                    val title = cursor.getString(nameCol) ?: cursor.getString(titleCol) ?: "Video $mediaId"
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val resolution = if (width > 0 && height > 0) "${width}x${height}" else ""
                    val path = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val folderName = path?.let { File(it).parentFile?.name } ?: "Videos"

                    discovered.add(
                        MediaItemEntity(
                            id = contentUri,
                            uri = contentUri,
                            title = title,
                            durationMs = duration,
                            sizeBytes = size,
                            resolution = resolution,
                            folderName = folderName
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 2. Add high quality demo clips to ensure immediate playback capability out of the box
        val demoItems = getDemoVideoClips()
        for (demo in demoItems) {
            if (discovered.none { it.id == demo.id }) {
                discovered.add(demo)
            }
        }

        mediaDao.insertAll(discovered)
        discovered
    }

    suspend fun resolveMediaFromUri(uri: Uri): MediaItemEntity = withContext(Dispatchers.IO) {
        var title = "Imported Video"
        var size = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) title = cursor.getString(nameIndex) ?: title
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {}

        val item = MediaItemEntity(
            id = uri.toString(),
            uri = uri.toString(),
            title = title,
            sizeBytes = size,
            folderName = "Imported"
        )
        mediaDao.insertOrUpdate(item)
        item
    }

    fun getDemoVideoClips(): List<MediaItemEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            MediaItemEntity(
                id = "demo_flex_1",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                title = "Flex X Cop S02E01.1080p.WEBRip",
                durationMs = 3600000L,
                sizeBytes = 1450000000L,
                resolution = "1920x1080",
                folderName = "Flex X Cop Season 2",
                lastPlayedTimestamp = now - 60000L,
                playbackPositionMs = 245000L // 4m 05s resume position
            ),
            MediaItemEntity(
                id = "demo_flex_2",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                title = "Flex X Cop S02E02.1080p.WEBRip",
                durationMs = 3720000L,
                sizeBytes = 1520000000L,
                resolution = "1920x1080",
                folderName = "Flex X Cop Season 2",
                lastPlayedTimestamp = now - 120000L
            ),
            MediaItemEntity(
                id = "demo_killers_1",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                title = "A Shop for Killers S01E01.720p",
                durationMs = 3120000L,
                sizeBytes = 950000000L,
                resolution = "1280x720",
                folderName = "A Shop for Killers",
                lastPlayedTimestamp = now - 3600000L
            ),
            MediaItemEntity(
                id = "demo_killers_2",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                title = "A Shop for Killers S01E02.720p",
                durationMs = 3280000L,
                sizeBytes = 980000000L,
                resolution = "1280x720",
                folderName = "A Shop for Killers",
                lastPlayedTimestamp = now - 7200000L
            ),
            MediaItemEntity(
                id = "demo_levelup_1",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                title = "Level Up S01 WEB-DL 720p Episode 1",
                durationMs = 1480000L,
                sizeBytes = 420000000L,
                resolution = "1280x720",
                folderName = "Level Up S01",
                lastPlayedTimestamp = now - 86400000L
            ),
            MediaItemEntity(
                id = "demo_whatsapp_1",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                title = "VID-20241020-WA0014",
                durationMs = 45000L,
                sizeBytes = 18000000L,
                resolution = "1080x1920",
                folderName = "WhatsApp Video",
                lastPlayedTimestamp = now - 1800000L
            ),
            MediaItemEntity(
                id = "demo_whatsapp_2",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                title = "VID-20241021-WA0022",
                durationMs = 62000L,
                sizeBytes = 22000000L,
                resolution = "1080x1920",
                folderName = "WhatsApp Video",
                lastPlayedTimestamp = now - 2400000L
            ),
            MediaItemEntity(
                id = "demo_sadness_1",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                title = "Love.In.Sadness.S01E08.720p.HDTV",
                durationMs = 3400000L,
                sizeBytes = 870000000L,
                resolution = "1280x720",
                folderName = "Love.In.Sadness",
                lastPlayedTimestamp = now - 172800000L
            ),
            MediaItemEntity(
                id = "demo_big_buck_bunny",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                title = "Big Buck Bunny (4K UltraHD Master)",
                durationMs = 596000L,
                sizeBytes = 158000000L,
                resolution = "3840x2160",
                folderName = "AM Showcase Clips",
                lastPlayedTimestamp = now - 259200000L
            )
        )
    }
}
