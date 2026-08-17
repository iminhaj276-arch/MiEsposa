package com.miesposa.sadia.features.phone

import android.content.Context
import com.miesposa.sadia.services.SadiaAccessibilityService

/**
 * Multi-step WhatsApp automation. This is experimental and fragile by nature (UI layouts
 * change between WhatsApp versions), so every step fails safely rather than guessing.
 *
 * IMPORTANT: this class never sends a message on the first ask. CommandEngine is
 * responsible for holding the drafted message and only calling [confirmAndSend] after
 * the user has verbally confirmed — see SadiaAction.SendWhatsAppMessage /
 * ConfirmSendWhatsApp in CommandModels.kt.
 */
class WhatsAppController(private val context: Context) {

    fun isAccessibilityReady(): Boolean = SadiaAccessibilityService.instance != null

    /** Opens WhatsApp fresh (caller should wait ~1.5s before the next step for the UI to load). */
    fun openWhatsApp(): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp") ?: return false
        launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Searches for and opens a contact's chat by name. Call after [openWhatsApp] + a short delay. */
    fun openChatWithContact(contactName: String): Boolean {
        val service = SadiaAccessibilityService.instance ?: return false
        val searchOpened = service.tapButtonByLabel("search")
        if (!searchOpened) return false
        Thread.sleep(500)
        val typed = service.typeIntoSearchField(contactName)
        if (!typed) return false
        Thread.sleep(700)
        return service.tapListItemByLabel(contactName)
    }

    /** Types the draft message into the open chat's compose box. Does NOT send it. */
    fun draftMessage(text: String): Boolean {
        val service = SadiaAccessibilityService.instance ?: return false
        Thread.sleep(500)
        return service.typeIntoFocusedField(text)
    }

    /** Only call this after the user has explicitly confirmed verbally. */
    fun confirmAndSend(): Boolean {
        val service = SadiaAccessibilityService.instance ?: return false
        return service.tapButtonByLabel("send")
    }
}
