package com.miesposa.sadia

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.miesposa.sadia.core.permissions.SadiaPermission
import com.miesposa.sadia.core.voice.WakeWordService
import com.miesposa.sadia.ui.HomeScreen
import com.miesposa.sadia.ui.ChatMessage
import com.miesposa.sadia.ui.PermissionCenterScreen
import com.miesposa.sadia.ui.PermissionRow
import com.miesposa.sadia.ui.theme.MiEsposaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val locator get() = (application as SadiaApplication).container

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListeningInternal() else pendingErrorMessage.value = "মাইক্রোফোন পারমিশন দেওয়া নেই। Permission দিলে voice command ব্যবহার করতে পারবে।"
    }

    private val messages = mutableStateListOf<ChatMessage>()
    private val isListening = mutableStateOf(false)
    private val pendingErrorMessage = mutableStateOf<String?>(null)
    private val isWakeWordOn = mutableStateOf(false)
    private val showPermissionCenter = mutableStateOf(false)
    private val permissionRefreshTick = mutableStateOf(0)

    private val runtimePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionRefreshTick.value++ }

    private val heardDebugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WakeWordService.ACTION_HEARD_DEBUG -> {
                    val heard = intent.getStringExtra(WakeWordService.EXTRA_HEARD_TEXT) ?: return
                    messages.add(ChatMessage(text = "🔍 শোনা গেছে: $heard", fromUser = false))
                }
                WakeWordService.ACTION_REPLY -> {
                    val reply = intent.getStringExtra(WakeWordService.EXTRA_REPLY_TEXT) ?: return
                    messages.add(ChatMessage(text = reply, fromUser = false))
                }
            }
        }
    }

    // Wake word needs BOTH mic and (on Android 13+) notification permission, since the
    // foreground service must show a persistent notification the whole time it runs.
    private val wakeWordPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micGranted = results[Manifest.permission.RECORD_AUDIO] == true
        val notifGranted = if (android.os.Build.VERSION.SDK_INT >= 33) {
            results[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true

        if (micGranted && notifGranted) {
            startWakeWord()
        } else {
            pendingErrorMessage.value = "Wake word চালু করতে মাইক্রোফোন ও নোটিফিকেশন — দুটো পারমিশনই লাগবে।"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messages.add(ChatMessage(text = getString(R.string.greeting), fromUser = false))

        LocalBroadcastManager.getInstance(this).registerReceiver(
            heardDebugReceiver, IntentFilter(WakeWordService.ACTION_HEARD_DEBUG).apply {
                addAction(WakeWordService.ACTION_REPLY)
            }
        )

        setContent {
            MiEsposaTheme {
                if (showPermissionCenter.value) {
                    // Reading permissionRefreshTick.value here (even unused directly)
                    // forces recomposition after a permission result comes back.
                    val _tick = permissionRefreshTick.value
                    val rows = SadiaPermission.values().map { permission ->
                        PermissionRow(
                            permission = permission,
                            isGranted = locator.permissionManager.isGranted(permission),
                            whyNeeded = permission.whyNeeded
                        )
                    }
                    PermissionCenterScreen(
                        rows = rows,
                        onAllowTapped = ::onPermissionAllowTapped,
                        onBack = { showPermissionCenter.value = false }
                    )
                } else {
                    HomeScreen(
                        messages = messages,
                        isListening = isListening.value,
                        onMicTapped = ::onMicTapped,
                        onSendText = ::handleUserInput,
                        onQuickAction = ::handleUserInput,
                        errorMessage = pendingErrorMessage.value,
                        onErrorShown = { pendingErrorMessage.value = null },
                        isWakeWordOn = isWakeWordOn.value,
                        onWakeWordToggle = ::onWakeWordToggle,
                        onOpenPermissionCenter = { showPermissionCenter.value = true }
                    )
                }
            }
        }
    }

    private fun onPermissionAllowTapped(permission: SadiaPermission) {
        val manifestPerm = permission.manifestPermission
        if (manifestPerm != null) {
            runtimePermissionLauncher.launch(manifestPerm)
        } else {
            // Accessibility / Notification Access are special-access screens, not
            // standard runtime permissions — route to the right system settings page.
            val action = when (permission) {
                SadiaPermission.ACCESSIBILITY -> android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
                SadiaPermission.NOTIFICATION_ACCESS -> "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                else -> android.provider.Settings.ACTION_SETTINGS
            }
            startActivity(Intent(action))
        }
    }

    private fun onMicTapped() {
        val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasMic) startListeningInternal()
        else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startListeningInternal() {
        isListening.value = true
        locator.voiceRecognizer.startListening(
            onPartialOrFinal = { text -> handleUserInput(text) },
            onError = { error -> pendingErrorMessage.value = error },
            onDone = { isListening.value = false }
        )
    }

    private fun handleUserInput(rawText: String) {
        if (rawText.isBlank()) return
        messages.add(ChatMessage(text = rawText, fromUser = true))

        lifecycleScope.launch {
            val action = locator.commandEngine.classify(rawText)
            val response = locator.commandEngine.execute(action)
            messages.add(ChatMessage(text = response.spokenText, fromUser = false))
            locator.ttsManager.speak(response.spokenText)

            if (!response.succeeded && response.spokenText.contains("Accessibility")) {
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
    }

    private fun onWakeWordToggle() {
        if (isWakeWordOn.value) {
            WakeWordService.stop(this)
            isWakeWordOn.value = false
            return
        }

        val neededPermissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val allGranted = neededPermissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) startWakeWord()
        else wakeWordPermissionsLauncher.launch(neededPermissions.toTypedArray())
    }

    private fun startWakeWord() {
        WakeWordService.start(this)
        isWakeWordOn.value = true
        messages.add(ChatMessage(text = "🎙️ Wake word চালু হয়েছে — এখন 'Sadia' বললেই আমি সাড়া দেব।", fromUser = false))
    }

    override fun onDestroy() {
        locator.voiceRecognizer.release()
        locator.ttsManager.shutdown()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(heardDebugReceiver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        // Catches permission changes made from system Settings (e.g. Accessibility,
        // Notification Access) since those don't go through the runtime launcher.
        permissionRefreshTick.value++
    }
}
