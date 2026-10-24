package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SubtitleCacheEntity

@Dao
interface SubtitleDao {
    @Query("SELECT * FROM subtitle_cache WHERE videoHash = :hash ORDER BY createdAt DESC")
    suspend fun getCachedSubtitlesForHash(hash: String): List<SubtitleCacheEntity>

    @Query("SELECT * FROM subtitle_cache WHERE videoTitle LIKE '%' || :title || '%' ORDER BY createdAt DESC")
    suspend fun getCachedSubtitlesForTitle(title: String): List<SubtitleCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtitle(entity: SubtitleCacheEntity): Long

    @Query("UPDATE subtitle_cache SET delayMs = :delayMs WHERE id = :id")
    suspend fun updateSubtitleDelay(id: Long, delayMs: Long)
}
