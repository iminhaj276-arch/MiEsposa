package com.miesposa.sadia.features.phone

import android.content.Context
import android.media.AudioManager

class VolumeController(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun setPercent(percent: Int): Boolean = try {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (max * percent / 100.0).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        true
    } catch (e: SecurityException) {
        // Android may block this if Do-Not-Disturb / notification policy access is required.
        false
    }

    fun step(up: Boolean): Boolean = try {
        val direction = if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        true
    } catch (e: SecurityException) {
        false
    }

    fun mute(): Boolean = try {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
        true
    } catch (e: SecurityException) {
        false
    }
}
