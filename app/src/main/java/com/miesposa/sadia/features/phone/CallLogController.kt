package com.miesposa.sadia.features.phone

import android.content.Context
import android.provider.CallLog

data class CallLogEntry(val name: String, val number: String, val type: String)

class CallLogController(private val context: Context) {

    /** Reads the most recent [limit] calls. Returns empty list if permission isn't granted. */
    fun recentCalls(limit: Int = 5): List<CallLogEntry> {
        val entries = mutableListOf<CallLogEntry>()
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE),
                null, null,
                "${CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                var count = 0
                while (it.moveToNext() && count < limit) {
                    val name = it.getString(0) ?: "অজানা নম্বর"
                    val number = it.getString(1) ?: ""
                    val typeCode = it.getInt(2)
                    val type = when (typeCode) {
                        CallLog.Calls.INCOMING_TYPE -> "ইনকামিং"
                        CallLog.Calls.OUTGOING_TYPE -> "আউটগোয়িং"
                        CallLog.Calls.MISSED_TYPE -> "মিসড"
                        else -> "অন্যান্য"
                    }
                    entries.add(CallLogEntry(name, number, type))
                    count++
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted — return empty, CommandEngine reports this honestly.
        }
        return entries
    }
}
