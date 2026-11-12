# SEWA AI local storage decision

Decision: local-first Android app with an optional stateless LLM proxy.

- Room (SQLite) is the source of truth for conversation history.
- All database reads/writes are performed off the main thread using coroutines.
- App data is kept in Android app-private internal storage.
- Local commands and Room history work without the network. Optional LLM chat uses the separate `server/` proxy; the Groq secret is never shipped in the APK.
- Android speech recognition is delegated to the OS/provider; offline capability varies by installed provider and language pack.

Future local tables can include reminders, tasks and audit events. Add Room migrations whenever the database schema version changes; do not use destructive migration for user data.
