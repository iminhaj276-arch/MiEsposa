package com.miesposa.sadia.core.memory

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.memoryDataStore by preferencesDataStore(name = "sadia_memory")

/**
 * Local-first memory. Nothing here is uploaded to any server automatically — see
 * snapshotForContext(), which returns only a small, user-visible subset for AI context,
 * matching spec section 15/27 (no secret uploading of personal memories).
 */
class MemoryStore(private val context: Context) {

    fun save(key: String, value: String) = runBlocking {
        context.memoryDataStore.edit { prefs -> prefs[stringPreferencesKey(key)] = value }
    }

    fun get(key: String): String? = runBlocking {
        context.memoryDataStore.data.first()[stringPreferencesKey(key)]
    }

    fun delete(key: String) = runBlocking {
        context.memoryDataStore.edit { prefs -> prefs.remove(stringPreferencesKey(key)) }
    }

    suspend fun getAll(): Map<String, String> {
        val prefs = context.memoryDataStore.data.first()
        return prefs.asMap().entries.associate { it.key.name to it.value.toString() }
    }

    /** Small, bounded context sent to the AI backend — user can disable this entirely
     *  from Privacy Settings (cloudAiEnabled flag, wired in PermissionManager). */
    fun snapshotForContext(maxEntries: Int = 10): String = runBlocking {
        getAll().entries.take(maxEntries).joinToString("\n") { "${it.key}: ${it.value}" }
    }
}
