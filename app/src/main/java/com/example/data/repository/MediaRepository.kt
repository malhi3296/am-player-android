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
    private val prefs = context.getSharedPreferences("am_player_media_prefs", Context.MODE_PRIVATE)

    val allMedia: Flow<List<MediaItemEntity>> = mediaDao.getAllMedia()
    val recentMedia: Flow<List<MediaItemEntity>> = mediaDao.getRecentMedia()

    fun getLastPlayedVideoId(): String? {
        return prefs.getString("last_played_video_id", null)
    }

    fun setLastPlayedVideoId(id: String) {
        prefs.edit().putString("last_played_video_id", id).apply()
    }

    suspend fun recordPlaybackStart(id: String) = withContext(Dispatchers.IO) {
        setLastPlayedVideoId(id)
        val existing = mediaDao.getMediaById(id)
        if (existing != null) {
            mediaDao.updatePlaybackPosition(
                id = id,
                positionMs = existing.playbackPositionMs,
                completed = false,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    suspend fun getMediaById(id: String): MediaItemEntity? = withContext(Dispatchers.IO) {
        mediaDao.getMediaById(id)
    }

    suspend fun savePlaybackPosition(id: String, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        setLastPlayedVideoId(id)
        val completed = durationMs > 0 && positionMs >= (durationMs - 5000L)
        mediaDao.updatePlaybackPosition(id, positionMs, completed, System.currentTimeMillis())
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        mediaDao.toggleFavorite(id, isFavorite)
    }

    suspend fun addMediaItem(item: MediaItemEntity) = withContext(Dispatchers.IO) {
        mediaDao.insertOrUpdate(item)
    }

    suspend fun deleteMediaItems(ids: List<String>) = withContext(Dispatchers.IO) {
        mediaDao.deleteByIds(ids)
        // Also remove if last played
        if (getLastPlayedVideoId() in ids) {
            prefs.edit().remove("last_played_video_id").apply()
        }
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

        // Purge any legacy demo items from previous builds
        mediaDao.purgeDemoItems()

        if (discovered.isNotEmpty()) {
            mediaDao.insertAll(discovered)
        }
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
}
