package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subtitle_cache")
data class SubtitleCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val videoHash: String,
    val videoTitle: String,
    val provider: String,
    val language: String,
    val filePath: String,
    val encoding: String = "UTF-8",
    val delayMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
