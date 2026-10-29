package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

data class AppSettingsState(
    val isDarkMode: Boolean = true,
    val accentColorHex: String = "#4D6BFE",
    val logoDarkModeOpacity: Float = 1.0f,
    val gestureControlsEnabled: Boolean = true,
    val onlineMusicEnabled: Boolean = false,
    val hwDecodingEnabled: Boolean = true,
    val swFallbackEnabled: Boolean = true,
    val colorMode: String = "Vivid",
    val audioBoostEnabled: Boolean = false,
    val showVideoThumbnails: Boolean = true,
    val playWhenLocked: Boolean = true,
    val autoResumeEnabled: Boolean = true,
    val autoScanStartup: Boolean = true,
    val equalizerPreset: String = "Balanced",
    val seekDurationSeconds: Int = 10,
    val playerSeekbarStyle: String = "Classic Dot",
    val mpvProfile: String = "High Quality (HW+ / Vulkan)",
    val subtitleEncoding: String = "UTF-8",
    val subtitleSize: String = "Medium",
    val subtitleFontSize: Int = 18,
    val subtitleTextColorHex: String = "#FFFFFF",
    val subtitleBackgroundColorHex: String = "#80000000",
    val subtitleProvidersEnabled: Boolean = true,
    val fileBrowserShowHidden: Boolean = false,
    val dynamicAppIconWithAccent: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettingsState> = _settings.asStateFlow()

    private fun loadSettings(): AppSettingsState {
        return AppSettingsState(
            isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true),
            accentColorHex = prefs.getString(KEY_ACCENT_COLOR, "#4D6BFE") ?: "#4D6BFE",
            logoDarkModeOpacity = prefs.getFloat(KEY_LOGO_OPACITY, 1.0f),
            gestureControlsEnabled = prefs.getBoolean(KEY_GESTURE_CONTROLS, true),
            onlineMusicEnabled = prefs.getBoolean(KEY_ONLINE_MUSIC, false),
            hwDecodingEnabled = prefs.getBoolean(KEY_HW_DECODING, true),
            swFallbackEnabled = prefs.getBoolean(KEY_SW_FALLBACK, true),
            colorMode = prefs.getString(KEY_COLOR_MODE, "Vivid") ?: "Vivid",
            audioBoostEnabled = prefs.getBoolean(KEY_AUDIO_BOOST, false),
            showVideoThumbnails = prefs.getBoolean(KEY_SHOW_THUMBNAILS, true),
            playWhenLocked = prefs.getBoolean(KEY_PLAY_LOCKED, true),
            autoResumeEnabled = prefs.getBoolean(KEY_AUTO_RESUME, true),
            autoScanStartup = prefs.getBoolean(KEY_AUTO_SCAN, true),
            equalizerPreset = prefs.getString(KEY_EQ_PRESET, "Balanced") ?: "Balanced",
            seekDurationSeconds = prefs.getInt(KEY_SEEK_DURATION, 10),
            playerSeekbarStyle = prefs.getString(KEY_SEEKBAR_STYLE, "Classic Dot") ?: "Classic Dot",
            mpvProfile = prefs.getString(KEY_MPV_PROFILE, "High Quality (HW+ / Vulkan)") ?: "High Quality (HW+ / Vulkan)",
            subtitleEncoding = prefs.getString(KEY_SUB_ENCODING, "UTF-8") ?: "UTF-8",
            subtitleSize = prefs.getString(KEY_SUB_SIZE, "Medium") ?: "Medium",
            subtitleFontSize = prefs.getInt(KEY_SUB_FONT_SIZE, 18),
            subtitleTextColorHex = prefs.getString(KEY_SUB_TEXT_COLOR, "#FFFFFF") ?: "#FFFFFF",
            subtitleBackgroundColorHex = prefs.getString(KEY_SUB_BG_COLOR, "#80000000") ?: "#80000000",
            subtitleProvidersEnabled = prefs.getBoolean(KEY_SUB_PROVIDERS, true),
            fileBrowserShowHidden = prefs.getBoolean(KEY_BROWSER_SHOW_HIDDEN, false),
            dynamicAppIconWithAccent = prefs.getBoolean(KEY_DYNAMIC_APP_ICON, true)
        )
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _settings.value = _settings.value.copy(isDarkMode = enabled)
    }

    fun setDynamicAppIconWithAccent(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_APP_ICON, enabled).apply()
        _settings.value = _settings.value.copy(dynamicAppIconWithAccent = enabled)
    }

    fun setAccentColor(colorHex: String) {
        prefs.edit().putString(KEY_ACCENT_COLOR, colorHex).apply()
        _settings.value = _settings.value.copy(accentColorHex = colorHex)
    }

    fun setLogoDarkModeOpacity(opacity: Float) {
        prefs.edit().putFloat(KEY_LOGO_OPACITY, opacity).apply()
        _settings.value = _settings.value.copy(logoDarkModeOpacity = opacity)
    }

    fun setGestureControls(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GESTURE_CONTROLS, enabled).apply()
        _settings.value = _settings.value.copy(gestureControlsEnabled = enabled)
    }

    fun setOnlineMusic(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ONLINE_MUSIC, enabled).apply()
        _settings.value = _settings.value.copy(onlineMusicEnabled = enabled)
    }

    fun setHwDecoding(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HW_DECODING, enabled).apply()
        _settings.value = _settings.value.copy(hwDecodingEnabled = enabled)
    }

    fun setSwFallback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SW_FALLBACK, enabled).apply()
        _settings.value = _settings.value.copy(swFallbackEnabled = enabled)
    }

    fun setColorMode(mode: String) {
        prefs.edit().putString(KEY_COLOR_MODE, mode).apply()
        _settings.value = _settings.value.copy(colorMode = mode)
    }

    fun setAudioBoost(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUDIO_BOOST, enabled).apply()
        _settings.value = _settings.value.copy(audioBoostEnabled = enabled)
    }

    fun setPlayWhenLocked(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PLAY_LOCKED, enabled).apply()
        _settings.value = _settings.value.copy(playWhenLocked = enabled)
    }

    fun setAutoResume(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RESUME, enabled).apply()
        _settings.value = _settings.value.copy(autoResumeEnabled = enabled)
    }

    fun setAutoScanStartup(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SCAN, enabled).apply()
        _settings.value = _settings.value.copy(autoScanStartup = enabled)
    }

    fun setEqualizerPreset(preset: String) {
        prefs.edit().putString(KEY_EQ_PRESET, preset).apply()
        _settings.value = _settings.value.copy(equalizerPreset = preset)
    }

    fun setSeekDuration(seconds: Int) {
        prefs.edit().putInt(KEY_SEEK_DURATION, seconds).apply()
        _settings.value = _settings.value.copy(seekDurationSeconds = seconds)
    }

    fun setPlayerSeekbarStyle(style: String) {
        prefs.edit().putString(KEY_SEEKBAR_STYLE, style).apply()
        _settings.value = _settings.value.copy(playerSeekbarStyle = style)
    }

    fun setMpvProfile(profile: String) {
        prefs.edit().putString(KEY_MPV_PROFILE, profile).apply()
        _settings.value = _settings.value.copy(mpvProfile = profile)
    }

    fun setSubtitleEncoding(encoding: String) {
        prefs.edit().putString(KEY_SUB_ENCODING, encoding).apply()
        _settings.value = _settings.value.copy(subtitleEncoding = encoding)
    }

    fun setSubtitleSize(size: String) {
        prefs.edit().putString(KEY_SUB_SIZE, size).apply()
        _settings.value = _settings.value.copy(subtitleSize = size)
    }

    fun setSubtitleFontSize(sizeSp: Int) {
        prefs.edit().putInt(KEY_SUB_FONT_SIZE, sizeSp).apply()
        _settings.value = _settings.value.copy(subtitleFontSize = sizeSp)
    }

    fun setSubtitleTextColor(colorHex: String) {
        prefs.edit().putString(KEY_SUB_TEXT_COLOR, colorHex).apply()
        _settings.value = _settings.value.copy(subtitleTextColorHex = colorHex)
    }

    fun setSubtitleBackgroundColor(colorHex: String) {
        prefs.edit().putString(KEY_SUB_BG_COLOR, colorHex).apply()
        _settings.value = _settings.value.copy(subtitleBackgroundColorHex = colorHex)
    }

    fun setSubtitleProviders(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SUB_PROVIDERS, enabled).apply()
        _settings.value = _settings.value.copy(subtitleProvidersEnabled = enabled)
    }

    fun setFileBrowserShowHidden(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BROWSER_SHOW_HIDDEN, enabled).apply()
        _settings.value = _settings.value.copy(fileBrowserShowHidden = enabled)
    }

    fun clearAppCache(): Long {
        var bytesCleared = 0L
        try {
            val cacheDir = context.cacheDir
            bytesCleared += deleteDirRecursive(cacheDir)
            val externalCache = context.externalCacheDir
            if (externalCache != null) {
                bytesCleared += deleteDirRecursive(externalCache)
            }
        } catch (_: Exception) {}
        return bytesCleared
    }

    private fun deleteDirRecursive(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var total = 0L
        dir.listFiles()?.forEach { file ->
            total += if (file.isDirectory) {
                deleteDirRecursive(file)
            } else {
                val len = file.length()
                if (file.delete()) len else 0L
            }
        }
        return total
    }

    fun exportSettingsJson(): String {
        val json = JSONObject()
        val s = _settings.value
        json.put("isDarkMode", s.isDarkMode)
        json.put("accentColorHex", s.accentColorHex)
        json.put("logoDarkModeOpacity", s.logoDarkModeOpacity.toDouble())
        json.put("gestureControlsEnabled", s.gestureControlsEnabled)
        json.put("onlineMusicEnabled", s.onlineMusicEnabled)
        json.put("hwDecodingEnabled", s.hwDecodingEnabled)
        json.put("swFallbackEnabled", s.swFallbackEnabled)
        json.put("colorMode", s.colorMode)
        json.put("seekDurationSeconds", s.seekDurationSeconds)
        json.put("playerSeekbarStyle", s.playerSeekbarStyle)
        json.put("mpvProfile", s.mpvProfile)
        json.put("subtitleFontSize", s.subtitleFontSize)
        json.put("subtitleTextColorHex", s.subtitleTextColorHex)
        json.put("subtitleBackgroundColorHex", s.subtitleBackgroundColorHex)
        json.put("fileBrowserShowHidden", s.fileBrowserShowHidden)
        return json.toString(2)
    }

    fun importSettingsJson(jsonStr: String): Boolean {
        return try {
            val json = JSONObject(jsonStr)
            if (json.has("accentColorHex")) setAccentColor(json.getString("accentColorHex"))
            if (json.has("isDarkMode")) setDarkMode(json.getBoolean("isDarkMode"))
            if (json.has("gestureControlsEnabled")) setGestureControls(json.getBoolean("gestureControlsEnabled"))
            if (json.has("onlineMusicEnabled")) setOnlineMusic(json.getBoolean("onlineMusicEnabled"))
            if (json.has("hwDecodingEnabled")) setHwDecoding(json.getBoolean("hwDecodingEnabled"))
            if (json.has("seekDurationSeconds")) setSeekDuration(json.getInt("seekDurationSeconds"))
            if (json.has("playerSeekbarStyle")) setPlayerSeekbarStyle(json.getString("playerSeekbarStyle"))
            if (json.has("mpvProfile")) setMpvProfile(json.getString("mpvProfile"))
            if (json.has("subtitleFontSize")) setSubtitleFontSize(json.getInt("subtitleFontSize"))
            if (json.has("subtitleTextColorHex")) setSubtitleTextColor(json.getString("subtitleTextColorHex"))
            if (json.has("fileBrowserShowHidden")) setFileBrowserShowHidden(json.getBoolean("fileBrowserShowHidden"))
            if (json.has("dynamicAppIconWithAccent")) setDynamicAppIconWithAccent(json.getBoolean("dynamicAppIconWithAccent"))
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val PREFS_NAME = "am_player_app_settings"
        private const val KEY_DARK_MODE = "pref_dark_mode"
        private const val KEY_ACCENT_COLOR = "pref_accent_color"
        private const val KEY_DYNAMIC_APP_ICON = "pref_dynamic_app_icon"
        private const val KEY_LOGO_OPACITY = "pref_logo_opacity"
        private const val KEY_GESTURE_CONTROLS = "pref_gesture_controls"
        private const val KEY_ONLINE_MUSIC = "pref_online_music"
        private const val KEY_HW_DECODING = "pref_hw_decoding"
        private const val KEY_SW_FALLBACK = "pref_sw_fallback"
        private const val KEY_COLOR_MODE = "pref_color_mode"
        private const val KEY_AUDIO_BOOST = "pref_audio_boost"
        private const val KEY_SHOW_THUMBNAILS = "pref_show_video_thumbnails"
        private const val KEY_PLAY_LOCKED = "pref_play_locked"
        private const val KEY_AUTO_RESUME = "pref_auto_resume"
        private const val KEY_AUTO_SCAN = "pref_auto_scan"
        private const val KEY_EQ_PRESET = "pref_eq_preset"
        private const val KEY_SEEK_DURATION = "pref_seek_duration"
        private const val KEY_SEEKBAR_STYLE = "pref_seekbar_style"
        private const val KEY_MPV_PROFILE = "pref_mpv_profile"
        private const val KEY_SUB_ENCODING = "pref_sub_encoding"
        private const val KEY_SUB_SIZE = "pref_sub_size"
        private const val KEY_SUB_FONT_SIZE = "pref_sub_font_size"
        private const val KEY_SUB_TEXT_COLOR = "pref_sub_text_color"
        private const val KEY_SUB_BG_COLOR = "pref_sub_bg_color"
        private const val KEY_SUB_PROVIDERS = "pref_sub_providers"
        private const val KEY_BROWSER_SHOW_HIDDEN = "pref_browser_show_hidden"
    }

    fun setShowVideoThumbnails(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_THUMBNAILS, enabled).apply()
        _settings.value = _settings.value.copy(showVideoThumbnails = enabled)
    }
}
