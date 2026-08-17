package com.miesposa.sadia.features.phone

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract

data class ContactMatch(val name: String, val number: String)

class ContactController(private val context: Context) {

    fun findByName(name: String): ContactMatch? {
        val query = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$name%")

        return try {
            context.contentResolver.query(query, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayName = cursor.getString(0) ?: name
                    val number = cursor.getString(1) ?: return null
                    ContactMatch(displayName, number)
                } else null
            }
        } catch (e: SecurityException) {
            null
        }
    }

    /** Places a call directly (requires CALL_PHONE granted). Returns false if it can't. */
    fun callNumber(number: String): Boolean = try {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }

    /** Fallback: opens the dialer pre-filled, doesn't call automatically. */
    fun openDialer(number: String): Boolean = try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}
