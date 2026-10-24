package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val title: String,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val resolution: String = "",
    val folderName: String = "Internal",
    val lastPlayedTimestamp: Long = 0L,
    val playbackPositionMs: Long = 0L,
    val completed: Boolean = false,
    val isFavorite: Boolean = false,
    val audioTrackIndex: Int = -1,
    val subtitleTrackIndex: Int = -1,
    val speed: Float = 1.0f
)
