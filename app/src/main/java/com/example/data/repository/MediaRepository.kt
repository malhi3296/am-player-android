package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
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

    
    suspend fun renameMediaItem(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        val existing = mediaDao.getMediaById(id) ?: return@withContext
        mediaDao.insertOrUpdate(existing.copy(title = newTitle.trim()))
    }

    suspend fun hideMediaItem(item: MediaItemEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Move to app-private vault directory with .nomedia so external apps cannot scan it
            val vaultDir = File(context.filesDir, "vault_videos").apply {
                if (!exists()) mkdirs()
                val nomedia = File(this, ".nomedia")
                if (!nomedia.exists()) nomedia.createNewFile()
            }

            val cleanTitle = item.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(50)
            val destFile = File(vaultDir, "vault_${System.currentTimeMillis()}_$cleanTitle.mp4")

            val srcUri = Uri.parse(item.uri)
            var copied = false
            try {
                context.contentResolver.openInputStream(srcUri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                copied = destFile.exists() && destFile.length() > 0
            } catch (_: Exception) {}

            // Try direct file copy if srcUri was a file:// or direct path
            if (!copied && srcUri.path != null) {
                try {
                    val srcFile = File(srcUri.path!!)
                    if (srcFile.exists()) {
                        srcFile.inputStream().use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        copied = destFile.exists() && destFile.length() > 0
                    }
                } catch (_: Exception) {}
            }

            // 2. Delete original from MediaStore so gallery and other apps on phone immediately stop seeing it
            var physicalPath: String? = null
            try {
                if (srcUri.scheme == "content") {
                    val proj = arrayOf(MediaStore.Video.Media.DATA)
                    context.contentResolver.query(srcUri, proj, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                            if (idx != -1) physicalPath = cursor.getString(idx)
                        }
                    }
                    context.contentResolver.delete(srcUri, null, null)
                }
            } catch (_: Exception) {}

            // If physical file path is known, try deleting it directly as well
            if (physicalPath != null) {
                try {
                    val origFile = File(physicalPath!!)
                    if (origFile.exists()) origFile.delete()
                    MediaScannerConnection.scanFile(context, arrayOf(physicalPath!!), null, null)
                } catch (_: Exception) {}
            } else if (srcUri.scheme == "file" && srcUri.path != null) {
                try {
                    val origFile = File(srcUri.path!!)
                    if (origFile.exists()) origFile.delete()
                    MediaScannerConnection.scanFile(context, arrayOf(origFile.absolutePath), null, null)
                } catch (_: Exception) {}
            }

            // 3. Update Room entity with private URI so secret vault can play it directly
            if (copied) {
                val updated = item.copy(
                    uri = Uri.fromFile(destFile).toString(),
                    folderName = "Vault"
                )
                mediaDao.insertOrUpdate(updated)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unhideMediaItem(item: MediaItemEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val srcFile = if (item.uri.startsWith("file://")) {
                File(Uri.parse(item.uri).path ?: "")
            } else {
                File(item.uri)
            }
            if (!srcFile.exists()) return@withContext false

            // Restore file back to external Movies/AMPlayer directory
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val destDir = File(moviesDir, "AMPlayer").apply { if (!exists()) mkdirs() }
            val cleanTitle = item.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(50)
            val restoredFile = File(destDir, "$cleanTitle.mp4")

            srcFile.inputStream().use { input ->
                restoredFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Delete from private vault
            srcFile.delete()

            // Trigger MediaScanner so phone gallery sees the restored video
            MediaScannerConnection.scanFile(context, arrayOf(restoredFile.absolutePath), null, null)

            val restoredUri = Uri.fromFile(restoredFile).toString()
            val updated = item.copy(
                uri = restoredUri,
                folderName = "AMPlayer"
            )
            mediaDao.insertOrUpdate(updated)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteSingleMedia(item: MediaItemEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(Uri.parse(item.uri), null, null)
        } catch (_: Exception) {}
        mediaDao.deleteByIds(listOf(item.id))
        true
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

        // Reconcile and prune items from DB that were deleted externally from Gallery/storage
        try {
            val currentInDb = mediaDao.getAllMediaSync()
            val discoveredIds = discovered.map { it.id }.toSet()
            val vaultPrefs = context.getSharedPreferences("am_player_secret_vault", Context.MODE_PRIVATE)
            val hiddenIds = vaultPrefs.getStringSet("vault_hidden_ids", emptySet()) ?: emptySet()

            val missingIds = currentInDb.filter { item ->
                // Never prune items that are protected in Secret Vault
                if (hiddenIds.contains(item.id)) return@filter false

                if (item.uri.startsWith("content://")) {
                    item.id !in discoveredIds
                } else if (item.uri.startsWith("file://")) {
                    !File(Uri.parse(item.uri).path ?: "").exists()
                } else false
            }.map { it.id }

            if (missingIds.isNotEmpty()) {
                mediaDao.deleteByIds(missingIds)
            }
        } catch (_: Exception) {}

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
