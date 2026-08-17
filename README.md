# 🥰 Mi Esposa 🧕 — Sadia (MVP)

Bangla-first Android AI assistant. This is a **real, compiling project skeleton** —
not a mockup — covering the core loop end to end:

**Voice/text input → CommandEngine classification → validated Android action → truthful spoken reply**

## What's implemented in this MVP

| Feature | Status |
|---|---|
| Premium dark UI (black / deep-red / purple, Jetpack Compose) | ✅ |
| Push-to-talk Bangla voice input (`bn-BD`) | ✅ |
| Text-to-Speech (Bangla, female voice if device supports it) | ✅ |
| Command Engine (rule-based classify → structured action → execute) | ✅ |
| Flashlight ON/OFF | ✅ |
| Battery status | ✅ |
| Open app (YouTube, Chrome, Camera, Gallery, Settings, Calculator, Maps, Phone, Messages) | ✅ |
| Web search (opens browser with query) | ✅ |
| Volume up/down/mute/set % | ✅ |
| System settings shortcuts (Wi-Fi/Bluetooth/Display/Apps) | ✅ |
| Local memory (remember/recall facts, DataStore-backed, user-deletable) | ✅ |
| General AI conversation fallback (via your own backend — no key in app) | ✅ |
| Runtime permission requests only when a feature is first used | ✅ |
| Wake-word ("Sadia") continuous listening — toggle in top bar, persistent notification while active | ✅ |
| Real AI backend (Gemini via Cloudflare Worker) | ✅ |
| Play song by name (YouTube Music / YouTube / Spotify) | ✅ |
| Recent call log | ✅ |
| Lock phone (voice-triggered unlock intentionally NOT built — security risk) | ✅ |
| WhatsApp: open a contact's chat and draft a message, send only after verbal confirmation | ✅ (experimental — see caveats below) |

## Not yet implemented (honest roadmap — do not assume these work)

These need dedicated design/testing passes and are **intentionally excluded** from
this MVP rather than shipped as fake/TODO code:

- Battery-optimization exemption prompt for the wake-word service (Android may still
  kill it in the background on some OEMs, e.g. MIUI, unless the user manually
  whitelists the app in battery settings).
- NotificationListenerService (reading/summarizing notifications).
- AccessibilityService (screen reading, tap/type/scroll automation).
- Calling, SMS composing/sending, contacts search.
- Alarm/timer/reminder scheduling.
- Charging/app-event triggers and scheduled automation (WorkManager scaffolding
  is already a dependency, engine not yet wired).
- Full Permission Center screen (the manager class exists; the settings UI doesn't yet).

## Project structure

```
MiEsposa/
├── app/
│   └── src/main/java/com/miesposa/sadia/
│       ├── MainActivity.kt              # runtime permission requests, wiring
│       ├── ServiceLocator.kt            # manual DI
│       ├── core/
│       │   ├── commands/                # CommandEngine + action schema
│       │   ├── voice/                   # SpeechRecognizer + TTS wrappers
│       │   ├── memory/                  # local DataStore memory
│       │   └── permissions/             # PermissionManager
│       ├── features/phone/              # Flashlight, Battery, AppLauncher, Volume, WebSearch
│       ├── network/                     # AiBackendClient (no key in app)
│       └── ui/                          # Compose HomeScreen + theme
```

## Build instructions

1. Open the `MiEsposa/` folder in Android Studio (Koala+ recommended).
2. Let Gradle sync (uses AGP 8.5.2, Kotlin 1.9.24, Compose BOM 2024.06.00).
3. Set your real backend URL in `AiBackendClient.kt` (`baseUrl` constructor param) —
   see `BACKEND_CONTRACT.md` below for what your server must implement.
4. Build → Build Bundle(s)/APK(s) → Build APK(s), or `./gradlew assembleDebug`.
5. Install: `adb install app/build/outputs/apk/debug/app-debug.apk`.
6. Launch, tap the mic once — Android will ask for microphone permission at that
   moment (not at first launch).

Test order per the spec: **Redmi 9 (Android 12) first**, then Galaxy A57 5G (Android 16).

## Backend contract (`AiBackendClient`)

Your backend must expose:

```
POST /v1/sadia/chat
Content-Type: application/json

{
  "message": "user's raw text",
  "memory_context": "small local memory snapshot, plain text",
  "assistant_name": "Sadia",
  "user_name": "Kolija",
  "language": "bn"
}
```

Response:

```json
{ "reply": "Sadia-র উত্তর, বাংলায়" }
```

Your backend holds the actual OpenAI/Gemini API key — it is never embedded in the APK.

## Next steps (tell me which to build next)

1. Full Permission Center screen (visual, matches spec section 26).
2. Alarm/timer via `AlarmClock` intents + exact-alarm handling for Android 12–16.
3. NotificationListenerService with the ON/OFF privacy toggle (default OFF).
4. AccessibilityService foundation with sensitive-field (password/OTP) blocking.
5. Wake-word evaluation (on-device vs. cloud trade-offs, battery impact on Redmi 9).
