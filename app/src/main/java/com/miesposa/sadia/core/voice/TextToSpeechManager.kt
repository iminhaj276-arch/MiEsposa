package com.miesposa.sadia.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import java.util.UUID

class TextToSpeechManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                configureBanglaFemaleVoice()
            }
        }
    }

    private fun configureBanglaFemaleVoice() {
        val engine = tts ?: return
        val banglaResult = engine.setLanguage(Locale("bn", "BD"))
        if (banglaResult == TextToSpeech.LANG_MISSING_DATA || banglaResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fall back to English rather than silently failing to speak at all.
            engine.language = Locale.US
        }

        // Prefer a female-sounding voice if the device's TTS engine exposes one.
        val femaleVoice: Voice? = engine.voices?.firstOrNull { voice ->
            voice.locale.language == "bn" && voice.name.contains("female", ignoreCase = true)
        }
        femaleVoice?.let { engine.voice = it }
    }

    fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sadia_utterance")
    }

    /**
     * Speaks and calls [onDone] once audio playback genuinely finishes (or immediately if
     * TTS isn't ready). Callers that also control a microphone (e.g. WakeWordService) MUST
     * wait for this before re-opening the mic — Android's audio focus system will otherwise
     * let a fresh recognition session cut off or fully block this speech, since starting
     * SpeechRecognizer requests focus exclusively over anything currently playing.
     */
    fun speakAndWait(text: String, onDone: () -> Unit) {
        val engine = tts
        if (!ready || engine == null) {
            onDone()
            return
        }
        val utteranceId = UUID.randomUUID().toString()
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onDone() }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { onDone() }
            override fun onError(utteranceId: String?, errorCode: Int) { onDone() }
        })
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
