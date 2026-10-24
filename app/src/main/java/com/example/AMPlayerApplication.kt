package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.MediaRepository
import com.example.data.subtitles.SubtitleRepository
import com.example.data.subtitles.SubtitleRepositoryImpl

interface AppContainer {
    val database: AppDatabase
    val mediaRepository: MediaRepository
    val subtitleRepository: SubtitleRepository
}

class DefaultAppContainer(private val application: Application) : AppContainer {
    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(application)
    }

    override val mediaRepository: MediaRepository by lazy {
        MediaRepository(application, database.mediaDao())
    }

    override val subtitleRepository: SubtitleRepository by lazy {
        SubtitleRepositoryImpl(application, database.subtitleDao())
    }
}

class AMPlayerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
