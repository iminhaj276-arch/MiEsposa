package com.miesposa.sadia.features.phone

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.miesposa.sadia.core.commands.KnownApps
import com.miesposa.sadia.core.commands.SettingsPane

class AppLauncher(private val context: Context) {

    /** Returns false (never crashes, never lies) if the app isn't installed. */
    fun openBySpokenName(spokenName: String): Boolean {
        val packageName = KnownApps.spokenNameToPackage[spokenName.lowercase()] ?: return false
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openSettingsPane(pane: SettingsPane): Boolean {
        val action = when (pane) {
            SettingsPane.WIFI -> Settings.ACTION_WIFI_SETTINGS
            SettingsPane.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
            SettingsPane.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
            SettingsPane.APPS -> Settings.ACTION_APPLICATION_SETTINGS
        }
        return try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            false
        }
    }
}
