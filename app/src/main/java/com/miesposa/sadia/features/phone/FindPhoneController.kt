package com.miesposa.sadia.features.phone

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri

class FindPhoneController(private val context: Context) {
    fun playFindSound(): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            val soundUri: Uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, soundUri)
            ringtone.play()
            true
        } catch (e: Exception) {
            false
        }
    }
}
