package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.IptvRepository
import com.example.data.repository.MediaRepository
import com.example.data.repository.MusicRepository
import com.example.data.subtitles.SubtitleRepository
import com.example.data.subtitles.SubtitleRepositoryImpl
import com.example.player.MusicPlayerManager

interface AppContainer {
    val database: AppDatabase
    val mediaRepository: MediaRepository
    val subtitleRepository: SubtitleRepository
    val musicRepository: MusicRepository
    val iptvRepository: IptvRepository
    val musicPlayerManager: MusicPlayerManager
    val vaultRepository: com.example.data.repository.VaultRepository
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

    override val musicRepository: MusicRepository by lazy {
        MusicRepository(application)
    }

    override val iptvRepository: IptvRepository by lazy {
        IptvRepository()
    }

    override val musicPlayerManager: MusicPlayerManager by lazy {
        MusicPlayerManager(application)
    }

    override val vaultRepository: com.example.data.repository.VaultRepository by lazy {
        com.example.data.repository.VaultRepository(application)
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
