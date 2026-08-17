package com.miesposa.sadia.core.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Push-to-talk voice input. Continuous "Hey Sadia" wake-word listening is NOT implemented
 * in this MVP (see spec section 25) because reliable always-on mic access requires a
 * foreground service + battery-optimization exemption that must be explained to and
 * approved by the user first. Push-to-talk is the honest, working fallback for now.
 */
class VoiceRecognizer(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(
        onPartialOrFinal: (String) -> Unit,
        onError: (String) -> Unit,
        onDone: () -> Unit
    ) {
        if (!isAvailable()) {
            onError("এই ডিভাইসে ভয়েস রিকগনিশন পাওয়া যাচ্ছে না।")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: android.os.Bundle) {
                    val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (text != null) onPartialOrFinal(text)
                    onDone()
                }
                override fun onError(error: Int) {
                    onError("ভয়েস শোনা যায়নি, আবার চেষ্টা করো।")
                    onDone()
                }
                override fun onPartialResults(partialResults: android.os.Bundle) {}
                override fun onReadyForSpeech(params: android.os.Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // Bangla (Bangladesh) primary locale for recognition
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            startListening(intent)
        }
    }

    fun stop() {
        recognizer?.stopListening()
    }

    fun release() {
        recognizer?.destroy()
        recognizer = null
    }
}
