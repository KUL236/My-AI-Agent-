# SEWA AI phase status — Android app + optional LLM proxy v0.3.0

## Included in this ZIP
- Kotlin + Jetpack Compose Android application (edge-to-edge safe).
- Push-to-talk speech recognition intent and Android TTS.
- Local commands: open apps by name, YouTube, Chrome, Settings, Wi-Fi/Bluetooth settings, Camera, flashlight, volume, timer, web search, maps, dialer, battery, time, greeting.
- Opt-in AccessibilityService: Home, Back, Recents, Notifications, Quick Settings, Lock screen, Screenshot, Scroll, tap-on-text with explicit confirmation.
- Room SQLite database for local chat history, coroutine-based access, clear-history action.
- Optional Render/Groq LLM proxy; no chat history is stored by the proxy.

## Fixed in v0.3.0
- Gradle: removed `kotlinOptions` (error in Kotlin 2.2); now uses `compilerOptions`. Room bumped to 2.8.4 for Kotlin 2.2 compatibility.
- Manifest: added package-visibility `<queries>` for camera, TTS and launcher apps (camera command previously always failed on Android 11+).
- Command matching: "time" no longer fires inside other words; speech-unavailable is now reported instead of silently ignored.
- UI: safe-drawing insets for Android 15 edge-to-edge; MIC button text no longer clipped.
- Docs: removed stale Groq/backend checklist.
- Unknown prompts can use the Render proxy while all phone actions remain local.
- Groq credentials are read only from the Render environment.

## Not yet implemented
- Multi-step autonomous planning and tool orchestration.
- Owner speaker verification, liveness and replay/anti-spoofing.
- Wake-word / always-on listening.
- Guaranteed offline speech recognition across languages.
- Reminders, notifications engine, encrypted export.
- Compiled APK and physical-device QA (not possible in the authoring environment).

Do not represent pending items as implemented.
