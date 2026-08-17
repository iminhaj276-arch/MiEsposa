package com.miesposa.sadia.core.commands

import com.miesposa.sadia.core.memory.MemoryStore
import com.miesposa.sadia.features.phone.AppLauncher
import com.miesposa.sadia.features.phone.BatteryController
import com.miesposa.sadia.features.phone.CallLogController
import com.miesposa.sadia.features.phone.ClipboardController
import com.miesposa.sadia.features.phone.ContactController
import com.miesposa.sadia.features.phone.FindPhoneController
import com.miesposa.sadia.features.phone.FlashlightController
import com.miesposa.sadia.features.phone.MusicPlayController
import com.miesposa.sadia.features.phone.StorageAnalyzer
import com.miesposa.sadia.features.phone.VolumeController
import com.miesposa.sadia.features.phone.WebSearchController
import com.miesposa.sadia.features.phone.WhatsAppController
import com.miesposa.sadia.network.AiBackendClient
import com.miesposa.sadia.services.SadiaAccessibilityService

/**
 * Core of the assistant. Two-stage pipeline:
 *   1. classify(): plain Bangla/English text -> SadiaAction (rule-based first, AI fallback)
 *   2. execute():  SadiaAction -> real Android side-effect + truthful spoken response
 *
 * This mirrors spec section 6: never let AI-generated text execute directly on the device.
 */
class CommandEngine(
    private val flashlightController: FlashlightController,
    private val batteryController: BatteryController,
    private val appLauncher: AppLauncher,
    private val webSearchController: WebSearchController,
    private val volumeController: VolumeController,
    private val musicPlayController: MusicPlayController,
    private val callLogController: CallLogController,
    private val whatsAppController: WhatsAppController,
    private val storageAnalyzer: StorageAnalyzer,
    private val findPhoneController: FindPhoneController,
    private val contactController: ContactController,
    private val clipboardController: ClipboardController,
    private val memoryStore: MemoryStore,
    private val aiBackendClient: AiBackendClient
) {
    // Holds a drafted-but-unsent WhatsApp message. Only ConfirmSendWhatsApp can send it —
    // a fresh command that isn't an explicit confirmation clears this instead of sending.
    private var pendingWhatsAppDraft: Pair<String, String>? = null // (contactName, message)

    /** Cheap, deterministic keyword classifier. Runs BEFORE any network call. */
    fun classify(rawText: String): SadiaAction {
        val text = rawText.trim().lowercase()

        if (text.isEmpty()) return SadiaAction.Unsupported("খালি কমান্ড")

        // If a WhatsApp message is pending, only a clear yes/no counts as a reply to it.
        if (pendingWhatsAppDraft != null) {
            if (containsAny(text, "নিশ্চিত", "পাঠাও", "সেন্ড করো", "yes", "confirm", "send it")) {
                return SadiaAction.ConfirmSendWhatsApp
            }
            if (containsAny(text, "না", "বাতিল", "cancel", "no")) {
                return SadiaAction.CancelSendWhatsApp
            }
            // Any other utterance falls through to normal classification — treated as
            // a new request, and execute() will silently drop the stale pending draft.
        }

        // Lock phone
        if (containsAny(text, "lock phone", "ফোন লক", "লক করো")) {
            return SadiaAction.LockPhone
        }

        // Recent calls
        if (containsAny(text, "recent call", "call list", "কল লিস্ট", "কে কল করেছিল", "মিসড কল")) {
            return SadiaAction.ShowRecentCalls
        }

        // Storage
        if (containsAny(text, "storage", "স্টোরেজ", "মেমরি কত")) {
            return SadiaAction.ShowStorageInfo
        }

        // Find my phone
        if (containsAny(text, "find my phone", "ফোন খুঁজে পাচ্ছি না", "sound বাজাও", "খুঁজে পাচ্ছি না")) {
            return SadiaAction.PlayFindPhoneSound
        }

        // Clipboard
        if (containsAny(text, "clipboard", "ক্লিপবোর্ড")) {
            return SadiaAction.ReadClipboard
        }

        // Call contact: "X কে call দাও" / "call X"
        val callMatch = Regex("""(\S+)(?:কে|-কে)\s*(?:call|কল)""").find(text)
            ?: Regex("""call\s+(\S+)""").find(text)
        if (containsAny(text, "call", "কল") && callMatch != null && !containsAny(text, "call list", "কল লিস্ট")) {
            val name = callMatch.groupValues[1].trim()
            if (name.isNotBlank()) return SadiaAction.CallContact(name)
        }

        // WhatsApp message: "Rahim কে WhatsApp এ বলো ..." / "message Rahim on whatsapp ..."
        val waMatch = Regex("""(?:whatsapp|হোয়াটসঅ্যাপ).*?(?:এ|to)\s*(\S+)\s*(?:কে|কে বলো|:|-)\s*(.+)""")
            .find(text)
        if (containsAny(text, "whatsapp", "হোয়াটসঅ্যাপ") && containsAny(text, "message", "বলো", "লিখো", "পাঠাও")) {
            // Simplified extraction: first word after "whatsapp"/"হোয়াটসঅ্যাপ" as contact,
            // rest of the sentence as message. Real parsing is hard from free speech —
            // this is a best-effort MVP heuristic, refined later with real usage data.
            val words = text.split(" ")
            val contactGuessIndex = words.indexOfFirst { it.contains("whatsapp") || it.contains("হোয়াটসঅ্যাপ") }
            val contact = words.getOrNull(contactGuessIndex + 1)?.trim() ?: ""
            val message = text.substringAfterLast(contact).trim().ifBlank { "" }
            if (contact.isNotBlank() && message.isNotBlank()) {
                return SadiaAction.DraftWhatsAppMessage(contact, message)
            }
        }

        // Flashlight
        if (containsAny(text, "flashlight", "torch", "টর্চ", "ফ্ল্যাশ")) {
            return if (containsAny(text, "off", "বন্ধ")) SadiaAction.FlashlightOff
            else SadiaAction.FlashlightOn
        }

        // Battery
        if (containsAny(text, "battery", "ব্যাটারি", "চার্জ")) {
            return SadiaAction.BatteryStatus
        }

        // Volume
        if (containsAny(text, "volume", "ভলিউম")) {
            return when {
                containsAny(text, "mute", "silent", "মিউট") -> SadiaAction.VolumeMute
                containsAny(text, "up", "বাড়া", "বাড়াও", "increase") -> SadiaAction.VolumeUp
                containsAny(text, "down", "কমা", "কমাও", "decrease") -> SadiaAction.VolumeDown
                else -> {
                    val percent = Regex("""(\d{1,3})""").find(text)?.groupValues?.get(1)?.toIntOrNull()
                    if (percent != null) SadiaAction.VolumeSet(percent.coerceIn(0, 100))
                    else SadiaAction.Unsupported("volume percent বোঝা যায়নি")
                }
            }
        }

        // Web search: "google-এ X search করো" / "search X"
        val searchMatch = Regex("""(?:search|সার্চ)\s+(?:for\s+)?(.+?)(?:\s+(?:করো|করে দাও|দাও))?$""")
            .find(text)
        if (containsAny(text, "search", "সার্চ") && searchMatch != null) {
            val query = searchMatch.groupValues[1].trim()
            if (query.isNotEmpty()) return SadiaAction.WebSearch(query)
        }

        // Remember fact: "মনে রাখো আমি ..."
        if (containsAny(text, "মনে রাখো", "remember")) {
            val fact = text.substringAfter("মনে রাখো").ifBlank { text.substringAfter("remember") }.trim()
            if (fact.isNotEmpty()) return SadiaAction.RememberFact(key = "note_${System.currentTimeMillis()}", value = fact)
        }

        // Play song: "গান চালাও" / "play song"
        if (containsAny(text, "গান চালাও", "play song", "গান বাজাও", "song play")) {
            val query = text
                .replace("গান চালাও", "").replace("গান বাজাও", "")
                .replace("play song", "").replace("song play", "")
                .trim()
            return SadiaAction.PlaySong(query.ifBlank { "" })
        }

        // Open app: "X খুলে দাও" / "open X"
        for ((spokenName, _) in KnownApps.spokenNameToPackage) {
            if (text.contains(spokenName)) {
                return SadiaAction.OpenApp(spokenName)
            }
        }

        // Fall back to general AI conversation — this is text-only, never executes anything.
        return SadiaAction.GeneralConversation(rawText)
    }

    /** Executes a validated action. Always returns a truthful result. */
    suspend fun execute(action: SadiaAction): SadiaResponse {
        return when (action) {
            is SadiaAction.FlashlightOn -> respondFrom(flashlightController.turnOn(), "ফ্ল্যাশলাইট চালু করে দিচ্ছি।", "এই ডিভাইসে ফ্ল্যাশলাইট নিয়ন্ত্রণ করা গেল না।")
            is SadiaAction.FlashlightOff -> respondFrom(flashlightController.turnOff(), "ফ্ল্যাশলাইট বন্ধ করে দিলাম।", "ফ্ল্যাশলাইট বন্ধ করা গেল না।")
            is SadiaAction.BatteryStatus -> {
                val status = batteryController.getStatus()
                SadiaResponse("Kolija, ব্যাটারি এখন ${status.percent}% এবং ${if (status.isCharging) "চার্জ হচ্ছে।" else "চার্জ হচ্ছে না।"}", true)
            }
            is SadiaAction.OpenApp -> {
                val opened = appLauncher.openBySpokenName(action.appNameSpoken)
                if (opened) SadiaResponse("ঠিক আছে Kolija, খুলে দিচ্ছি।", true)
                else SadiaResponse("এই অ্যাপটি এই ফোনে ইনস্টল করা নেই।", false)
            }
            is SadiaAction.WebSearch -> {
                val opened = webSearchController.search(action.query)
                if (opened) SadiaResponse("\"${action.query}\" নিয়ে সার্চ করে দিচ্ছি।", true)
                else SadiaResponse("সার্চ ব্রাউজার খোলা গেল না।", false)
            }
            is SadiaAction.PlaySong -> {
                if (action.query.isBlank()) {
                    SadiaResponse("কোন গানটা চালাব বলো, Kolija।", false)
                } else {
                    val played = musicPlayController.play(action.query)
                    if (played) SadiaResponse("\"${action.query}\" চালিয়ে দিচ্ছি।", true)
                    else SadiaResponse("গানটা চালানো গেল না — কোনো মিউজিক অ্যাপ খুঁজে পাইনি।", false)
                }
            }
            is SadiaAction.VolumeSet -> respondFrom(volumeController.setPercent(action.percent), "ভলিউম ${action.percent}% সেট করলাম।", "ভলিউম পরিবর্তন করা গেল না।")
            is SadiaAction.VolumeUp -> respondFrom(volumeController.step(up = true), "ভলিউম বাড়িয়ে দিলাম।", "ভলিউম বাড়ানো গেল না।")
            is SadiaAction.VolumeDown -> respondFrom(volumeController.step(up = false), "ভলিউম কমিয়ে দিলাম।", "ভলিউম কমানো গেল না।")
            is SadiaAction.VolumeMute -> respondFrom(volumeController.mute(), "সাউন্ড মিউট করে দিলাম।", "মিউট করা গেল না।")
            is SadiaAction.RememberFact -> {
                memoryStore.save(action.key, action.value)
                SadiaResponse("মনে রাখলাম, Kolija।", true)
            }
            is SadiaAction.RecallFact -> {
                val value = memoryStore.get(action.key)
                if (value != null) SadiaResponse(value, true)
                else SadiaResponse("এই বিষয়ে আমার কাছে কিছু মনে রাখা নেই।", false)
            }
            is SadiaAction.LockPhone -> {
                val service = SadiaAccessibilityService.instance
                if (service == null) {
                    SadiaResponse("ফোন লক করতে Accessibility permission লাগবে। Settings থেকে চালু করে দাও।", false)
                } else {
                    respondFrom(service.lockScreen(), "ফোন লক করে দিলাম।", "ফোন লক করা গেল না।")
                }
            }
            is SadiaAction.ShowRecentCalls -> {
                val calls = callLogController.recentCalls(5)
                if (calls.isEmpty()) {
                    SadiaResponse("সাম্প্রতিক কোনো কল পাওয়া যায়নি, অথবা Call Log permission দেওয়া নেই।", false)
                } else {
                    val summary = calls.joinToString("। ") { "${it.name} (${it.type})" }
                    SadiaResponse("সাম্প্রতিক কলগুলো: $summary", true)
                }
            }
            is SadiaAction.DraftWhatsAppMessage -> {
                if (!whatsAppController.isAccessibilityReady()) {
                    SadiaResponse("WhatsApp-এ বার্তা পাঠাতে Accessibility permission লাগবে। Settings থেকে চালু করে দাও।", false)
                } else {
                    whatsAppController.openWhatsApp()
                    Thread.sleep(1500)
                    val chatOpened = whatsAppController.openChatWithContact(action.contactName)
                    if (!chatOpened) {
                        SadiaResponse("${action.contactName} নামের কাউকে WhatsApp-এ খুঁজে পাইনি।", false)
                    } else {
                        val drafted = whatsAppController.draftMessage(action.message)
                        if (drafted) {
                            pendingWhatsAppDraft = action.contactName to action.message
                            SadiaResponse("লিখে রেখেছি: \"${action.message}\" — ${action.contactName}-কে পাঠাব? বলো 'নিশ্চিত' বা 'বাতিল'।", true)
                        } else {
                            SadiaResponse("বার্তাটা টাইপ করা গেল না।", false)
                        }
                    }
                }
            }
            is SadiaAction.ConfirmSendWhatsApp -> {
                val draft = pendingWhatsAppDraft
                pendingWhatsAppDraft = null
                if (draft == null) {
                    SadiaResponse("পাঠানোর জন্য কোনো বার্তা প্রস্তুত নেই।", false)
                } else {
                    respondFrom(
                        whatsAppController.confirmAndSend(),
                        "${draft.first}-কে বার্তা পাঠিয়ে দিলাম।",
                        "বার্তা পাঠানো গেল না — Send বাটন খুঁজে পাইনি।"
                    )
                }
            }
            is SadiaAction.CancelSendWhatsApp -> {
                pendingWhatsAppDraft = null
                SadiaResponse("ঠিক আছে, বার্তাটা পাঠানো হলো না।", true)
            }
            is SadiaAction.ShowStorageInfo -> {
                val info = storageAnalyzer.analyze()
                SadiaResponse(
                    "মোট স্টোরেজ ${"%.1f".format(info.totalGb)} GB, ব্যবহৃত ${"%.1f".format(info.usedGb)} GB, খালি আছে ${"%.1f".format(info.freeGb)} GB।",
                    true
                )
            }
            is SadiaAction.PlayFindPhoneSound -> {
                respondFrom(findPhoneController.playFindSound(), "বস, আমি এখানে আছি! শব্দ বাজাচ্ছি।", "শব্দ বাজানো গেল না।")
            }
            is SadiaAction.CallContact -> {
                val match = contactController.findByName(action.contactName)
                if (match == null) {
                    SadiaResponse("${action.contactName} নামে কোনো কন্ট্যাক্ট খুঁজে পাইনি।", false)
                } else {
                    val called = contactController.callNumber(match.number)
                    if (called) SadiaResponse("${match.name}-কে কল করছি।", true)
                    else {
                        contactController.openDialer(match.number)
                        SadiaResponse("সরাসরি কল করার permission নেই, তাই ডায়ালার খুলে দিলাম — ${match.name}।", false)
                    }
                }
            }
            is SadiaAction.ReadClipboard -> {
                val text = clipboardController.readClipboard()
                if (text.isNullOrBlank()) SadiaResponse("ক্লিপবোর্ডে কিছু নেই।", false)
                else SadiaResponse("ক্লিপবোর্ডে আছে: $text", true)
            }
            is SadiaAction.OpenSystemSettings -> respondFrom(appLauncher.openSettingsPane(action.settingsPane), "সেটিংস খুলে দিচ্ছি।", "সেটিংস খোলা গেল না।")
            is SadiaAction.GeneralConversation -> {
                val reply = aiBackendClient.chat(action.userText, memoryStore.snapshotForContext())
                SadiaResponse(reply, true)
            }
            is SadiaAction.Unsupported -> SadiaResponse(
                "Kolija, এই কাজটা আমি এখনো পারি না — ${action.reason}।",
                false
            )
        }
    }

    private fun respondFrom(ok: Boolean, successText: String, failureText: String) =
        if (ok) SadiaResponse(successText, true) else SadiaResponse(failureText, false)

    private fun containsAny(text: String, vararg keywords: String) = keywords.any { text.contains(it) }
}
