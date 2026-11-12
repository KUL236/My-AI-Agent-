# SEWA AI — Local-only architecture

```text
User voice / text
       |
       v
Jetpack Compose UI (MainActivity)
       |
       v
LocalCommandEngine ---> Android intents / system APIs (apps, settings, timer, torch, volume)
       |        \
       |         `--> SewaAccessibilityService (Home/Back/Recents/Notifications/Scroll/Tap)
       |                  ^ user must enable manually; tap needs spoken/typed confirmation
       v
Room DAO (coroutines) -> SQLite in app-private storage

Speech input -> Android speech-recognition service (provider-dependent)
Assistant output -> Android TextToSpeech
```

The optional `server/` service proxies LLM requests to Groq. It receives chat context from the app but does not persist it. The Groq key is an environment secret on Render, never an Android resource or source constant.
