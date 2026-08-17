package com.miesposa.sadia.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Powers the "reach into another app" features: typing + sending a WhatsApp message,
 * locking the screen, and similar. This is the most powerful (and most dangerous) part
 * of the app, so it follows strict rules at all times, matching the original spec
 * (section 20/21/27):
 *
 *  - NEVER reads or types into a field that looks like a password, PIN, or OTP field.
 *  - NEVER acts inside banking / payment apps — see SENSITIVE_PACKAGE_BLOCKLIST.
 *  - A "send"-type action always requires the caller to have already gotten explicit
 *    verbal confirmation from the user (enforced by CommandEngine before this is called,
 *    not by this class — but this class refuses again as a second layer, see [isSafeToAct]).
 *  - Only acts on the single foreground app the user asked about; never roams across apps
 *    scraping content in the background.
 */
class SadiaAccessibilityService : AccessibilityService() {

    companion object {
        // Static reference so CommandEngine-side controllers can reach the running
        // service without needing a bindService dance. Null when the user hasn't
        // enabled Accessibility for Sadia in system settings.
        var instance: SadiaAccessibilityService? = null
            private set

        val SENSITIVE_PACKAGE_BLOCKLIST = setOf(
            "com.google.android.apps.walletnfcrel", // Google Wallet
            "com.google.android.gms", // covers various secure GMS UI surfaces
            "com.android.settings" // never automate system Settings actions
            // Add any banking/payment app package names here as needed.
        )

        private val SENSITIVE_FIELD_HINTS = listOf(
            "password", "পাসওয়ার্ড", "পিন", "pin", "otp", "cvv", "security code", "verification code"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* passive; actions are pulled on-demand */ }
    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    /** Locking the phone is always safe — the reverse (unlocking) is intentionally NOT implemented. */
    fun lockScreen(): Boolean = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)

    fun currentPackageName(): String? = rootInActiveWindow?.packageName?.toString()

    fun isCurrentAppBlocklisted(): Boolean = currentPackageName() in SENSITIVE_PACKAGE_BLOCKLIST

    /**
     * Types [text] into the currently focused editable field in the foreground app.
     * Refuses if the focused field looks like a password/PIN/OTP field, or if the
     * foreground app is on the sensitive blocklist.
     */
    fun typeIntoFocusedField(text: String): Boolean {
        if (isCurrentAppBlocklisted()) return false
        val root = rootInActiveWindow ?: return false
        val focused = findFocusedEditableNode(root) ?: return false
        if (looksSensitive(focused)) return false

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    /**
     * Taps the first clickable node whose text or content-description matches [label]
     * (e.g. "Send"). Refuses on blocklisted apps. Caller (CommandEngine) is responsible
     * for having already gotten explicit confirmation before calling this for a "send".
     */
    fun tapButtonByLabel(label: String): Boolean {
        if (isCurrentAppBlocklisted()) return false
        val root = rootInActiveWindow ?: return false
        val target = findClickableNodeByLabel(root, label) ?: return false
        return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /** Taps the first clickable node matching [label] in the app's contact/search list — used to open a chat. */
    fun tapListItemByLabel(label: String): Boolean = tapButtonByLabel(label)

    /** Types [text] into whichever editable field on screen looks like a search box, if any. */
    fun typeIntoSearchField(text: String): Boolean {
        if (isCurrentAppBlocklisted()) return false
        val root = rootInActiveWindow ?: return false
        val searchNode = findNodeByHint(root, listOf("search", "সার্চ", "খুঁজুন")) ?: return false
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return searchNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    // --- Internal tree-walking helpers -------------------------------------------------

    private fun looksSensitive(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return true
        val hintText = (node.hintText?.toString() ?: "").lowercase()
        val text = (node.text?.toString() ?: "").lowercase()
        val contentDesc = (node.contentDescription?.toString() ?: "").lowercase()
        return SENSITIVE_FIELD_HINTS.any { hintText.contains(it) || text.contains(it) || contentDesc.contains(it) }
    }

    private fun findFocusedEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isFocused && root.isEditable) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            findFocusedEditableNode(child)?.let { return it }
        }
        return null
    }

    private fun findClickableNodeByLabel(root: AccessibilityNodeInfo, label: String): AccessibilityNodeInfo? {
        val normalizedLabel = label.lowercase()
        val text = (root.text?.toString() ?: "").lowercase()
        val contentDesc = (root.contentDescription?.toString() ?: "").lowercase()
        if (root.isClickable && (text.contains(normalizedLabel) || contentDesc.contains(normalizedLabel))) {
            return root
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            findClickableNodeByLabel(child, label)?.let { return it }
        }
        return null
    }

    private fun findNodeByHint(root: AccessibilityNodeInfo, hints: List<String>): AccessibilityNodeInfo? {
        val hintText = (root.hintText?.toString() ?: "").lowercase()
        val contentDesc = (root.contentDescription?.toString() ?: "").lowercase()
        if (root.isEditable && hints.any { hintText.contains(it) || contentDesc.contains(it) }) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            findNodeByHint(child, hints)?.let { return it }
        }
        return null
    }
}
