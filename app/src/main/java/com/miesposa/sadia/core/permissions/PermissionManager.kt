package com.miesposa.sadia.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class SadiaPermission(val manifestPermission: String?, val label: String, val whyNeeded: String) {
    MICROPHONE(Manifest.permission.RECORD_AUDIO, "মাইক্রোফোন", "ভয়েস কমান্ড শোনার জন্য"),
    CAMERA(Manifest.permission.CAMERA, "ক্যামেরা", "ছবি তোলা ও ভিশন ফিচারের জন্য"),
    NOTIFICATIONS(Manifest.permission.POST_NOTIFICATIONS, "নোটিফিকেশন", "Wake word চালু থাকার নোটিফিকেশন দেখানোর জন্য"),
    PHONE(Manifest.permission.CALL_PHONE, "ফোন কল", "ভয়েসে কল করার জন্য"),
    CONTACTS(Manifest.permission.READ_CONTACTS, "কন্ট্যাক্টস", "নাম বলে কল করার জন্য"),
    CALL_LOG(Manifest.permission.READ_CALL_LOG, "কল লিস্ট", "সাম্প্রতিক কল দেখার জন্য"),
    SMS(Manifest.permission.SEND_SMS, "এসএমএস", "SMS পাঠানোর জন্য"),
    ACCESSIBILITY(null, "অ্যাক্সেসিবিলিটি", "WhatsApp automation ও ফোন লক করার জন্য"),
    NOTIFICATION_ACCESS(null, "নোটিফিকেশন অ্যাক্সেস", "নোটিফিকেশন পড়ে শোনানোর জন্য")
}

/**
 * Per spec section 26/27: permissions are surfaced in a Permission Center and are only
 * ever REQUESTED (via the Activity) at the moment the matching feature is used — this
 * class only reports current status, it does not request anything on its own.
 */
class PermissionManager(private val context: Context) {

    fun isGranted(permission: SadiaPermission): Boolean {
        val manifestPerm = permission.manifestPermission
        return if (manifestPerm != null) {
            ContextCompat.checkSelfPermission(context, manifestPerm) == PackageManager.PERMISSION_GRANTED
        } else {
            isSpecialPermissionGranted(permission)
        }
    }

    private fun isSpecialPermissionGranted(permission: SadiaPermission): Boolean = when (permission) {
        SadiaPermission.NOTIFICATION_ACCESS -> {
            val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            enabledListeners?.contains(context.packageName) == true
        }
        SadiaPermission.ACCESSIBILITY -> {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
            enabledServices == "1" // Coarse check; refine per-service in Phase 2.
        }
        else -> false
    }
}
