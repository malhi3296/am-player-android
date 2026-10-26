package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.MediaItemEntity
import com.example.data.subtitles.OpenSubtitlesHasher
import com.example.data.subtitles.SubtitleParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AM Player", appName)
    }

    @Test
    fun testOpenSubtitlesHasherFallback() {
        val hash = OpenSubtitlesHasher.generateFallbackHash("Tears of Steel")
        assertNotNull(hash)
        assertEquals(16, hash.length)
    }

    @Test
    fun testSubtitleEncodingDetection() {
        val utf8Bytes = "Hello Subtitles!".toByteArray(StandardCharsets.UTF_8)
        val detected = SubtitleParser.detectEncoding(utf8Bytes)
        assertEquals(StandardCharsets.UTF_8, detected)
    }

    @Test
    fun testSubtitleDelayShift() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testSrt = File(context.cacheDir, "test.srt")
        val srtContent = "1\n00:00:10,000 --> 00:00:15,000\nHello World\n"
        testSrt.writeText(srtContent, StandardCharsets.UTF_8)

        val delayedSrt = File(context.cacheDir, "delayed_test.srt")
        SubtitleParser.applyDelayToSrt(testSrt, delayedSrt, 2000L) // +2 seconds

        assertTrue(delayedSrt.exists())
        val resultText = delayedSrt.readText(StandardCharsets.UTF_8)
        assertTrue(resultText.contains("00:00:12,000 --> 00:00:17,000"))
    }

    @Test
    fun testRoomDatabasePersistence() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val testItem = MediaItemEntity(
            id = "test_media_1",
            uri = "content://media/test/1",
            title = "Test Video 1",
            durationMs = 60000L,
            playbackPositionMs = 15000L
        )
        db.mediaDao().insertOrUpdate(testItem)
        val retrieved = db.mediaDao().getMediaById("test_media_1")
        assertNotNull(retrieved)
        assertEquals("Test Video 1", retrieved?.title)
        assertEquals(15000L, retrieved?.playbackPositionMs)
    }

    @Test
    fun testVaultRepository() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vault = com.example.data.repository.VaultRepository(context)
        
        // Initial state setup with PIN
        vault.setupVault("1234")
        assertTrue(vault.isVaultConfigured.value)
        assertTrue(vault.verifyPin("1234"))
        org.junit.Assert.assertFalse(vault.verifyPin("9999"))

        // Test hiding and unhiding videos
        vault.hideVideo("secret_vid_1")
        assertTrue(vault.isVideoHidden("secret_vid_1"))
        assertTrue(vault.hiddenVideoIds.value.contains("secret_vid_1"))

        // Test vault folders
        vault.createVaultFolder("Private")
        assertTrue(vault.vaultFolders.value.contains("Private"))
        vault.setVideoVaultFolder("secret_vid_1", "Private")
        assertEquals("Private", vault.getVideoVaultFolder("secret_vid_1"))

        vault.unhideVideo("secret_vid_1")
        org.junit.Assert.assertFalse(vault.isVideoHidden("secret_vid_1"))
    }
}
