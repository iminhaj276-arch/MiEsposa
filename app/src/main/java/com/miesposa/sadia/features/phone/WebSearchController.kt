package com.miesposa.sadia.features.phone

import android.content.Context
import android.content.Intent
import java.net.URLEncoder

class WebSearchController(private val context: Context) {
    fun search(query: String): Boolean {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=$encoded"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
