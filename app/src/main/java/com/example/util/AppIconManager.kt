package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object AppIconManager {
    private const val TAG = "AppIconManager"

    private val ALIAS_MAP = mapOf(
        "#E53935" to "com.example.MainActivityRed",
        "#9C27B0" to "com.example.MainActivityPurple",
        "#00E676" to "com.example.MainActivityGreen",
        "#FF9100" to "com.example.MainActivityAmber",
        "#00B0FF" to "com.example.MainActivityCyan",
        "#FF4081" to "com.example.MainActivityPink",
        "#FFD600" to "com.example.MainActivityYellow"
    )

    private const val DEFAULT_ALIAS = "com.example.MainActivityDefault"

    fun setAppIcon(context: Context, accentColorHex: String, dynamicIconEnabled: Boolean) {
        try {
            val pm = context.packageManager
            val targetAlias = if (dynamicIconEnabled) {
                ALIAS_MAP.entries.firstOrNull { it.key.equals(accentColorHex, ignoreCase = true) }?.value
                    ?: DEFAULT_ALIAS
            } else {
                DEFAULT_ALIAS
            }

            val allAliases = ALIAS_MAP.values + DEFAULT_ALIAS

            for (alias in allAliases) {
                val component = ComponentName(context.packageName, alias)
                val newState = if (alias == targetAlias) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                val currentState = pm.getComponentEnabledSetting(component)
                if (currentState != newState) {
                    pm.setComponentEnabledSetting(
                        component,
                        newState,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update app launcher icon", e)
        }
    }
}
