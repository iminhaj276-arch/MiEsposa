package com.miesposa.sadia

import android.content.Context
import com.miesposa.sadia.core.commands.CommandEngine
import com.miesposa.sadia.core.memory.MemoryStore
import com.miesposa.sadia.core.permissions.PermissionManager
import com.miesposa.sadia.core.voice.TextToSpeechManager
import com.miesposa.sadia.core.voice.VoiceRecognizer
import com.miesposa.sadia.features.phone.AppLauncher
import com.miesposa.sadia.features.phone.BatteryController
import com.miesposa.sadia.features.phone.CallLogController
import com.miesposa.sadia.features.phone.ClipboardController
import com.miesposa.sadia.features.phone.ContactController
import com.miesposa.sadia.features.phone.FindPhoneController
import com.miesposa.sadia.features.phone.FlashlightController
import com.miesposa.sadia.features.phone.MusicPlayController
import com.miesposa.sadia.features.phone.StorageAnalyzer
import com.miesposa.sadia.features.phone.VolumeController
import com.miesposa.sadia.features.phone.WebSearchController
import com.miesposa.sadia.features.phone.WhatsAppController
import com.miesposa.sadia.network.AiBackendClient

/** Wires every module together once, at app start. Nothing here talks to UI directly. */
class ServiceLocator(private val appContext: Context) {

    val permissionManager = PermissionManager(appContext)
    val memoryStore = MemoryStore(appContext)

    val flashlightController = FlashlightController(appContext)
    val batteryController = BatteryController(appContext)
    val appLauncher = AppLauncher(appContext)
    val webSearchController = WebSearchController(appContext)
    val volumeController = VolumeController(appContext)
    val musicPlayController = MusicPlayController(appContext)
    val callLogController = CallLogController(appContext)
    val whatsAppController = WhatsAppController(appContext)
    val storageAnalyzer = StorageAnalyzer(appContext)
    val findPhoneController = FindPhoneController(appContext)
    val contactController = ContactController(appContext)
    val clipboardController = ClipboardController(appContext)

    val ttsManager = TextToSpeechManager(appContext)
    val voiceRecognizer = VoiceRecognizer(appContext)

    val aiBackendClient = AiBackendClient()

    val commandEngine = CommandEngine(
        flashlightController = flashlightController,
        batteryController = batteryController,
        appLauncher = appLauncher,
        webSearchController = webSearchController,
        volumeController = volumeController,
        musicPlayController = musicPlayController,
        callLogController = callLogController,
        whatsAppController = whatsAppController,
        storageAnalyzer = storageAnalyzer,
        findPhoneController = findPhoneController,
        contactController = contactController,
        clipboardController = clipboardController,
        memoryStore = memoryStore,
        aiBackendClient = aiBackendClient
    )
}
