package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VaultRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isVaultConfigured = MutableStateFlow(prefs.getString(KEY_PIN, null) != null)
    val isVaultConfigured: StateFlow<Boolean> = _isVaultConfigured.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _hiddenVideoIds = MutableStateFlow(
        prefs.getStringSet(KEY_HIDDEN_IDS, emptySet())?.toSet() ?: emptySet()
    )
    val hiddenVideoIds: StateFlow<Set<String>> = _hiddenVideoIds.asStateFlow()

    fun setupVault(pin: String, enableBiometric: Boolean) {
        prefs.edit()
            .putString(KEY_PIN, pin)
            .putBoolean(KEY_BIOMETRIC, enableBiometric)
            .apply()
        _isVaultConfigured.value = true
        _isBiometricEnabled.value = enableBiometric
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN, null) ?: return false
        return stored == pin
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun hideVideo(id: String) {
        val current = _hiddenVideoIds.value.toMutableSet()
        current.add(id)
        prefs.edit().putStringSet(KEY_HIDDEN_IDS, current).apply()
        _hiddenVideoIds.value = current
    }

    fun unhideVideo(id: String) {
        val current = _hiddenVideoIds.value.toMutableSet()
        current.remove(id)
        prefs.edit().putStringSet(KEY_HIDDEN_IDS, current).apply()
        _hiddenVideoIds.value = current
    }

    fun isVideoHidden(id: String): Boolean {
        return _hiddenVideoIds.value.contains(id)
    }

    companion object {
        private const val PREFS_NAME = "am_player_secret_vault"
        private const val KEY_PIN = "vault_pin"
        private const val KEY_BIOMETRIC = "vault_biometric_enabled"
        private const val KEY_HIDDEN_IDS = "vault_hidden_ids"
    }
}
