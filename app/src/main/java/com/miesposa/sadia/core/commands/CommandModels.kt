package com.miesposa.sadia.core.commands

/**
 * Every user utterance is first classified into ONE of these actions before anything
 * executes. Free-form AI text is never executed directly against the phone — this is the
 * validation boundary described in the spec (section 6 / 21).
 */
sealed class SadiaAction {
    data class OpenApp(val appNameSpoken: String) : SadiaAction()
    object FlashlightOn : SadiaAction()
    object FlashlightOff : SadiaAction()
    object BatteryStatus : SadiaAction()
    data class VolumeSet(val percent: Int) : SadiaAction()
    object VolumeUp : SadiaAction()
    object VolumeDown : SadiaAction()
    object VolumeMute : SadiaAction()
    data class WebSearch(val query: String) : SadiaAction()
    data class PlaySong(val query: String) : SadiaAction()
    data class RememberFact(val key: String, val value: String) : SadiaAction()
    data class RecallFact(val key: String) : SadiaAction()
    object LockPhone : SadiaAction()
    object ShowRecentCalls : SadiaAction()
    object ShowStorageInfo : SadiaAction()
    object PlayFindPhoneSound : SadiaAction()
    data class CallContact(val contactName: String) : SadiaAction()
    object ReadClipboard : SadiaAction()
    data class DraftWhatsAppMessage(val contactName: String, val message: String) : SadiaAction()
    object ConfirmSendWhatsApp : SadiaAction()
    object CancelSendWhatsApp : SadiaAction()
    data class OpenSystemSettings(val settingsPane: SettingsPane) : SadiaAction()
    data class GeneralConversation(val userText: String) : SadiaAction()
    data class Unsupported(val reason: String) : SadiaAction()
}

enum class SettingsPane { WIFI, BLUETOOTH, DISPLAY, APPS }

/** Result Sadia speaks back. Always truthful — never claims success that didn't happen. */
data class SadiaResponse(
    val spokenText: String,
    val succeeded: Boolean
)

/** Known, installable-app name -> package mapping used for OPEN_APP. Extend as needed. */
object KnownApps {
    val spokenNameToPackage: Map<String, String> = mapOf(
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "camera" to "com.android.camera",
        "gallery" to "com.google.android.apps.photos",
        "settings" to "com.android.settings",
        "calculator" to "com.google.android.calculator",
        "maps" to "com.google.android.apps.maps",
        "phone" to "com.android.dialer",
        "messages" to "com.google.android.apps.messaging",
        "whatsapp" to "com.whatsapp",
        "হোয়াটসঅ্যাপ" to "com.whatsapp",
        "ফেসবুক" to "com.facebook.katana",
        "facebook" to "com.facebook.katana"
    )
}
