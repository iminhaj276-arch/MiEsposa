package com.miesposa.sadia.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Talks ONLY to your own backend (never to OpenAI/Gemini directly from the app) so no API
 * key ever ships inside the APK — see BACKEND_CONTRACT.md for the request/response schema
 * this expects your server to implement.
 *
 * Set BASE_URL to your deployed backend before building a release APK.
 */
class AiBackendClient(
    private val baseUrl: String = "https://sadia-backend.iminhaj276.workers.dev"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * @param userText the raw utterance
     * @param memoryContext small local-memory snapshot (already privacy-filtered by MemoryStore)
     */
    suspend fun chat(userText: String, memoryContext: String): String = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("message", userText)
                put("memory_context", memoryContext)
                put("assistant_name", "Sadia")
                put("user_name", "Kolija")
                put("language", "bn")
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/v1/sadia/chat")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext fallbackReply()
                val json = JSONObject(response.body?.string().orEmpty())
                json.optString("reply").ifBlank { fallbackReply() }
            }
        } catch (e: Exception) {
            fallbackReply()
        }
    }

    private fun fallbackReply(): String =
        "Kolija, এই মুহূর্তে AI সার্ভারের সাথে সংযোগ করা যাচ্ছে না। ইন্টারনেট চেক করে আবার চেষ্টা করো।"
}
