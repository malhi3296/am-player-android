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

    private val _hiddenVideoIds = MutableStateFlow(
        prefs.getStringSet(KEY_HIDDEN_IDS, emptySet())?.toSet() ?: emptySet()
    )
    val hiddenVideoIds: StateFlow<Set<String>> = _hiddenVideoIds.asStateFlow()

    private val _vaultFolders = MutableStateFlow(
        prefs.getStringSet(KEY_VAULT_FOLDERS, setOf("General"))?.toSet() ?: setOf("General")
    )
    val vaultFolders: StateFlow<Set<String>> = _vaultFolders.asStateFlow()

    fun setupVault(pin: String) {
        prefs.edit()
            .putString(KEY_PIN, pin)
            .apply()
        _isVaultConfigured.value = true
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN, null) ?: return false
        return stored == pin
    }

    fun hideVideo(id: String, folderName: String = "General") {
        val current = _hiddenVideoIds.value.toMutableSet()
        current.add(id)
        prefs.edit()
            .putStringSet(KEY_HIDDEN_IDS, current)
            .putString("vault_folder_$id", folderName)
            .apply()
        _hiddenVideoIds.value = current
    }

    fun unhideVideo(id: String) {
        val current = _hiddenVideoIds.value.toMutableSet()
        current.remove(id)
        prefs.edit()
            .putStringSet(KEY_HIDDEN_IDS, current)
            .remove("vault_folder_$id")
            .apply()
        _hiddenVideoIds.value = current
    }

    fun isVideoHidden(id: String): Boolean {
        return _hiddenVideoIds.value.contains(id)
    }

    fun getVideoVaultFolder(id: String): String {
        return prefs.getString("vault_folder_$id", "General") ?: "General"
    }

    fun setVideoVaultFolder(id: String, folderName: String) {
        prefs.edit().putString("vault_folder_$id", folderName).apply()
    }

    fun createVaultFolder(folderName: String) {
        val trimmed = folderName.trim()
        if (trimmed.isEmpty()) return
        val current = _vaultFolders.value.toMutableSet()
        current.add(trimmed)
        prefs.edit().putStringSet(KEY_VAULT_FOLDERS, current).apply()
        _vaultFolders.value = current
    }

    fun deleteVaultFolder(folderName: String) {
        if (folderName == "General") return // keep default
        val current = _vaultFolders.value.toMutableSet()
        current.remove(folderName)
        prefs.edit().putStringSet(KEY_VAULT_FOLDERS, current).apply()
        _vaultFolders.value = current
    }

    companion object {
        private const val PREFS_NAME = "am_player_secret_vault"
        private const val KEY_PIN = "vault_pin"
        private const val KEY_HIDDEN_IDS = "vault_hidden_ids"
        private const val KEY_VAULT_FOLDERS = "vault_folders"
    }
}
