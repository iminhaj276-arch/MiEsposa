package com.miesposa.sadia.features.phone

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * "এই গানটা চালাও" — searches for the song by name and plays it via whichever music
 * app is installed (YouTube Music preferred, then YouTube, then browser fallback).
 * Never fakes playback — if nothing can open it, this returns false and CommandEngine
 * reports that honestly.
 */
class MusicPlayController(private val context: Context) {

    private val musicPackagesInPriorityOrder = listOf(
        "com.google.android.apps.youtube.music",
        "com.google.android.youtube",
        "com.spotify.music"
    )

    fun play(query: String): Boolean {
        for (packageName in musicPackagesInPriorityOrder) {
            if (isInstalled(packageName) && tryPlayOn(packageName, query)) return true
        }
        // Fallback: open a YouTube search in the browser.
        return try {
            val encoded = Uri.encode(query)
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=$encoded")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: Exception) {
        false
    }

    private fun tryPlayOn(packageName: String, query: String): Boolean = try {
        val intent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage(packageName)
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}
