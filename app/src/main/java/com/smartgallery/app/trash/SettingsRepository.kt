package com.smartgallery.app.trash

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("smart_gallery_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_24H_VAULT = "use_24h_vault_enabled"
    }

    // Default setting: 24-Hour Vault Option True rahega
    fun is24HourVaultEnabled(): Boolean {
        return prefs.getBoolean(KEY_24H_VAULT, true)
    }

    fun set24HourVaultEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_24H_VAULT, enabled).apply()
    }
}
