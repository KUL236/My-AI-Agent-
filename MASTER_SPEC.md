# SEWA AI — Master specification (Android app with optional LLM proxy)

Tagline: Understand Every Language. Execute Only Your Authorized Commands.

Platform: Android, Kotlin, Jetpack Compose.
Storage: Room/SQLite on device only. Optional LLM requests go through a separate Render API proxy; no cloud chat history is stored by the app.
Speech: Android speech-recognition provider and Android TextToSpeech; actual language/offline support depends on device provider and installed language packs.

Security principles: do not claim that speech transcription authenticates the speaker; sensitive actions require explicit confirmation and later owner verification with liveness and fallback device authentication. Do not embed secrets in the APK.

Current milestone (v0.3.0): local command execution, opt-in Accessibility phone control with confirmation for taps, persistent local chat history, and optional Groq chat through a secret-safe proxy. See docs/PHASE_STATUS.md for implemented and pending features.
