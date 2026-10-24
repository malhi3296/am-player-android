package com.example.data.subtitles

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

object OpenSubtitlesHasher {

    private const val HASH_CHUNK_SIZE = 65536L // 64k

    /**
     * Computes the OpenSubtitles 64-bit hash for a given file.
     * The algorithm computes a 64-bit checksum of the first 64KB and last 64KB of the file + file size.
     */
    fun computeHash(file: File): String {
        val size = file.length()
        if (size < HASH_CHUNK_SIZE) {
            return String.format("%016x", size)
        }

        var checksum = size
        FileInputStream(file).use { stream ->
            val channel = stream.channel
            checksum = computeChecksumForChannel(channel, 0L, HASH_CHUNK_SIZE, checksum)
            checksum = computeChecksumForChannel(channel, maxOf(0L, size - HASH_CHUNK_SIZE), HASH_CHUNK_SIZE, checksum)
        }

        return String.format("%016x", checksum)
    }

    /**
     * Compute hash from Android content Uri using ContentResolver if file is seekable
     */
    fun computeHashFromUri(context: Context, uri: Uri, fallbackName: String): String {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val size = pfd.statSize
                if (size > 0 && size >= HASH_CHUNK_SIZE) {
                    FileInputStream(pfd.fileDescriptor).use { stream ->
                        val channel = stream.channel
                        var checksum = size
                        checksum = computeChecksumForChannel(channel, 0L, HASH_CHUNK_SIZE, checksum)
                        checksum = computeChecksumForChannel(channel, maxOf(0L, size - HASH_CHUNK_SIZE), HASH_CHUNK_SIZE, checksum)
                        return String.format("%016x", checksum)
                    }
                }
            }
            // Fallback pseudo-hash based on title and length
            generateFallbackHash(fallbackName)
        } catch (e: Exception) {
            generateFallbackHash(fallbackName)
        }
    }

    fun generateFallbackHash(title: String): String {
        val cleaned = title.lowercase().trim()
        var hash = 0xCBF29CE484222325UL.toLong()
        for (c in cleaned) {
            hash = hash xor c.code.toLong()
            hash *= 0x100000001B3L
        }
        return String.format("%016x", hash)
    }

    private fun computeChecksumForChannel(
        channel: FileChannel,
        position: Long,
        length: Long,
        initialChecksum: Long
    ): Long {
        val buffer = ByteBuffer.allocateDirect(length.toInt()).order(ByteOrder.LITTLE_ENDIAN)
        channel.position(position)
        channel.read(buffer)
        buffer.flip()

        val longBuffer = buffer.asLongBuffer()
        var checksum = initialChecksum
        while (longBuffer.hasRemaining()) {
            checksum += longBuffer.get()
        }
        return checksum
    }
}
