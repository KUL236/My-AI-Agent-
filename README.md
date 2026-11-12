# SEWA AI — Local-only Android assistant

SEWA AI is a native Kotlin + Jetpack Compose Android assistant. This edition stores chat history on the device using Room (SQLite). It does not use Supabase, a cloud database, Groq, or an app-owned internet connection.

## Included

- Native Android app using Kotlin and Jetpack Compose.
- Push-to-talk speech input using the Android speech-recognition service.
- Android Text-to-Speech responses.
- Room database (`sewa_local.db`) for persistent chat history.
- Clear-history action that deletes local chat rows.
- Local command engine: open any installed app by name, Settings / Wi-Fi / Bluetooth settings, Camera, flashlight, volume, timer, web search, maps, dialer, battery, time, greetings.
- Phone control through an Android AccessibilityService (opt-in): Home, Back, Recents, Notifications, Quick Settings, Lock screen, Screenshot, Scroll, and tap-on-text (tap always asks for confirmation).
- Optional LLM chat through a Render-hosted API proxy. The Groq key stays on Render and is never embedded in the APK.

## Phone control (Accessibility)

1. Say or type `enable phone control` -> Accessibility settings open -> turn on **SEWA AI Phone Control**.
2. On Android 13+ for a sideloaded APK: Settings -> Apps -> SEWA AI -> ⋮ -> **Allow restricted settings**, then enable the service.
3. Example commands: `go home`, `go back`, `open notifications`, `lock screen`, `screenshot`, `scroll down`, `tap Subscribe` (SEWA asks "haan/yes" first).

`scroll` and `tap` act on the app in front of SEWA, so SEWA moves itself to the background first, then acts. Open the target app first, then give the command.

## Local data design

- Room / SQLite: chat messages (`chat_messages`) with role, content and timestamp.
- Database location: app-private internal storage; other apps cannot normally read it.
- Uninstalling the app normally removes its app-private database. Back up/export is not included in this build.
- Do not store passwords, API keys, or raw voice biometric templates in chat history.

## Requirements

- Android Studio (current stable)
- JDK 17
- Android SDK Platform 36 (install via SDK Manager if required)
- Android phone API 26+ or emulator
- Internet access on the development computer for first Gradle sync/dependency download

## Open and build

1. Extract the ZIP.
2. Android Studio → Open → select the `android` directory (contains `settings.gradle.kts`).
3. Set Gradle JDK to 17.
4. Allow Gradle Sync to download Android Gradle Plugin, Kotlin, Compose and Room dependencies.
5. Select Build → Make Project.
6. Select Build → Build Bundle(s) / APK(s) → Build APK(s).
7. Debug APK output: `android/app/build/outputs/apk/debug/app-debug.apk`.

This handoff contains source code, not a precompiled APK. The current execution environment does not have Android SDK/Gradle installed, so a successful APK build and physical-device QA cannot be claimed.

## Speech recognition note

The app uses Android's installed speech-recognition service. Language coverage and offline recognition depend on the device/provider and downloaded language packs. The app itself does not send requests to a SEWA cloud backend, but the speech-recognition provider may process audio according to its own settings and privacy policy.

## Optional LLM setup

The Android app remains the only user-facing product. Unknown prompts can use the small Render API service in `server/`; local commands continue to work without it. Deploy the `server/` directory using `render.yaml`, set `GROQ_API_KEY` in Render, then build with `-PsewaLlmEndpoint=https://YOUR-SERVICE.onrender.com/v1/chat`. Never put a Groq key in Kotlin, Git, or an APK. Because the key was previously shared in chat, revoke it and create a replacement before deployment.

## Current limitations

- LLM chat requires the optional Render proxy; local commands work without network access.
- No owner voice verification, anti-spoofing, liveness detection or wake word yet. Speech-to-text is not speaker authentication.
- Not a fully autonomous agent: phone commands are intentionally single-step and keyword based. The optional LLM answers general questions but cannot claim to have performed phone actions.
- Wi-Fi / Bluetooth cannot be toggled by apps on modern Android; SEWA opens the settings page. Calls open the dialer (you press Call). Apps with secure screens (banking, password fields) may block accessibility control.
- No cloud sync, Supabase, account login or automatic backup.
- No reminders/notifications engine or multi-step autonomous actions yet.

## Project structure

```text
sewa-ai/
├── android/
│   ├── app/src/main/java/com/sewa/ai/MainActivity.kt
│   ├── app/src/main/java/com/sewa/ai/LocalCommandEngine.kt
│   ├── app/src/main/java/com/sewa/ai/SewaAccessibilityService.kt
│   ├── app/src/main/java/com/sewa/ai/SewaDatabase.kt
│   └── app/src/main/AndroidManifest.xml
├── docs/
└── MASTER_SPEC.md
```
