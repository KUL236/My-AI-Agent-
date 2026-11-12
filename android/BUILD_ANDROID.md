# Build SEWA AI Android app

## Requirements
- Android Studio (current stable)
- JDK 17 (Android Studio bundled JBR recommended)
- Android SDK Platform 36 and Build Tools installed via SDK Manager
- Internet access for first Gradle sync to fetch dependencies

## Gradle wrapper
`android/gradle/wrapper/gradle-wrapper.properties` pins Gradle 8.13 (required by AGP 8.13). Android Studio downloads it on first sync. If it asks for a Gradle version, choose 8.13.

## Build
1. Extract the ZIP.
2. Android Studio → Open → choose the `android` folder.
3. Set Gradle JDK to 17 and allow Sync to finish.
4. Build → Make Project.
5. Build → Build Bundle(s) / APK(s) → Build APK(s).
6. To enable the optional LLM, add `-PsewaLlmEndpoint=https://YOUR-SERVICE.onrender.com/v1/chat` to the Gradle command or configure it in Android Studio's Gradle arguments.
7. APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Local database
Room uses SQLite in app-private internal storage. The `chat_messages` table stores user and assistant messages with timestamps. No cloud account or internet permission is required by the app. The Android speech-recognition provider may independently use network access depending on device settings.

## Verification caveat
This package has not been compiled in this environment because Android SDK and Gradle are unavailable here. Perform Gradle Sync, APK build and physical-device QA in Android Studio before distribution.
