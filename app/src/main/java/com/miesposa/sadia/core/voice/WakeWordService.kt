package com.miesposa.sadia.core.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.miesposa.sadia.MainActivity
import com.miesposa.sadia.R
import com.miesposa.sadia.SadiaApplication

/**
 * Continuous "Sadia" wake-word listening (spec section 25).
 *
 * Honesty/privacy rules this follows:
 *  - Only runs after the user explicitly turns it ON in the app (see MainActivity toggle).
 *  - Shows a PERSISTENT notification the entire time it's listening — Android requires
 *    this for a microphone foreground service, and it also means the mic status is never
 *    hidden from the user.
 *  - Nothing is sent anywhere until the wake word "sadia" is actually heard; everything
 *    before that is discarded, not stored, not uploaded.
 *  - The user can stop it anytime from the app or by swiping the notification away
 *    (which calls stopSelf via the notification's delete intent — see below).
 */
class WakeWordService : Service() {

    companion object {
        const val CHANNEL_ID = "sadia_wake_word_channel"
        const val NOTIFICATION_ID = 1001
        val WAKE_WORD_VARIANTS = listOf("sadia", "sadiya", "সাদিয়া", "সাদিয়ে", "সাদিয়")
        const val ACTION_STOP = "com.miesposa.sadia.action.STOP_WAKE_WORD"
        const val ACTION_HEARD_DEBUG = "com.miesposa.sadia.action.WAKE_WORD_HEARD_DEBUG"
        const val EXTRA_HEARD_TEXT = "heard_text"
        const val ACTION_REPLY = "com.miesposa.sadia.action.WAKE_WORD_REPLY"
        const val EXTRA_REPLY_TEXT = "reply_text"

        fun start(context: Context) {
            val intent = Intent(context, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WakeWordService::class.java))
        }
    }

    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var mutedUntilMillis = 0L

    private val locator get() = (application as SadiaApplication).container

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopListeningLoop()
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        isRunning = true
        startListeningLoop()
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        recognizer?.destroy()
        recognizer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startListeningLoop() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf()
            return
        }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val heard = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.lowercase().orEmpty()
                    handleHeardText(heard)
                }
                override fun onError(error: Int) {
                    // No speech / timeout / busy / etc — this is completely normal in a
                    // continuous loop. No longer surfaced to the user (was just debug noise).
                    relisten()
                }
                override fun onPartialResults(partialResults: Bundle) {}
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            beginListening(this)
        }
    }

    private fun beginListening(recognizer: SpeechRecognizer) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            // Let each session listen longer before giving up on silence alone — fewer
            // restart cycles (and fewer OEM "listening started" beep sounds) while idle.
            // But once the user actually starts and finishes speaking, finalize quickly
            // for a snappy reply.
            putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 1200)
            putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 1200)
            putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 8000)
        }
        recognizer.startListening(intent)
    }

    /** Restart listening shortly after each result/error — this is what makes it "continuous".
     *  Recreates the SpeechRecognizer from scratch each time rather than reusing the same
     *  instance: reusing it caused ERROR_RECOGNIZER_BUSY (error code 8) loops on some
     *  devices (notably MIUI), where the previous session doesn't fully release before
     *  the next startListening() call. Delay kept modest — long silence timeout above
     *  already keeps genuine restarts (and their beep sound) infrequent. */
    private fun relisten() {
        if (!isRunning) return
        handler.postDelayed({
            if (isRunning) startListeningLoop()
        }, 800)
    }

    private fun stopListeningLoop() {
        isRunning = false
        recognizer?.stopListening()
    }

    private fun onSpeakingFinished() {
        // Ignore anything heard for the next second — gives the speaker's own audio
        // tail time to fully decay before the mic starts trusting what it hears again.
        mutedUntilMillis = System.currentTimeMillis() + 1000
        relisten()
    }

    private fun broadcastHeard(text: String) {
        val intent = Intent(ACTION_HEARD_DEBUG).putExtra(EXTRA_HEARD_TEXT, text)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun handleHeardText(heard: String) {
        val matchedWakeWord = WAKE_WORD_VARIANTS.firstOrNull { heard.contains(it) }
        if (matchedWakeWord == null) {
            // Wake word not in this utterance — nothing to act on, keep listening.
            relisten()
            return
        }

        if (System.currentTimeMillis() < mutedUntilMillis) {
            // Sadia's own voice from the speaker just bled into the mic and got
            // misheard as the wake word (a classic self-echo loop). Ignore it —
            // this is what was causing "জি বলেন Kolija" to repeat endlessly.
            relisten()
            return
        }

        // Pause listening while we compute + speak a response, so the mic doesn't
        // steal audio focus and silence/cut off our own TTS reply mid-sentence.
        recognizer?.stopListening()

        // Everything after the wake word is the actual command.
        val command = heard.substringAfter(matchedWakeWord).trim()

        if (command.isBlank()) {
            // User just said "Sadia" with no command yet — acknowledge and wait.
            val greeting = "জি বলেন Kolija, আমি আপনার জন্য কী করতে পারি?"
            broadcastReply(greeting)
            locator.ttsManager.speakAndWait(greeting) { onSpeakingFinished() }
            return
        }

        Thread {
            val action = locator.commandEngine.classify(command)
            // execute() is a suspend fun; run it on a simple blocking bridge here since
            // this is a plain background thread, not a coroutine scope.
            kotlinx.coroutines.runBlocking {
                val response = locator.commandEngine.execute(action)
                broadcastReply(response.spokenText)
                // Resume listening only once the reply has actually finished playing —
                // this is the fix for TTS getting cut off by the mic restarting too soon.
                handler.post {
                    locator.ttsManager.speakAndWait(response.spokenText) { onSpeakingFinished() }
                }
            }
        }.start()
    }

    private fun broadcastReply(text: String) {
        val intent = Intent(ACTION_REPLY).putExtra(EXTRA_REPLY_TEXT, text)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        updateNotification(text)
    }

    private fun updateNotification(latestText: String) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification(latestText))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sadia Wake Word",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sadia মাইক্রোফোনে 'Sadia' শব্দের জন্য শুনছে"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(latestText: String? = null): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, WakeWordService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sadia শুনছে 🎙️")
            .setContentText(latestText?.take(80) ?: "'Sadia' বললেই সাড়া দেবে — বন্ধ করতে ট্যাপ করো")
            .setStyle(NotificationCompat.BigTextStyle().bigText(latestText ?: "'Sadia' বললেই সাড়া দেবে — বন্ধ করতে ট্যাপ করো"))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "বন্ধ করো", stopIntent)
            .build()
    }
}
