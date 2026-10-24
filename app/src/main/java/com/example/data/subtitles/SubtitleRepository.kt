package com.example.data.subtitles

import com.example.data.model.SubtitleCacheEntity
import com.example.data.model.SubtitleCandidate
import kotlinx.coroutines.flow.Flow
import java.io.File

interface SubtitleRepository {
    /**
     * Search subtitles concurrently across multiple providers with ranking.
     */
    suspend fun searchSubtitles(
        videoHash: String,
        videoTitle: String,
        preferredLanguage: String = "en"
    ): List<SubtitleCandidate>

    /**
     * Downloads and caches the selected subtitle candidate to local disk.
     * Returns the cached local File.
     */
    suspend fun downloadAndCacheSubtitle(
        candidate: SubtitleCandidate,
        videoHash: String,
        videoTitle: String
    ): Result<File>

    /**
     * Check if local cached subtitles exist for this video hash.
     */
    suspend fun getCachedSubtitles(videoHash: String): List<SubtitleCacheEntity>

    /**
     * Apply sync offset to a subtitle file and save.
     */
    suspend fun applyDelay(
        sourceFile: File,
        videoHash: String,
        delayMs: Long
    ): File
}
