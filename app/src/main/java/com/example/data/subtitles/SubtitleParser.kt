package com.example.data.subtitles

import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object SubtitleParser {

    /**
     * Auto-detect character encoding to prevent garbled text ("Mojibake").
     * Checks UTF-8 BOM, UTF-16 LE/BE BOM, and validates UTF-8 byte sequences.
     */
    fun detectEncoding(bytes: ByteArray): Charset {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return StandardCharsets.UTF_8
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return StandardCharsets.UTF_16LE
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return StandardCharsets.UTF_16BE
        }

        // Try decoding as UTF-8
        return try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
            decoder.decode(java.nio.ByteBuffer.wrap(bytes))
            StandardCharsets.UTF_8
        } catch (e: Exception) {
            // Fallback to ISO-8859-1 / Windows-1252
            try {
                Charset.forName("windows-1252")
            } catch (ex: Exception) {
                StandardCharsets.ISO_8859_1
            }
        }
    }

    /**
     * Shifts SRT subtitle timestamps by offsetMs and writes out a clean UTF-8 SRT file.
     */
    fun applyDelayToSrt(sourceFile: File, outputFile: File, offsetMs: Long) {
        val bytes = sourceFile.readBytes()
        val encoding = detectEncoding(bytes)
        val content = String(bytes, encoding)

        val pattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})[.,](\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2})[.,](\\d{3})")
        val matcher = pattern.matcher(content)
        val sb = StringBuffer()

        while (matcher.find()) {
            val startMs = parseTimeToMs(matcher.group(1)!!, matcher.group(2)!!, matcher.group(3)!!, matcher.group(4)!!)
            val endMs = parseTimeToMs(matcher.group(5)!!, matcher.group(6)!!, matcher.group(7)!!, matcher.group(8)!!)

            val newStartMs = maxOf(0L, startMs + offsetMs)
            val newEndMs = maxOf(0L, endMs + offsetMs)

            val replacement = "${formatMsToTime(newStartMs)} --> ${formatMsToTime(newEndMs)}"
            matcher.appendReplacement(sb, replacement)
        }
        matcher.appendTail(sb)

        outputFile.parentFile?.mkdirs()
        outputFile.writeText(sb.toString(), StandardCharsets.UTF_8)
    }

    private fun parseTimeToMs(h: String, m: String, s: String, ms: String): Long {
        return h.toLong() * 3600000L + m.toLong() * 60000L + s.toLong() * 1000L + ms.toLong()
    }

    private fun formatMsToTime(timeMs: Long): String {
        val h = timeMs / 3600000L
        val m = (timeMs % 3600000L) / 60000L
        val s = (timeMs % 60000L) / 1000L
        val ms = timeMs % 1000L
        return String.format("%02d:%02d:%02d,%03d", h, m, s, ms)
    }
}
