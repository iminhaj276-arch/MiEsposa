package com.miesposa.sadia.features.phone

import android.content.ClipboardManager
import android.content.Context

class ClipboardController(private val context: Context) {
    fun readClipboard(): String? {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = manager?.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).text?.toString()
    }
}
