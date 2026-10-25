package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettingsState(
    val isDarkMode: Boolean = true,
    val hwDecodingEnabled: Boolean = true,
    val swFallbackEnabled: Boolean = true,
    val colorMode: String = "Vivid",
    val audioBoostEnabled: Boolean = true,
    val playWhenLocked: Boolean = true,
    val autoResumeEnabled: Boolean = true,
    val autoScanStartup: Boolean = true,
    val equalizerPreset: String = "Bass Boost",
    val seekDurationSeconds: Int = 10,
    val subtitleEncoding: String = "UTF-8",
    val subtitleSize: String = "Medium",
    val subtitleProvidersEnabled: Boolean = true
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettingsState> = _settings.asStateFlow()

    private fun loadSettings(): AppSettingsState {
        return AppSettingsState(
            isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true),
            hwDecodingEnabled = prefs.getBoolean(KEY_HW_DECODING, true),
            swFallbackEnabled = prefs.getBoolean(KEY_SW_FALLBACK, true),
            colorMode = prefs.getString(KEY_COLOR_MODE, "Vivid") ?: "Vivid",
            audioBoostEnabled = prefs.getBoolean(KEY_AUDIO_BOOST, true),
            playWhenLocked = prefs.getBoolean(KEY_PLAY_LOCKED, true),
            autoResumeEnabled = prefs.getBoolean(KEY_AUTO_RESUME, true),
            autoScanStartup = prefs.getBoolean(KEY_AUTO_SCAN, true),
            equalizerPreset = prefs.getString(KEY_EQ_PRESET, "Bass Boost") ?: "Bass Boost",
            seekDurationSeconds = prefs.getInt(KEY_SEEK_DURATION, 10),
            subtitleEncoding = prefs.getString(KEY_SUB_ENCODING, "UTF-8") ?: "UTF-8",
            subtitleSize = prefs.getString(KEY_SUB_SIZE, "Medium") ?: "Medium",
            subtitleProvidersEnabled = prefs.getBoolean(KEY_SUB_PROVIDERS, true)
        )
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _settings.value = _settings.value.copy(isDarkMode = enabled)
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

    fun setSubtitleEncoding(encoding: String) {
        prefs.edit().putString(KEY_SUB_ENCODING, encoding).apply()
        _settings.value = _settings.value.copy(subtitleEncoding = encoding)
    }

    fun setSubtitleSize(size: String) {
        prefs.edit().putString(KEY_SUB_SIZE, size).apply()
        _settings.value = _settings.value.copy(subtitleSize = size)
    }

    fun setSubtitleProviders(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SUB_PROVIDERS, enabled).apply()
        _settings.value = _settings.value.copy(subtitleProvidersEnabled = enabled)
    }

    companion object {
        private const val PREFS_NAME = "am_player_app_settings"
        private const val KEY_DARK_MODE = "pref_dark_mode"
        private const val KEY_HW_DECODING = "pref_hw_decoding"
        private const val KEY_SW_FALLBACK = "pref_sw_fallback"
        private const val KEY_COLOR_MODE = "pref_color_mode"
        private const val KEY_AUDIO_BOOST = "pref_audio_boost"
        private const val KEY_PLAY_LOCKED = "pref_play_locked"
        private const val KEY_AUTO_RESUME = "pref_auto_resume"
        private const val KEY_AUTO_SCAN = "pref_auto_scan"
        private const val KEY_EQ_PRESET = "pref_eq_preset"
        private const val KEY_SEEK_DURATION = "pref_seek_duration"
        private const val KEY_SUB_ENCODING = "pref_sub_encoding"
        private const val KEY_SUB_SIZE = "pref_sub_size"
        private const val KEY_SUB_PROVIDERS = "pref_sub_providers"
    }
}
